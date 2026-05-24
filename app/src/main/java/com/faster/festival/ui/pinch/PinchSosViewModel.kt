package com.faster.festival.ui.pinch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.AppConfig
import com.faster.festival.core.crypto.NonceGenerator
import com.faster.festival.core.sos.EmergencySOSManager
import com.faster.festival.core.sos.EmergencySOSState
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.repository.local.SosHistoryRepository
import com.faster.festival.domain.sos.PinchAlertDetail
import com.faster.festival.domain.sos.PinchUiStatus
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.ui.pinch.map.getCurrentLocation
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** Top-level screen the new Pinch SOS flow shows. */
enum class PinchScreen { Landing, Swipe, Live }

/** Which bottom sheet / modal is open over the live screen. */
enum class PinchSheet { None, AddMoreInfo, Phone, Medical, WhatHappened, Location }

/**
 * UI state for the new Pinch SOS flow. Everything the live screen renders is
 * derived from the backend `ui_status` ([uiStatus]) — never from a raw
 * `user_status` enum.
 */
data class PinchSosUiState(
    val screen: PinchScreen = PinchScreen.Landing,
    val uiStatus: PinchUiStatus = PinchUiStatus.AlertReceived,
    /** `pinch-ingest` in flight, alert id not yet assigned. */
    val isSending: Boolean = false,
    /** Dispatch round-trip failed even after retry; show a retry banner. */
    val isFailed: Boolean = false,
    val failureMessage: String? = null,
    /** Terminal reached — show the resolved / cancelled / rejected card. */
    val isTerminal: Boolean = false,
    val terminalStatus: PinchUiStatus? = null,
    val etaLabel: String? = null,
    val responderMessage: String? = null,
    val alertId: String? = null,
    val trackingSessionId: String? = null,
    val userLatLng: LatLng? = null,
    val mapLocationLoaded: Boolean = false,
    val activeSheet: PinchSheet = PinchSheet.None,
    /** A details submit / location push is in flight. */
    val sheetSubmitting: Boolean = false,
    /** One-shot snackbar text (e.g. "Sent to staff"). */
    val transientMessage: String? = null,
    val isOffline: Boolean = false
) {
    val isCancelPending: Boolean get() = uiStatus == PinchUiStatus.CancelRequested
    /** Cancel is only offered while genuinely in flight (not terminal / pending). */
    val canCancel: Boolean
        get() = !isTerminal && !isCancelPending && alertId != null &&
            uiStatus != PinchUiStatus.CancelDenied
}

/**
 * Drives the new Pinch SOS UI off the single-source-of-truth
 * [EmergencySOSManager]. This view model owns NO networking, polling, signing,
 * persistence or location pipeline — it only:
 *   • maps [EmergencySOSManager.state] → [PinchSosUiState] (`ui_status`-driven),
 *   • forwards user intents (swipe / cancel / details / location) to the manager,
 *   • holds the on-screen GPS marker + transient UI (sheets, banners).
 */
