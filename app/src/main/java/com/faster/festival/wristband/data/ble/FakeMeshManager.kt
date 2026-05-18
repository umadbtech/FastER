package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.DeviceState
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.ProvisioningStep
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.SosState
import com.faster.festival.wristband.domain.model.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * No-hardware implementation. Default in [com.faster.festival.wristband.di.WristbandModule]
 * when `useFakeMesh = true`.
 *
 *  • [provision] walks the 9 real BLE Mesh steps with realistic delays, then
 *    flips on the long-running telemetry emitter.
 *  • Telemetry runs in a manager-scoped coroutine — it survives navigation
 *    away from the progress screen so the dashboard sees a live 1 Hz feed.
 *  • [reconnect] re-arms the same emitter, mimicking the real BLE Mesh node
 *    resuming its 1 s telemetry publish.
 *  • [emitFakeSos] lets previews / instrumentation drive the SOS overlay.
 */
class FakeMeshManager : WristbandMeshManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    override val connection = _connection.asStateFlow()

    private val _inbound = MutableSharedFlow<VendorPayloadCodec.Inbound>(extraBufferCapacity = 64)
    override val inboundVendor = _inbound.asSharedFlow()

    @Volatile private var provisioned: Boolean = false
    private var telemetryJob: Job? = null
    private var seq: Int = 0

    override fun provision(timeoutMs: Long): Flow<ProvisioningProgress> = flow {
        val tracker = ProvisioningProgressTracker()
        emit(tracker.snapshot())
        ProvisioningStep.values().forEach { step ->
            tracker.start(step); emit(tracker.snapshot())
            delay(700)
            tracker.complete(step); emit(tracker.snapshot())
        }
        provisioned = true
        _connection.value = ConnectionStatus.Connected
        startTelemetry()
        tracker.finish(); emit(tracker.snapshot())
        // Flow ends here. Telemetry continues in [scope] — independent of the
        // collector's lifecycle, so the dashboard sees a live feed even though
        // the progress VM is long gone.
    }

    override suspend fun reconnect(unicastAddress: Int): Result<Unit> {
        _connection.value = ConnectionStatus.Reconnecting
        delay(900)
        provisioned = true
        _connection.value = ConnectionStatus.Connected
        startTelemetry()
        return Result.success(Unit)
    }

    override suspend fun sendVendor(unicastAddress: Int, payload: ByteArray) = Result.success(Unit)

    override suspend fun unpair(unicastAddress: Int) {
        provisioned = false
        telemetryJob?.cancel()
        telemetryJob = null
        seq = 0
        _connection.value = ConnectionStatus.Idle
    }

    override suspend fun shutdown() {
        provisioned = false
        telemetryJob?.cancel()
        telemetryJob = null
        _connection.value = ConnectionStatus.Idle
    }

    /** Test helper: fire a synthetic SOS so previews / screenshots can show the alert. */
    suspend fun emitFakeSos(eventId: Long = System.currentTimeMillis() / 1000) {
        _inbound.emit(
            VendorPayloadCodec.Inbound.SosMsg(
                SosEvent(
                    eventId = eventId,
                    state = SosState.Active,
                    deviceUptimeMs = 3_141_592,
                    retryCount = 0,
                    batteryPct = 73,
                    receivedAtMs = System.currentTimeMillis()
                )
            )
        )
    }

    private fun startTelemetry() {
        if (telemetryJob?.isActive == true) return
        telemetryJob = scope.launch {
            while (provisioned) {
                delay(1_000)
                if (!provisioned) break
                seq++
                _inbound.emit(
                    VendorPayloadCodec.Inbound.TelemetryMsg(
                        Telemetry(
                            seqNum = seq,
                            accelX_g = Random.nextFloat() * 0.1f - 0.05f,
                            accelY_g = Random.nextFloat() * 0.1f - 0.05f,
                            accelZ_g = 1f + Random.nextFloat() * 0.05f,
                            peakMag_g = Random.nextFloat() * 0.3f,
                            motionDetected = Random.nextBoolean(),
                            batteryPct = (82 - (seq % 30)).coerceAtLeast(5),
                            deviceState = DeviceState.Operational,
                            receivedAtMs = System.currentTimeMillis()
                        )
                    )
                )
            }
        }
    }
}
