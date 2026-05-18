package com.faster.festival.wristband.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.core.sos.EmergencySOSManager
import com.faster.festival.core.sos.EmergencySOSState
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.SosState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel that backs [com.faster.festival.wristband.ui.sos.SosAlertScreen]
 * — the full-screen overlay shown when a wristband SOS arrives.
 *
 * After the unified [EmergencySOSManager] landed, the VM stops owning the
 * BLE ACK and the backend round-trip — both of those happen inside the
 * manager. The VM is now a UI projection of the manager's [EmergencySOSState]
 * mapped onto the overlay-friendly [SosAlertUiState] the screen already
 * consumes (preserved verbatim so the Compose layer is unchanged).
 */
data class SosAlertUiState(
    val event: SosEvent? = null,
    val state: SosState = SosState.Idle,
    val ackSent: Boolean = false,
    val responderDispatched: Boolean = false,
    val resolved: Boolean = false,
    val canceled: Boolean = false,
    val errorMessage: String? = null
)

class SosAlertViewModel(
    private val emergency: EmergencySOSManager
) : ViewModel() {

    private val _state = MutableStateFlow(SosAlertUiState())
    val state: StateFlow<SosAlertUiState> = _state.asStateFlow()

    init {
        observeEmergency()
    }

    fun onResponderDispatched(etaMinutes: Int) = viewModelScope.launch {
        emergency.markResponderDispatched(etaMinutes)
    }

    fun onResolved(falseAlarm: Boolean = false) = viewModelScope.launch {
        emergency.markResolved(falseAlarm)
    }

    fun dismiss() {
        // Idempotent — manager handles the case where there's no active session.
        viewModelScope.launch { emergency.cancelByUser() }
        _state.value = SosAlertUiState()
    }

    private fun observeEmergency() {
        viewModelScope.launch {
            emergency.state.collectLatest { es ->
                when (es) {
                    EmergencySOSState.Idle, EmergencySOSState.Preparing -> {
                        _state.value = SosAlertUiState()
                    }
                    is EmergencySOSState.Sending -> {
                        // Render only when the source is wristband — the
                        // overlay is the wristband-flow surface. Manual SOS
                        // shows the FasterScreen-side UI instead.
                        val wb = es.session.wristbandEvent
                        if (wb != null) {
                            _state.value = SosAlertUiState(
                                event = SosEvent(
                                    eventId = wb.eventId,
                                    state = SosState.Active,
                                    deviceUptimeMs = wb.deviceUptimeMs,
                                    retryCount = wb.retryCount,
                                    batteryPct = wb.batteryPct,
                                    receivedAtMs = es.session.startedAtEpochMs
                                ),
                                state = SosState.Active
                            )
                        }
                    }
                    is EmergencySOSState.Active -> {
                        val wb = es.session.wristbandEvent ?: return@collectLatest
                        _state.update {
                            it.copy(
                                event = SosEvent(
                                    eventId = wb.eventId,
                                    state = SosState.Confirmed,
                                    deviceUptimeMs = wb.deviceUptimeMs,
                                    retryCount = wb.retryCount,
                                    batteryPct = wb.batteryPct,
                                    receivedAtMs = es.session.startedAtEpochMs
                                ),
                                state = mapStatus(es.userStatus.name),
                                ackSent = true,
                                responderDispatched = es.responderName != null ||
                                        es.etaMinutes != null,
                                errorMessage = null
                            )
                        }
                    }
                    is EmergencySOSState.Resolved -> {
                        _state.update { it.copy(resolved = true, state = SosState.Resolved) }
                    }
                    is EmergencySOSState.Cancelled -> {
                        _state.update { it.copy(canceled = true, state = SosState.Canceled) }
                    }
                    is EmergencySOSState.Failed -> {
                        _state.update { it.copy(errorMessage = es.message) }
                    }
                }
            }
        }
    }

    /**
     * Map the backend `user_status` enum onto the firmware-side [SosState]
     * the overlay UI was originally written against. Keeps the Compose layer
     * untouched.
     */
    private fun mapStatus(userStatusName: String): SosState = when (userStatusName) {
        "SosReceived", "DispatchReceived", "DispatchConfirmed" -> SosState.Confirmed
        "ResponderAssigned", "ResponderAccepted", "ResponderEnRoute",
        "ResponderOnScene" -> SosState.Responder
        "Resolved", "Closed" -> SosState.Resolved
        "Rejected", "CancelledByUser" -> SosState.Canceled
        else -> SosState.Active
    }

    class Factory(
        private val emergency: EmergencySOSManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SosAlertViewModel(emergency) as T
    }
}
