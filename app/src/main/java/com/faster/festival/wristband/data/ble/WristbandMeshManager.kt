package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import kotlinx.coroutines.flow.Flow

/**
 * App-facing wrapper over whichever BLE Mesh stack is in use. Two
 * implementations ship today:
 *
 *  • [FakeMeshManager] — drives the entire flow without hardware. Used in
 *    DEBUG by default so the app is demoable end-to-end.
 *  • Real Nordic-backed implementation — TODO drop in `NordicMeshManager`
 *    + `BleMeshGatt` when the Nordic nRF Mesh dependency is added. The
 *    interface here is intentionally Nordic-agnostic so the swap is one file.
 */
interface WristbandMeshManager {
    val connection: Flow<ConnectionStatus>
    val inboundVendor: Flow<VendorPayloadCodec.Inbound>

    fun provision(timeoutMs: Long = 60_000L): Flow<ProvisioningProgress>
    suspend fun reconnect(unicastAddress: Int): Result<Unit>
    suspend fun sendVendor(unicastAddress: Int, payload: ByteArray): Result<Unit>
    suspend fun unpair(unicastAddress: Int)
    suspend fun shutdown()
}
