package com.faster.festival.core.sos

import android.content.Context
import com.faster.festival.AppConfig
import com.faster.festival.core.crypto.NonceGenerator
import com.faster.festival.data.remote.SosPhoneLocation
import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.data.sos.SosLocationProvider
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.WristbandInfo
import com.faster.festival.domain.sos.CancelPinchSOSUseCase
import com.faster.festival.domain.sos.PinchAlertDetail
import com.faster.festival.domain.sos.PinchUiStatus
import com.faster.festival.domain.sos.PollSOSStatusUseCase
import com.faster.festival.domain.sos.SendLocationUpdateUseCase
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.domain.sos.SubmitPinchDetailsUseCase
import com.faster.festival.domain.sos.TriggerSOSUseCase
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Single source of truth for an active SOS — whether it was triggered by a
 * wristband BLE Mesh `0x11` packet or by the in-app "Get Medical Help" button.
 *
 * Responsibilities:
 *  • One active session at a time. Both inputs reuse the same session if
 *    something is already in flight. Wristband retries (1s/2s/4s/8s) are
 *    deduped via [SOSDeduplicator] — they never produce a second backend alert.
 *  • Sends BLE Mesh `0x20` ACK back to the wristband as soon as a `0x11`
 *    arrives — independent of UI lifecycle.
 *  • Drives backend `pinch-ingest` via the existing [TriggerSOSUseCase] (which
 *    handles the Ed25519 sign + retry on transport / 5xx).
 *  • Owns the polling job — only one. Cancelling the manager cancels polling.
 *  • Persists the active session so process death + relaunch can resume.
 *
 * The two existing view models ([com.faster.festival.wristband.ui.sos.SosAlertViewModel]
 * and [com.faster.festival.presentation.sos.SOSViewModel]) become observers
 * over [state] — they no longer call backend / BLE themselves.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EmergencySOSManager(
    private val appContext: Context,
    private val triggerSos: TriggerSOSUseCase,
    private val pollStatus: PollSOSStatusUseCase,
    private val submitDetailsUseCase: SubmitPinchDetailsUseCase,
    private val cancelPinch: CancelPinchSOSUseCase,
    private val sendLocation: SendLocationUpdateUseCase,
    private val locationProvider: SosLocationProvider,
    private val pairedWristbandRepo: WristbandRepository,
    private val wristbandAck: WristbandAckManager,
    private val sessionStore: ActiveSessionStore,
    private val deduplicator: SOSDeduplicator = SOSDeduplicator(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val _state = MutableStateFlow<EmergencySOSState>(EmergencySOSState.Idle)
    val state: StateFlow<EmergencySOSState> = _state.asStateFlow()

    /**
     * Fires exactly once when [resumeIfPersisted] successfully restores an
     * in-flight session after process death. UI observes this to auto-
     * navigate the user back to the SOS overlay / pinch-help screen so they
     * don't lose visibility on an active emergency.
     */
    private val _recoveredOnLaunch = MutableSharedFlow<ActiveSOSSession>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val recoveredOnLaunch: SharedFlow<ActiveSOSSession> = _recoveredOnLaunch.asSharedFlow()

    private val mutex = Mutex()
    private var pollingJob: Job? = null

    /**
     * Restore any in-flight session from disk. Call once on app start AFTER
     * the SOS module is initialized but before any UI binds.
     */
    fun resumeIfPersisted() {
        scope.launch {
            val saved = runCatching { sessionStore.load() }.getOrNull() ?: return@launch
            mutex.withLock {
                if (_state.value !is EmergencySOSState.Idle) return@withLock
                if (saved.alertId.isNullOrBlank()) {
                    // Process died between persist and the 2xx response — the
                    // backend may or may not have the alert. Show "Sending"
                    // and let the worker re-dispatch (it reuses
                    // client_trigger_id, so the server idempotently dedups).
                    Timber.tag(TAG).w(
                        "Resuming persisted SOS with no alert_id — re-dispatching | trigger=%s",
                        saved.clientTriggerId.takeLast(8)
                    )
                    _state.value = EmergencySOSState.Sending(saved)
                    SosDispatchRetryWorker.enqueue(appContext)
                } else {
                    Timber.tag(TAG).i(
                        "Resuming persisted SOS — trigger=%s alert=%s source=%s",
                        saved.clientTriggerId.takeLast(8),
                        saved.alertId.takeLast(8),
                        saved.sourceTag
                    )
                    _state.value = EmergencySOSState.Active(
                        session = saved,
                        userStatus = SosUserStatus.SosReceived,
                        responderName = null,
                        etaMinutes = null,
                        uiStatus = PinchUiStatus.fromRaw(saved.lastUiStatus)
                            .takeIf { it != PinchUiStatus.Unknown }
                            ?: PinchUiStatus.AlertReceived
                    )
                    startPolling(saved)
                }
                // Fire AFTER state is set so the UI observer can navigate
                // straight into the live overlay.
                _recoveredOnLaunch.tryEmit(saved)
            }
        }
    }

    // ─── Public entry points ───────────────────────────────────────────────

    /**
     * In-app "Get Medical Help" tap. If a wristband-source session is already
     * active, this is a no-op — both flows feed into the same emergency.
     */
    suspend fun startManualSOS(festivalId: String = AppConfig.DEFAULT_FESTIVAL_SLUG) {
        mutex.withLock {
            if (currentSession() != null) {
                Timber.tag(TAG).i("Manual SOS skipped — active session already in flight")
                return
            }
            val session = ActiveSOSSession(
                clientTriggerId = NonceGenerator.newClientTriggerId(),
                source = SosSource.Manual,
                festivalId = festivalId,
                startedAtEpochMs = System.currentTimeMillis()
            )
            beginSession(session)
        }
    }

    /**
     * Wristband BLE Mesh `0x11` arrived. Called from the long-lived
     * [WristbandSosWatcher] coroutine — never from a screen.
     */
    suspend fun handleWristbandSOS(
        eventId: Long,
        retryCount: Int,
        batteryPct: Int,
        deviceUptimeMs: Long,
        festivalId: String = AppConfig.DEFAULT_FESTIVAL_SLUG
    ) {
        // Always ACK back to the wristband — regardless of whether we've seen
        // this event before. The ACK manager dedups internally.
        wristbandAck.ackOnce(eventId, helpDispatched = true)

        // Drop firmware retries.
        if (!deduplicator.shouldHandle(eventId)) {
            Timber.tag(TAG).d("Wristband SOS retry — drop dup event_id=%d retry=%d",
                eventId, retryCount)
            return
        }

        mutex.withLock {
            // If a manual SOS just started, *adopt* the wristband event into the
            // existing session rather than start a second one.
            currentSession()?.let { existing ->
                if (existing.wristbandEvent == null) {
                    val merged = existing.copy(
                        wristbandEvent = WristbandEventInfo(
                            eventId, retryCount, batteryPct, deviceUptimeMs
                        )
                    )
                    persist(merged)
                    Timber.tag(TAG).i("Adopting wristband event %d into manual session %s",
                        eventId, existing.clientTriggerId.takeLast(8))
                }
                return
            }

            val session = ActiveSOSSession(
                clientTriggerId = NonceGenerator.newClientTriggerId(),
                source = SosSource.Wristband,
                festivalId = festivalId,
                startedAtEpochMs = System.currentTimeMillis(),
                wristbandEvent = WristbandEventInfo(
                    eventId, retryCount, batteryPct, deviceUptimeMs
                )
            )
            beginSession(session)
        }
    }

    /** Wristband `0x12` cancel arrived. */
    suspend fun handleWristbandCancel(eventId: Long) {
        mutex.withLock {
            val s = currentSession() ?: return
            if (s.wristbandEvent?.eventId != eventId) return
            Timber.tag(TAG).i("Wristband cancel — closing session %s",
                s.clientTriggerId.takeLast(8))
            endSession(EmergencySOSState.Cancelled(s))
        }
        deduplicator.forget(eventId)
    }

    /** UI-driven cancel (user dismisses overlay etc.) — immediate local close. */
    suspend fun cancelByUser() {
        mutex.withLock {
            val s = currentSession() ?: return
            Timber.tag(TAG).i("User cancel — closing session %s",
                s.clientTriggerId.takeLast(8))
            endSession(EmergencySOSState.Cancelled(s))
        }
    }

    /**
     * New Pinch-flow cancel: POST `pinch-cancel` then KEEP polling. The
     * authoritative terminal (`ui_status=CANCELLED`) arrives via the poll loop —
     * we never assume the cancel succeeded. Optimistically shows CANCEL_REQUESTED;
     * reverts on failure and returns the [Result] so the UI can offer a retry.
     * The `client_request_id` is generated once and reused on retry for dedup.
     */
    suspend fun requestUserCancel(reason: String = "User cancelled from mobile"): Result<Unit> {
        val active = _state.value as? EmergencySOSState.Active
            ?: return Result.failure(IllegalStateException("No active SOS to cancel"))
        val alertId = active.session.alertId
            ?: return Result.failure(IllegalStateException("Alert id not assigned yet — try again"))
        val cancelId = active.session.cancelRequestId ?: NonceGenerator.newCancelRequestId()
        val updatedSession = active.session.copy(cancelRequestId = cancelId)
        persist(updatedSession)
        // Optimistic — the poll loop confirms with the real ui_status.
        _state.value = active.copy(
            session = updatedSession,
            uiStatus = PinchUiStatus.CancelRequested
        )
        Timber.tag(TAG).i("pinch-cancel request — alert=%s", alertId.takeLast(8))
        return cancelPinch(alertId, cancelId, reason).map { ui ->
            (_state.value as? EmergencySOSState.Active)?.let { cur ->
                _state.value = cur.copy(
                    uiStatus = ui.takeIf { it != PinchUiStatus.Unknown }
                        ?: PinchUiStatus.CancelRequested
                )
            }
            Unit
        }.onFailure { e ->
            Timber.tag(TAG).w(e, "pinch-cancel failed — reverting optimistic CANCEL_REQUESTED")
            (_state.value as? EmergencySOSState.Active)?.let { cur ->
                _state.value = cur.copy(uiStatus = active.uiStatus)
            }
        }
    }

    /**
     * Submit one signed partial-details update (`pinch-alert-details`) for the
     * active alert. [clientUpdateId] is generated once per user action by the
     * caller and reused on retry for dedup. Returns the [Result] so the UI can
     * show per-sheet success / failure.
     */
    suspend fun submitDetails(
        detail: PinchAlertDetail,
        clientUpdateId: String
    ): Result<Unit> {
        val alertId = (_state.value as? EmergencySOSState.Active)?.session?.alertId
            ?: return Result.failure(IllegalStateException("No active alert for details submit"))
        return submitDetailsUseCase(alertId, detail, clientUpdateId)
    }

    /**
     * On-demand signed location push for the "My current location" action.
     * Requires a `tracking_session_id` (assigned at ingest) and a real GPS fix.
     */
    suspend fun pushCurrentLocationNow(): Result<Unit> {
        val active = _state.value as? EmergencySOSState.Active
            ?: return Result.failure(IllegalStateException("No active SOS"))
        val trackingId = active.session.trackingSessionId
            ?: return Result.failure(IllegalStateException("Tracking session not assigned yet"))
        val fix = locationProvider.currentFix()
            ?: return Result.failure(IllegalStateException("No GPS fix available"))
        return sendLocation(
            clientTriggerId = active.session.clientTriggerId,
            trackingSessionId = trackingId,
            location = SosLocation(fix.latitude, fix.longitude, fix.accuracy.toInt())
        )
    }

    /**
     * UI-driven "Dispatch Responder" — sends BLE `0x21` to the wristband.
     * Backend PATCH for the same transition lands in Phase B.
     */
    suspend fun markResponderDispatched(etaMinutes: Int) {
        val current = state.value as? EmergencySOSState.Active ?: return
        val wb = current.session.wristbandEvent
        if (wb != null) wristbandAck.responderDispatchedOnce(wb.eventId, etaMinutes)
        _state.value = current.copy(
            userStatus = SosUserStatus.ResponderEnRoute,
            etaMinutes = etaMinutes,
            uiStatus = PinchUiStatus.HelpOnTheWay
        )
    }

    /** UI-driven "Mark Resolved" — sends BLE `0x22` and ends the session. */
    suspend fun markResolved(falseAlarm: Boolean = false) {
        mutex.withLock {
            val s = currentSession() ?: return
            s.wristbandEvent?.let { wristbandAck.resolvedOnce(it.eventId, falseAlarm) }
            endSession(EmergencySOSState.Resolved(s, SosUserStatus.Resolved))
        }
    }

    /** Retry after a Failed state — same client_trigger_id, fresh sign. */
    suspend fun retry() {
        val failed = state.value as? EmergencySOSState.Failed ?: return
        val session = failed.session ?: return
        mutex.withLock {
            _state.value = EmergencySOSState.Sending(session)
        }
        sendBackend(session)
    }

    // ─── Internals ─────────────────────────────────────────────────────────

    private suspend fun beginSession(session: ActiveSOSSession) {
        persist(session)
        _state.value = EmergencySOSState.Sending(session)
        sendBackend(session)
    }

    private suspend fun sendBackend(session: ActiveSOSSession) {
        val location = locationProvider.currentFix()?.let {
            SosLocation(it.latitude, it.longitude, it.accuracy.toInt())
        }
        val wristband = buildWristbandInfo(session)

        val result = triggerSos(
            clientTriggerId = session.clientTriggerId,
            festivalId = session.festivalId,
            location = location,
            wristband = wristband
        )
        result.fold(
            onSuccess = { handle ->
                // Persist the alert_id + tracking_session_id + ui_status before
                // transitioning so process death mid-handler doesn't undo the
                // dispatch acknowledgement, and the FG location emitter can fire
                // immediately (tracking id now arrives WITH the ingest response).
                val ackedSession = session.copy(
                    alertId = handle.alertId,
                    trackingSessionId = handle.trackingSessionId ?: session.trackingSessionId,
                    lastUiStatus = handle.initialUiStatus.raw
                )
                persist(ackedSession)
                _state.value = EmergencySOSState.Active(
                    session = ackedSession,
                    userStatus = handle.initialStatus
                        .takeIf { it != SosUserStatus.Unknown }
                        ?: SosUserStatus.SosReceived,
                    responderName = null,
                    etaMinutes = null,
                    uiStatus = handle.initialUiStatus
                )
                startPolling(ackedSession)
                // Fire-and-forget Project 1 audit POST — never blocks the SOS
                // critical path.
                fireSosHistoryAudit(ackedSession, location)
            },
            onFailure = { err ->
                Timber.tag(TAG).e(err, "pinch-ingest failed for trigger=%s",
                    session.clientTriggerId.takeLast(8))
                _state.value = EmergencySOSState.Failed(
                    session = session,
                    message = err.localizedMessage ?: "SOS could not be sent."
                )
                // Inline 3-attempt budget exhausted — hand off to the durable
                // retry queue (WorkManager). The session is already persisted
                // with alertId == null, so the worker has everything it needs.
                SosDispatchRetryWorker.enqueue(appContext)
            }
        )
    }

    /**
     * Called by [SosDispatchRetryWorker] after a successful re-dispatch.
     * Transitions Failed/Sending → Active and starts polling.
     */
    fun onDispatchAcknowledged(
        clientTriggerId: String,
        alertId: String?,
        initialStatus: SosUserStatus,
        trackingSessionId: String? = null,
        initialUiStatus: PinchUiStatus = PinchUiStatus.AlertReceived
    ) {
        scope.launch {
            mutex.withLock {
                val cur = currentOrFailedSession() ?: return@withLock
                if (cur.clientTriggerId != clientTriggerId) {
                    Timber.tag(TAG).w(
                        "Worker ack for stale trigger=%s (current=%s) — ignoring",
                        clientTriggerId.takeLast(8),
                        cur.clientTriggerId.takeLast(8)
                    )
                    return@withLock
                }
                val acked = cur.copy(
                    alertId = alertId,
                    trackingSessionId = trackingSessionId ?: cur.trackingSessionId,
                    lastUiStatus = initialUiStatus.raw
                )
                persist(acked)
                _state.value = EmergencySOSState.Active(
                    session = acked,
                    userStatus = initialStatus
                        .takeIf { it != SosUserStatus.Unknown }
                        ?: SosUserStatus.SosReceived,
                    responderName = null,
                    etaMinutes = null,
                    uiStatus = initialUiStatus
                )
                startPolling(acked)
                Timber.tag(TAG).i(
                    "Worker dispatch ack — state → Active | trigger=%s alert=%s",
                    clientTriggerId.takeLast(8), alertId?.takeLast(8) ?: "-"
                )
            }
        }
    }

    /**
     * Called by [SosDispatchRetryWorker] when the retry budget is exhausted
     * with no successful dispatch. Surface as a terminal Failed state so the
     * UI can offer a manual retry CTA.
     */
    fun onDispatchExhausted(clientTriggerId: String, message: String) {
        scope.launch {
            mutex.withLock {
                val cur = currentOrFailedSession() ?: return@withLock
                if (cur.clientTriggerId != clientTriggerId) return@withLock
                _state.value = EmergencySOSState.Failed(session = cur, message = message)
                Timber.tag(TAG).e(
                    "Worker retry budget exhausted — trigger=%s",
                    clientTriggerId.takeLast(8)
                )
            }
        }
    }

    /**
     * Fire-and-forget Project 1 audit record. Wristband-source sessions only —
     * a manual SOS has no `event_id` to record.
     */
    private fun fireSosHistoryAudit(session: ActiveSOSSession, location: SosLocation?) {
        val wbEvent = session.wristbandEvent ?: return
        val wristbandId = runCatching {
            // .getActiveOnce is suspend — bounce to a launch.
            null
        }
        scope.launch {
            val active = pairedWristbandRepo.getActiveOnce() ?: return@launch
            val phoneLoc = location?.let {
                SosPhoneLocation(
                    lat = it.latitude,
                    lng = it.longitude,
                    accuracyMeters = it.accuracyMeters,
                    capturedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                )
            }
            pairedWristbandRepo.recordSosEvent(
                wristbandId = active.wristbandId,
                eventId = wbEvent.eventId,
                state = "active",
                retryCount = wbEvent.retryCount,
                batteryPct = wbEvent.batteryPct,
                deviceUptimeMs = wbEvent.deviceUptimeMs,
                receivedAtIso = DateTimeFormatter.ISO_INSTANT.format(
                    Instant.ofEpochMilli(session.startedAtEpochMs)
                ),
                clientTriggerId = session.clientTriggerId,
                phoneLocation = phoneLoc
            ).onSuccess { resp ->
                Timber.tag(TAG).i(
                    "Project 1 SOS audit OK — sos_id=%s",
                    resp?.sosId?.takeLast(8) ?: "-"
                )
            }
        }
    }

    /**
     * Helper used by worker-callbacks. Returns whichever non-terminal session
     * is in scope — Sending, Active, or Failed.
     */
    private fun currentOrFailedSession(): ActiveSOSSession? = when (val s = _state.value) {
        is EmergencySOSState.Sending -> s.session
        is EmergencySOSState.Active -> s.session
        is EmergencySOSState.Failed -> s.session
        else -> null
    }

    private fun startPolling(session: ActiveSOSSession) {
        pollingJob?.cancel()
        var current = session
        pollingJob = scope.launch {
            // Poll by alert id when known (new contract keys status on the alert
            // id), falling back to client_trigger_id pre-ack.
            pollStatus(session.alertId, session.clientTriggerId).collectLatest { alert ->
                val status = SosUserStatus.fromRaw(alert.userStatus)
                val ui = PinchUiStatus.fromRaw(alert.uiStatus)

                // Late tracking_session_id (older views may still send it) —
                // the FG location emitter gates on this value.
                val newTrackingId = alert.trackingSessionId
                if (!newTrackingId.isNullOrBlank() &&
                    current.trackingSessionId != newTrackingId
                ) {
                    current = current.copy(trackingSessionId = newTrackingId)
                    persist(current)
                    Timber.tag(TAG).i("tracking_session_id received — %s", newTrackingId)
                }

                // Persist ui_status transitions so a relaunch restores the right
                // stage (skip disk churn when unchanged).
                if (ui != PinchUiStatus.Unknown && current.lastUiStatus != ui.raw) {
                    current = current.copy(lastUiStatus = ui.raw)
                    persist(current)
                }

                // ui_status is authoritative for terminal routing.
                val terminalUi = ui.isTerminal
                if (terminalUi || status.isTerminal) {
                    mutex.withLock {
                        endSession(
                            when {
                                ui == PinchUiStatus.Cancelled ||
                                    status == SosUserStatus.CancelledByUser ->
                                    EmergencySOSState.Cancelled(current)
                                ui == PinchUiStatus.Rejected ->
                                    EmergencySOSState.Resolved(current, SosUserStatus.Rejected)
                                else ->
                                    EmergencySOSState.Resolved(
                                        current,
                                        status.takeIf { it.isTerminal } ?: SosUserStatus.Resolved
                                    )
                            }
                        )
                    }
                } else {
                    _state.value = EmergencySOSState.Active(
                        session = current,
                        userStatus = status,
                        responderName = alert.responderName,
                        etaMinutes = alert.etaMinutes,
                        uiStatus = ui.takeIf { it != PinchUiStatus.Unknown }
                            ?: PinchUiStatus.AlertReceived,
                        etaLabel = alert.etaLabel,
                        responderMessage = alert.responderMessage
                    )
                }
            }
        }
    }

    private suspend fun endSession(terminalState: EmergencySOSState) {
        pollingJob?.cancel()
        pollingJob = null
        sessionStore.clear()
        deduplicator.reset()
        _recoveredOnLaunch.resetReplayCache()
        // A pending retry worker is now stale — the session it would resend
        // doesn't exist anymore. Cancel it so it doesn't fire later and
        // resurrect a closed emergency.
        SosDispatchRetryWorker.cancel(appContext)
        _state.value = terminalState
    }

    /**
     * Called by the UI observer after navigating in response to a
     * recovered-on-launch event. Clears the replay cache so a later observer
     * doesn't re-navigate.
     */
    fun consumeRecoveryEvent() {
        _recoveredOnLaunch.resetReplayCache()
    }

    private fun currentSession(): ActiveSOSSession? = when (val s = _state.value) {
        is EmergencySOSState.Sending -> s.session
        is EmergencySOSState.Active -> s.session
        else -> null
    }

    private suspend fun persist(session: ActiveSOSSession) {
        runCatching { sessionStore.save(session) }
            .onFailure { Timber.tag(TAG).w(it, "Could not persist active session") }
    }

    private suspend fun buildWristbandInfo(session: ActiveSOSSession): WristbandInfo {
        // Wristband-source sessions ALWAYS report connected with the firmware
        // event's battery info. Manual-source sessions ask the paired-repo
        // for the most recent telemetry-driven battery level.
        return when (session.source) {
            SosSource.Wristband -> {
                val wb = session.wristbandEvent
                WristbandInfo(
                    wristbandId = pairedWristbandRepo.activeWristband.first()?.wristbandId,
                    connectionState = "connected",
                    batteryPercent = wb?.batteryPct
                )
            }
            SosSource.Manual -> {
                val paired = pairedWristbandRepo.activeWristband.first()
                if (paired == null) {
                    WristbandInfo(
                        wristbandId = null,
                        connectionState = "mobile_only",
                        batteryPercent = null
                    )
                } else {
                    WristbandInfo(
                        wristbandId = paired.wristbandId,
                        connectionState = "connected",
                        batteryPercent = paired.batteryLevel
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "EmergencySOSManager"
    }
}
