package com.faster.festival.core.sos

import com.faster.festival.domain.sos.PinchUiStatus
import com.faster.festival.domain.sos.SosUserStatus

/** What started an active SOS — wristband button or in-app "Get Medical Help". */
sealed class SosSource {
    object Manual : SosSource()
    object Wristband : SosSource()
}

/**
 * Single in-flight emergency. Persisted to [ActiveSessionStore] so a process
 * death + relaunch can resume polling without losing the alert.
 *
 * Per spec: only ONE active session at a time. If wristband fires while a
 * manual SOS is in flight (or vice versa), the second trigger reuses the
 * existing [clientTriggerId] — we never create two backend alerts for the
 * same incident.
 */
data class ActiveSOSSession(
    val clientTriggerId: String,
    val source: SosSource,
    val festivalId: String,
    val startedAtEpochMs: Long,
    val wristbandEvent: WristbandEventInfo? = null,
    /**
     * Set once the server hands us a tracking session via
     * `pinch-alert-status.tracking_session_id`. Required as the payload key
     * for `pinch-update-location` — the foreground service won't fire
     * periodic GPS pushes until this is non-null.
     */
    val trackingSessionId: String? = null,
    /**
     * Set once `pinch-ingest` returns 2xx. The signal that distinguishes
     *   • a session whose backend alert exists (safe to poll) from
     *   • a session whose backend alert is still pending (resume must
     *     re-dispatch via [SosDispatchRetryWorker]).
     *
     * Persisted so process-death-during-dispatch can be reconciled on launch.
     */
    val alertId: String? = null,
    /**
     * Last backend `ui_status` (raw). Persisted so a relaunch restores the live
     * screen at the right stage before the first poll lands.
     */
    val lastUiStatus: String? = null,
    /**
     * Idempotency key for `pinch-cancel`. Generated once when the user first
     * taps Cancel and reused on retry so the backend dedups duplicate cancels.
     */
    val cancelRequestId: String? = null
) {
    val sourceTag: String
        get() = when (source) {
            SosSource.Manual -> "manual"
            SosSource.Wristband -> "wristband"
        }
}

/**
 * Snapshot of the firmware-side SOS at the moment we created the session.
 * Subsequent retries from the same `event_id` reuse the existing session
 * (see [SOSDeduplicator]) — only the first event populates this field.
 */
data class WristbandEventInfo(
    val eventId: Long,
    val retryCount: Int,
    val batteryPct: Int,
    val deviceUptimeMs: Long
)

/**
 * Unified state machine driving both the SOS overlay (wristband-triggered)
 * and the manual-button screen. Both view models observe the same flow off
 * [EmergencySOSManager.state] so they can never disagree.
 */
sealed class EmergencySOSState {

    object Idle : EmergencySOSState()

    /** Bootstrap (key gen / register / attest) is running silently. */
    object Preparing : EmergencySOSState()

    /** `pinch-ingest` round-trip in flight. UI shows a spinner / red banner. */
    data class Sending(val session: ActiveSOSSession) : EmergencySOSState()

    /** Backend accepted the SOS; polling status. */
    data class Active(
        val session: ActiveSOSSession,
        val userStatus: SosUserStatus,
        val responderName: String?,
        val etaMinutes: Int?,
        /** Backend-driven UI state — what the live Pinch screen renders from. */
        val uiStatus: PinchUiStatus = PinchUiStatus.AlertReceived,
        /** Human ETA banner from `eta.label` (e.g. "Arrives between 12:31-12:36"). */
        val etaLabel: String? = null,
        /** Responder status line from `responder.message`. */
        val responderMessage: String? = null
    ) : EmergencySOSState()

    /** Server-side terminal state (RESOLVED / CLOSED / REJECTED). */
    data class Resolved(
        val session: ActiveSOSSession,
        val terminalStatus: SosUserStatus
    ) : EmergencySOSState()

    /** Cancelled — by user (UI), or by wristband 0x12 cancel packet. */
    data class Cancelled(val session: ActiveSOSSession) : EmergencySOSState()

    /** `pinch-ingest` failed even after retry. UI offers a retry CTA. */
    data class Failed(val session: ActiveSOSSession?, val message: String) : EmergencySOSState()
}
