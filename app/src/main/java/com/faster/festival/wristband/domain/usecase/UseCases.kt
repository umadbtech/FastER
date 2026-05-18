package com.faster.festival.wristband.domain.usecase

import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.Telemetry
import com.faster.festival.wristband.domain.repository.WristbandMeshRepository
import kotlinx.coroutines.flow.Flow

class ProvisionWristbandUseCase(private val repo: WristbandMeshRepository) {
    operator fun invoke(): Flow<ProvisioningProgress> = repo.pairNewWristband()
}
class ReconnectWristbandUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke(): Result<Boolean> = repo.reconnectIfPaired()
}
class ObserveTelemetryUseCase(private val repo: WristbandMeshRepository) {
    operator fun invoke(): Flow<Telemetry> = repo.telemetry
}
class ObserveSosEventsUseCase(private val repo: WristbandMeshRepository) {
    val events: Flow<SosEvent> = repo.sosEvents
    val cancels: Flow<SosCancel> = repo.sosCancels
}
class ObserveDeviceEventsUseCase(private val repo: WristbandMeshRepository) {
    operator fun invoke(): Flow<DeviceEvent> = repo.deviceEvents
}
class ObserveConnectionUseCase(private val repo: WristbandMeshRepository) {
    operator fun invoke(): Flow<ConnectionStatus> = repo.connection
}
class SendSosAckUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke(eventId: Long, helpDispatched: Boolean = true) =
        repo.sendSosAck(eventId, helpDispatched)
}
class SendSosResponderUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke(eventId: Long, etaMinutes: Int) =
        repo.sendResponderDispatched(eventId, etaMinutes)
}
class SendSosResolvedUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke(eventId: Long, falseAlarm: Boolean = false) =
        repo.sendResolved(eventId, falseAlarm)
}
class SendRemoteConfigUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke(key: Int, value: Int) = repo.sendConfig(key, value)
}
class UnpairWristbandUseCase(private val repo: WristbandMeshRepository) {
    suspend operator fun invoke() = repo.unpair()
}
