package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.local.WristbandRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProvisionStep {
    Splash,
    LocationPermission,
    PowerOn,
    Connecting,
    Detected,
    Confirm,
    Complete,
    QuickGuide
}

data class ProvisionUiState(
    val currentStep: ProvisionStep = ProvisionStep.Splash,
    val isLoading: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val wristbandId: String = "",
    val error: String? = null
)

class ProvisionViewModel(
    private val wristbandRepository: WristbandRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProvisionUiState())
    val uiState: StateFlow<ProvisionUiState> = _uiState.asStateFlow()

    fun proceedToLocationPermission() {
        _uiState.value = _uiState.value.copy(currentStep = ProvisionStep.LocationPermission)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = granted,
            currentStep = ProvisionStep.PowerOn
        )
    }

    fun activatePairingMode() {
        _uiState.value = _uiState.value.copy(currentStep = ProvisionStep.Connecting)
        simulateWristbandScan()
    }

    private fun simulateWristbandScan() {
        viewModelScope.launch {
            delay(2500)
            _uiState.value = _uiState.value.copy(
                currentStep = ProvisionStep.Detected,
                wristbandId = "FSTR-2026-A7B3"
            )
        }
    }

    fun pairWristband() {
        _uiState.value = _uiState.value.copy(currentStep = ProvisionStep.Confirm)
    }

    fun finishPairing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1200)

            // Persist the paired wristband to SQLite so the Faster/Home/Profile
            // screens can reflect it and it survives app restarts.
            val id = _uiState.value.wristbandId.ifBlank { "FSTR-2026-A7B3" }
            wristbandRepository?.savePairedWristband(
                wristbandId = id,
                deviceName = "FASTER Wristband",
                firmwareVersion = "2.1.4",
                batteryLevel = 82,
                connectionStatus = "Strong Connection"
            )

            _uiState.value = _uiState.value.copy(
                currentStep = ProvisionStep.Complete,
                isLoading = false
            )
        }
    }

    fun showQuickGuide() {
        _uiState.value = _uiState.value.copy(currentStep = ProvisionStep.QuickGuide)
    }

    fun goBack() {
        val prev = when (_uiState.value.currentStep) {
            ProvisionStep.Splash -> null
            ProvisionStep.LocationPermission -> ProvisionStep.Splash
            ProvisionStep.PowerOn -> ProvisionStep.LocationPermission
            ProvisionStep.Connecting -> ProvisionStep.PowerOn
            ProvisionStep.Detected -> ProvisionStep.PowerOn
            ProvisionStep.Confirm -> ProvisionStep.Detected
            ProvisionStep.Complete -> null
            ProvisionStep.QuickGuide -> ProvisionStep.Complete
        }
        if (prev != null) {
            _uiState.value = _uiState.value.copy(currentStep = prev)
        }
    }

    fun reset() {
        _uiState.value = ProvisionUiState()
    }

    class Factory(
        private val wristbandRepository: WristbandRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProvisionViewModel(wristbandRepository) as T
        }
    }
}
