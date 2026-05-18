package com.faster.festival.presentation.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.core.sos.EmergencySOSManager
import com.faster.festival.core.sos.EmergencySOSState
import com.faster.festival.domain.sos.SosUserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the in-app "Get Medical Help" trigger.
 *
 * After the unified [EmergencySOSManager] landed, this VM is a thin lens:
 *  • `triggerSos(festivalId)` delegates to [EmergencySOSManager.startManualSOS]
 *  • `cancelPolling()` / dismiss → [EmergencySOSManager.cancelByUser]
 *  • State is derived from [EmergencySOSManager.state] + [SOSSetupManager.readiness]
 *
 * The VM no longer owns its own polling loop, retry handling, or backend
 * round-trip — those live in the manager so the wristband path and manual
 * path can never produce two backend alerts for one emergency.
 */
class SOSViewModel(
    private val setup: SOSSetupManager,
    private val emergency: EmergencySOSManager
) : ViewModel() {

    private val _state = MutableStateFlow<SOSUiState>(SOSUiState.Idle)
    val state: StateFlow<SOSUiState> = _state.asStateFlow()

    init {
        observeReadiness()
        observeEmergency()
        setup.ensureSetup()
    }

    // ─── Public API (unchanged surface for callers) ────────────────────────

    fun triggerSos(festivalId: String) {
        when (val s = _state.value) {
            is SOSUiState.Ready, is SOSUiState.TriggerFailed -> Unit
            else -> {
                Timber.tag(TAG).w("triggerSos called from non-ready state: %s", s)
                return
            }
        }
        viewModelScope.launch { emergency.startManualSOS(festivalId) }
    }

    fun retryTrigger() {
        if (_state.value !is SOSUiState.TriggerFailed) return
        viewModelScope.launch { emergency.retry() }
    }

    fun cancelPolling() {
        viewModelScope.launch { emergency.cancelByUser() }
    }

    fun retrySetup() = setup.ensureSetup()

    // ─── State translation ─────────────────────────────────────────────────

    private fun observeReadiness() {
        viewModelScope.launch {
            setup.readiness.collectLatest { readiness ->
                // Don't overwrite an active SOS surface with a setup state.
                val current = _state.value
                if (current is SOSUiState.Sending ||
                    current is SOSUiState.InProgress ||
                    current is SOSUiState.Resolved ||
                    current is SOSUiState.TriggerFailed
                ) return@collectLatest

                _state.value = when (readiness) {
                    SOSSetupManager.Readiness.Unknown -> SOSUiState.Idle
                    SOSSetupManager.Readiness.SetupInProgress -> SOSUiState.SetupInProgress
                    SOSSetupManager.Readiness.Ready -> SOSUiState.Ready
                    SOSSetupManager.Readiness.OnboardingRequired -> SOSUiState.OnboardingRequired
                    SOSSetupManager.Readiness.Failed ->
                        SOSUiState.SetupFailed("Setup failed. Tap to retry.")
                }
            }
        }
    }

    private fun observeEmergency() {
        viewModelScope.launch {
            emergency.state.collectLatest { es ->
                _state.value = when (es) {
                    EmergencySOSState.Idle, EmergencySOSState.Preparing -> _state.value
                    is EmergencySOSState.Sending ->
                        SOSUiState.Sending(es.session.clientTriggerId)
                    is EmergencySOSState.Active ->
                        SOSUiState.InProgress(
                            clientTriggerId = es.session.clientTriggerId,
                            status = es.userStatus,
                            responderName = es.responderName,
                            etaMinutes = es.etaMinutes
                        )
                    is EmergencySOSState.Resolved ->
                        SOSUiState.Resolved(es.terminalStatus)
                    is EmergencySOSState.Cancelled ->
                        SOSUiState.Resolved(SosUserStatus.CancelledByUser)
                    is EmergencySOSState.Failed ->
                        SOSUiState.TriggerFailed(
                            clientTriggerId = es.session?.clientTriggerId ?: "",
                            message = es.message
                        )
                }
                // Once the manager reaches a terminal state, refresh setup
                // readiness so the next "Get Medical Help" tap finds Ready
                // instead of the stale Resolved/TriggerFailed surface.
                if (es is EmergencySOSState.Resolved ||
                    es is EmergencySOSState.Cancelled
                ) {
                    setup.ensureSetup()
                }
            }
        }
    }

    private companion object {
        const val TAG = "SOSViewModel"
    }

    class Factory(
        private val setup: SOSSetupManager,
        private val emergency: EmergencySOSManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SOSViewModel(setup, emergency) as T
    }
}
