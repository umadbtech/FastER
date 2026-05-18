package com.faster.festival.wristband.domain.repository

import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.Telemetry
import kotlinx.coroutines.flow.Flow

interface WristbandMeshRepository {
    val connection: Flow<ConnectionStatus>
    val telemetry: Flow<Telemetry>
    val sosEvents: Flow<SosEvent>
    val sosCancels: Flow<SosCancel>
    val deviceEvents: Flow<DeviceEvent>

    fun pairNewWristband(): Flow<ProvisioningProgress>
    suspend fun reconnectIfPaired(): Result<Boolean>
    suspend fun sendSosAck(eventId: Long, helpDispatched: Boolean): Result<Unit>
    suspend fun sendResponderDispatched(eventId: Long, etaMinutes: Int): Result<Unit>
    suspend fun sendResolved(eventId: Long, falseAlarm: Boolean): Result<Unit>
    suspend fun sendConfig(key: Int, value: Int): Result<Unit>
    suspend fun unpair()
    suspend fun activeUnicast(): Int?
    suspend fun shutdown()
}
