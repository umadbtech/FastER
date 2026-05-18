package com.faster.festival.wristband.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.Telemetry
import com.faster.festival.wristband.domain.usecase.ObserveConnectionUseCase
import com.faster.festival.wristband.domain.usecase.ObserveDeviceEventsUseCase
import com.faster.festival.wristband.domain.usecase.ObserveTelemetryUseCase
import com.faster.festival.wristband.domain.usecase.ReconnectWristbandUseCase
import com.faster.festival.wristband.domain.usecase.UnpairWristbandUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val connection: ConnectionStatus = ConnectionStatus.Idle,
    val deviceName: String = "FASTER Wristband",
    val deviceId: String = "",
    val telemetry: Telemetry? = null,
    val lastSyncMs: Long? = null,
    val lastEvent: DeviceEvent? = null
)

class WristbandDashboardViewModel(
    pairedRepo: WristbandRepository,
    observeConnection: ObserveConnectionUseCase,
    observeTelemetry: ObserveTelemetryUseCase,
    observeDeviceEvents: ObserveDeviceEventsUseCase,
    private val reconnectUseCase: ReconnectWristbandUseCase,
    private val unpairUseCase: UnpairWristbandUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        // Identity comes from the persisted Room row — the same row written
        // by WristbandMeshRepositoryImpl.pairNewWristband() on success.
        viewModelScope.launch {
            pairedRepo.activeWristband.collect { paired ->
                _state.update {
                    it.copy(
                        deviceId = paired?.wristbandId ?: "",
                        deviceName = paired?.deviceName ?: "FASTER Wristband"
                    )
                }
            }
        }
        viewModelScope.launch {
            observeConnection().collect { c -> _state.update { it.copy(connection = c) } }
        }
        viewModelScope.launch {
            observeTelemetry().collect { t ->
                _state.update {
                    it.copy(telemetry = t, lastSyncMs = System.currentTimeMillis())
                }
            }
        }
        viewModelScope.launch {
            observeDeviceEvents().collect { e -> _state.update { it.copy(lastEvent = e) } }
        }
    }

    fun reconnect() = viewModelScope.launch { reconnectUseCase() }

    /**
     * Sequence the suspend teardown before navigating. Avoids a race where the
     * UI navigates away while the GATT disconnect / Room delete is still in
     * flight — without this the user could see a half-paired FASTER tab for a
     * frame after tapping Unpair.
     */
    fun unpair(onDone: () -> Unit) = viewModelScope.launch {
        unpairUseCase()
        onDone()
    }

    class Factory(
        private val pairedRepo: WristbandRepository,
        private val observeConnection: ObserveConnectionUseCase,
        private val observeTelemetry: ObserveTelemetryUseCase,
        private val observeDeviceEvents: ObserveDeviceEventsUseCase,
        private val reconnect: ReconnectWristbandUseCase,
        private val unpair: UnpairWristbandUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WristbandDashboardViewModel(
                pairedRepo, observeConnection, observeTelemetry,
                observeDeviceEvents, reconnect, unpair
            ) as T
    }
}
