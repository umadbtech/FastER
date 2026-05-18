package com.faster.festival.domain.sos

import com.faster.festival.data.sos.DeviceRegistrationManager
import com.faster.festival.data.sos.remote.SosAlert
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.WristbandInfo
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing port for the SOS trusted-device flow. The repository owns:
 *  • Idempotent device bootstrap (delegated to [DeviceRegistrationManager])
 *  • Signed `pinch-ingest` with retry orchestration
 *  • Status polling
 */
interface SosRepository {

    /** True after device key + register + verify-attestation have all succeeded. */
    val isDeviceReady: Flow<Boolean>

    /** Device id assigned by the backend, persisted locally. */
    suspend fun deviceId(): String?

    suspend fun bootstrap(): DeviceRegistrationManager.BootstrapResult

    /**
     * Trigger an SOS event. Generates one [TriggerHandle] per call — handle
     * carries the `client_trigger_id` to use for retries / polling.
     */
    suspend fun triggerSos(
        clientTriggerId: String,
        festivalId: String,
        location: SosLocation?,
        wristband: WristbandInfo
    ): Result<TriggerHandle>

    /** Poll once. Caller drives cadence — see [PollSOSStatusUseCase]. */
    suspend fun pollStatus(clientTriggerId: String): Result<SosAlert?>

    /**
     * Periodic GPS push for an in-flight SOS. Signed with the same Ed25519
     * key as `pinch-ingest`, canonical path `/pinch-update-location`.
     *
     * Caller (the foreground service) drives cadence and is responsible for
     * NOT calling this until dispatch has handed us a [trackingSessionId]
     * (`pinch-alert-status.tracking_session_id`).
     */
    suspend fun sendLocationUpdate(
        clientTriggerId: String,
        trackingSessionId: String,
        location: SosLocation
    ): Result<Unit>

    /** Hard reset — clears device id, attestation flag, signing key. */
    suspend fun resetTrustedDevice()
}

/**
 * Returned from a successful [SosRepository.triggerSos]. Carries the alert
 * id (when the server returns one) and the `clientTriggerId` to use for
 * polling / retries.
 */
data class TriggerHandle(
    val clientTriggerId: String,
    val alertId: String?,
    val initialStatus: SosUserStatus
)
