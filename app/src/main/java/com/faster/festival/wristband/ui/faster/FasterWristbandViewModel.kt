package com.faster.festival.wristband.ui.faster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.local.PairedWristband
import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.usecase.ObserveConnectionUseCase
import com.faster.festival.wristband.domain.usecase.ReconnectWristbandUseCase
import com.faster.festival.wristband.domain.usecase.UnpairWristbandUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the FASTER tab's wristband row. Combines:
 *  • Persisted [PairedWristband] from Room (filtered to skip legacy null-unicast rows)
 *  • Live [ConnectionStatus] from the BLE Mesh manager
 * → into a single [Mode] the screen branches on.
 */
data class FasterWristbandUiState(
    val paired: PairedWristband? = null,
    val connection: ConnectionStatus = ConnectionStatus.Idle,
    val unpairing: Boolean = false
) {
    enum class Mode { NotPaired, Connected, Disconnected, Reconnecting, Connecting }

    val mode: Mode get() = when {
        paired == null -> Mode.NotPaired
        connection is ConnectionStatus.Connected -> Mode.Connected
        connection is ConnectionStatus.Reconnecting -> Mode.Reconnecting
        connection is ConnectionStatus.Connecting -> Mode.Connecting
        else -> Mode.Disconnected
    }
}

class FasterWristbandViewModel(
    pairedRepo: WristbandRepository,
    observeConnection: ObserveConnectionUseCase,
    private val reconnectUseCase: ReconnectWristbandUseCase,
    private val unpairUseCase: UnpairWristbandUseCase
) : ViewModel() {

    val state: StateFlow<FasterWristbandUiState> = combine(
        pairedRepo.activeWristband,
        observeConnection()
    ) { paired, connection ->
        FasterWristbandUiState(paired = paired, connection = connection)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FasterWristbandUiState()
    )

    fun reconnect() = viewModelScope.launch { reconnectUseCase() }

    fun unpair(onDone: () -> Unit) = viewModelScope.launch {
        unpairUseCase()
        onDone()
    }

    class Factory(
        private val pairedRepo: WristbandRepository,
        private val observeConnection: ObserveConnectionUseCase,
        private val reconnect: ReconnectWristbandUseCase,
        private val unpair: UnpairWristbandUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FasterWristbandViewModel(pairedRepo, observeConnection, reconnect, unpair) as T
    }
}
