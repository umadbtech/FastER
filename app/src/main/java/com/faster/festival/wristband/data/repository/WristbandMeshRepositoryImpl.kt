package com.faster.festival.wristband.data.repository

import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.wristband.data.ble.MeshNetworkStore
import com.faster.festival.wristband.data.ble.VendorPayloadCodec
import com.faster.festival.wristband.data.ble.WristbandMeshManager
import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.Telemetry
import com.faster.festival.wristband.domain.repository.WristbandMeshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class WristbandMeshRepositoryImpl(
    private val mesh: WristbandMeshManager,
    private val store: MeshNetworkStore,
    private val pairedRepo: WristbandRepository
) : WristbandMeshRepository {

    override val connection: Flow<ConnectionStatus> = mesh.connection

    override val telemetry: Flow<Telemetry> = mesh.inboundVendor
        .filterIsInstance<VendorPayloadCodec.Inbound.TelemetryMsg>()
        .map { it.telemetry }

    override val sosEvents: Flow<SosEvent> = mesh.inboundVendor
        .filterIsInstance<VendorPayloadCodec.Inbound.SosMsg>()
        .map { it.event }

    override val sosCancels: Flow<SosCancel> = mesh.inboundVendor
        .filterIsInstance<VendorPayloadCodec.Inbound.SosCancelMsg>()
        .map { it.cancel }

    override val deviceEvents: Flow<DeviceEvent> = mesh.inboundVendor
        .filterIsInstance<VendorPayloadCodec.Inbound.DeviceEventMsg>()
        .map { it.event }

    /**
     * The Room row is the user-facing mirror — written ONLY when provisioning
     * actually finishes successfully. The walkthrough's simulated success no
     * longer touches Room (see ProvisionViewModel).
     *
     * For [FakeMeshManager], the unicast lookup falls back to the default
     * starting address so the row still gets written and the FASTER screen
     * flips into "paired" mode.
     */
    override fun pairNewWristband(): Flow<ProvisioningProgress> =
        mesh.provision().onEach { p ->
            if (p.finished && !p.isFailed) {
                val unicast = store.unicastOnce()
                    ?: com.faster.festival.wristband.data.ble.MeshConstants.DEFAULT_UNICAST_START
                store.saveUnicast(unicast)
                store.touchLastSeen()
                pairedRepo.savePairedWristband(
                    wristbandId = "FSTR-%04X".format(unicast),
                    deviceName = "FASTER Wristband",
                    firmwareVersion = "—",
                    batteryLevel = 0,
                    connectionStatus = "Connected",
                    unicastAddress = unicast
                )
            }
        }

    override suspend fun reconnectIfPaired(): Result<Boolean> {
        val u = store.unicastOnce() ?: return Result.success(false)
        return mesh.reconnect(u).map { true }
    }

    override suspend fun sendSosAck(eventId: Long, helpDispatched: Boolean): Result<Unit> {
        val u = store.unicastOnce() ?: return Result.failure(IllegalStateException("not paired"))
        return mesh.sendVendor(u, VendorPayloadCodec.encodeSosAck(eventId, helpDispatched))
    }

    override suspend fun sendResponderDispatched(eventId: Long, etaMinutes: Int): Result<Unit> {
        val u = store.unicastOnce() ?: return Result.failure(IllegalStateException("not paired"))
        return mesh.sendVendor(u, VendorPayloadCodec.encodeSosResponder(eventId, etaMinutes))
    }

    override suspend fun sendResolved(eventId: Long, falseAlarm: Boolean): Result<Unit> {
        val u = store.unicastOnce() ?: return Result.failure(IllegalStateException("not paired"))
        return mesh.sendVendor(u, VendorPayloadCodec.encodeSosResolved(eventId, falseAlarm))
    }

    override suspend fun sendConfig(key: Int, value: Int): Result<Unit> {
        val u = store.unicastOnce() ?: return Result.failure(IllegalStateException("not paired"))
        return mesh.sendVendor(u, VendorPayloadCodec.encodeConfig(key, value))
    }

    override suspend fun unpair() {
        store.unicastOnce()?.let { mesh.unpair(it) }
        pairedRepo.unpair()
        store.clear()
    }

    override suspend fun activeUnicast(): Int? = store.unicastOnce()
    override suspend fun shutdown() = mesh.shutdown()
}