class PinchSosViewModel(
    private val manager: EmergencySOSManager,
    private val networkMonitor: NetworkMonitor,
    private val sosHistoryRepository: SosHistoryRepository? = null,
    private val festivalId: String = AppConfig.DEFAULT_FESTIVAL_ID
) : ViewModel() {

    private val _ui = MutableStateFlow(PinchSosUiState())
    val uiState: StateFlow<PinchSosUiState> = _ui.asStateFlow()

    /** True once this VM has shown the live screen for an in-flight session. */
    private var hasEnteredLive = false
    private var recordedHistory = false
    private var locationStarted = false

    init {
        viewModelScope.launch { manager.state.collectLatest { reduce(it) } }
        viewModelScope.launch {
            networkMonitor.state.collectLatest { cs ->
                _ui.update { it.copy(isOffline = cs.isOffline) }
            }
        }
    }

    // ─── manager.state → ui ─────────────────────────────────────────────────

    private fun reduce(es: EmergencySOSState) {
        when (es) {
            is EmergencySOSState.Sending -> {
                hasEnteredLive = true
                _ui.update {
                    it.copy(
                        screen = PinchScreen.Live,
                        isSending = true,
                        isFailed = false,
                        isTerminal = false,
                        terminalStatus = null,
                        uiStatus = PinchUiStatus.AlertReceived,
                        alertId = es.session.alertId,
                        trackingSessionId = es.session.trackingSessionId
                    )
                }
            }
            is EmergencySOSState.Active -> {
                hasEnteredLive = true
                maybeRecordHistory(es.session.alertId)
                _ui.update {
                    it.copy(
                        screen = PinchScreen.Live,
                        isSending = false,
                        isFailed = false,
                        isTerminal = false,
                        terminalStatus = null,
                        uiStatus = es.uiStatus,
                        etaLabel = es.etaLabel,
                        responderMessage = es.responderMessage,
                        alertId = es.session.alertId,
                        trackingSessionId = es.session.trackingSessionId
                    )
                }
            }
            is EmergencySOSState.Resolved -> if (hasEnteredLive) {
                val terminal = if (es.terminalStatus == SosUserStatus.Rejected)
                    PinchUiStatus.Rejected else PinchUiStatus.Completed
                _ui.update {
                    it.copy(
                        screen = PinchScreen.Live, isSending = false, isFailed = false,
                        isTerminal = true, terminalStatus = terminal, uiStatus = terminal
                    )
                }
            }
            is EmergencySOSState.Cancelled -> if (hasEnteredLive) {
                _ui.update {
                    it.copy(
                        screen = PinchScreen.Live, isSending = false, isFailed = false,
                        isTerminal = true, terminalStatus = PinchUiStatus.Cancelled,
                        uiStatus = PinchUiStatus.Cancelled
                    )
                }
            }
            is EmergencySOSState.Failed -> {
                _ui.update {
                    it.copy(
                        screen = if (hasEnteredLive) PinchScreen.Live else it.screen,
                        isSending = false,
                        isFailed = true,
                        failureMessage = es.message
                    )
                }
            }
            EmergencySOSState.Idle, EmergencySOSState.Preparing -> {
                // No active session — keep the user on Landing/Swipe.
            }
        }
    }

    private fun maybeRecordHistory(alertId: String?) {
        if (recordedHistory || alertId.isNullOrBlank()) return
        recordedHistory = true
        val repo = sosHistoryRepository ?: return
        viewModelScope.launch {
            runCatching {
                repo.recordSos(
                    requestId = alertId,
                    emergencyTypes = emptyList(),
                    status = "submitted",
                    locationText = null,
                    coordinates = null,
                    contactPhone = null,
                    additionalInfo = null,
                    triggerType = "pinch_flow"
                )
            }.onFailure { Timber.tag(TAG).w(it, "Local SOS history record failed") }
        }
    }

    // ─── navigation ─────────────────────────────────────────────────────────

    fun navigateToSwipe() { _ui.update { it.copy(screen = PinchScreen.Swipe) } }

    fun backToLanding() { _ui.update { it.copy(screen = PinchScreen.Landing) } }

    /** Called by the terminal-card "Done" button before the screen pops back. */
    fun dismissTerminal() {
        hasEnteredLive = false
        recordedHistory = false
        _ui.update {
            it.copy(
                screen = PinchScreen.Landing,
                isTerminal = false,
                terminalStatus = null,
                activeSheet = PinchSheet.None
            )
        }
    }

    // ─── intents → manager ──────────────────────────────────────────────────

    /** Swipe-to-confirm → fire the real signed `pinch-ingest`. */
    fun swipeForHelp() {
        if (_ui.value.screen == PinchScreen.Live) return
        _ui.update { it.copy(screen = PinchScreen.Live, isSending = true) }
        viewModelScope.launch { manager.startManualSOS(festivalId = festivalId) }
    }

    fun retrySend() {
        viewModelScope.launch { manager.retry() }
    }

    fun requestCancel() {
        viewModelScope.launch {
            manager.requestUserCancel().onFailure {
                _ui.update { s -> s.copy(transientMessage = "Couldn't cancel — try again") }
            }
        }
    }

    // ─── sheets ─────────────────────────────────────────────────────────────

    fun openSheet(sheet: PinchSheet) { _ui.update { it.copy(activeSheet = sheet) } }
    fun closeSheet() { _ui.update { it.copy(activeSheet = PinchSheet.None) } }
    fun clearTransient() { _ui.update { it.copy(transientMessage = null) } }

    // ─── pinch-alert-details submits ────────────────────────────────────────

    fun submitPhone(rawPhone: String) =
        submitDetail(PinchAlertDetail.Phone(phoneE164 = toE164(rawPhone)), "phone")

    fun submitMedical(selected: Set<String>) =
        submitDetail(PinchAlertDetail.Medical(medicalInfo = selected.toList()), "medical")

    fun submitIncident(categories: Set<String>, notes: String) =
        submitDetail(
            PinchAlertDetail.Incident(
                categories = categories.toList(),
                additionalNotes = notes.ifBlank { null }
            ),
            "incident"
        )

    fun submitIncidentDeclined() =
        submitDetail(PinchAlertDetail.IncidentDeclined, "incident")

    fun selectLocationZone(choice: String, description: String) =
        submitDetail(PinchAlertDetail.LocationChoice(choice, description), "location")

    /** "My current location" → on-demand signed `pinch-update-location`. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            _ui.update { it.copy(sheetSubmitting = true) }
            val res = manager.pushCurrentLocationNow()
            _ui.update {
                it.copy(
                    sheetSubmitting = false,
                    activeSheet = if (res.isSuccess) PinchSheet.None else it.activeSheet,
                    transientMessage = if (res.isSuccess) "Location shared with staff"
                    else "Couldn't share location — check GPS"
                )
            }
        }
    }

    private fun submitDetail(detail: PinchAlertDetail, kind: String) {
        viewModelScope.launch {
            _ui.update { it.copy(sheetSubmitting = true) }
            val updateId = NonceGenerator.newClientUpdateId(kind)
            val res = manager.submitDetails(detail, updateId)
            _ui.update {
                it.copy(
                    sheetSubmitting = false,
                    activeSheet = if (res.isSuccess) PinchSheet.None else it.activeSheet,
                    transientMessage = if (res.isSuccess) "Sent to staff"
                    else "Couldn't send — try again"
                )
            }
        }
    }

    // ─── live GPS marker ────────────────────────────────────────────────────

    fun initLocationIfNeeded(context: Context) {
        if (locationStarted) return
        locationStarted = true
        val app = context.applicationContext
        viewModelScope.launch {
            while (true) {
                val loc = getCurrentLocation(app)
                _ui.update {
                    it.copy(userLatLng = loc ?: it.userLatLng, mapLocationLoaded = true)
                }
                delay(LOCATION_REFRESH_MS)
            }
        }
    }

    private fun toE164(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("+")) return "+" + trimmed.drop(1).filter { it.isDigit() }
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+1$digits"            // US 10-digit
            digits.length == 11 && digits.startsWith("1") -> "+$digits"
            else -> "+$digits"
        }
    }

    private companion object {
        const val TAG = "PinchSosViewModel"
        const val LOCATION_REFRESH_MS = 10_000L
    }

    class Factory(
        private val manager: EmergencySOSManager,
        private val networkMonitor: NetworkMonitor,
        private val sosHistoryRepository: SosHistoryRepository? = null,
        private val festivalId: String = AppConfig.DEFAULT_FESTIVAL_ID
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PinchSosViewModel(manager, networkMonitor, sosHistoryRepository, festivalId) as T
    }
}
