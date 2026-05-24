package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import kotlinx.coroutines.flow.Flow

/**
 * App-facing wrapper over the BLE Mesh stack. The single production
 * implementation is [NordicMeshManager], backed by the Nordic nRF Mesh SDK
 * + [BleMeshGatt]. The interface is intentionally Nordic-agnostic so the
 * underlying stack can be swapped in one file.
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
