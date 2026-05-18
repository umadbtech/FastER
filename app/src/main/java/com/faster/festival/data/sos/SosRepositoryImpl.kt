package com.faster.festival.data.sos

import com.faster.festival.BuildConfig
import com.faster.festival.core.crypto.DeviceSignatureManager
import com.faster.festival.core.crypto.NonceGenerator
import com.faster.festival.data.sos.local.DeviceIdentityStore
import com.faster.festival.data.sos.remote.AttestationExpiredException
import com.faster.festival.data.sos.remote.DeviceContext
import com.faster.festival.data.sos.remote.LocationUpdateRequest
import com.faster.festival.data.sos.remote.SosAlert
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.SosTriggerRequest
import com.faster.festival.data.sos.remote.WristbandInfo
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.domain.sos.TriggerHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter

class SosRepositoryImpl(
    private val identityStore: DeviceIdentityStore,
    private val remote: SosRemoteDataSource,
    private val signatureManager: DeviceSignatureManager,
    private val deviceRegistration: DeviceRegistrationManager,
    private val appVersion: String = BuildConfig.VERSION_NAME
) : SosRepository {

    override val isDeviceReady: Flow<Boolean> =
        combine(
            identityStore.deviceIdFlow,
            identityStore.attestationVerifiedFlow
        ) { id, verified -> !id.isNullOrBlank() && verified }

    override suspend fun deviceId(): String? = identityStore.deviceId()

    override suspend fun bootstrap() = deviceRegistration.bootstrap()

    override suspend fun triggerSos(
        clientTriggerId: String,
        festivalId: String,
        location: SosLocation?,
        wristband: WristbandInfo
    ): Result<TriggerHandle> =
        triggerSosInternal(clientTriggerId, festivalId, location, wristband, recoveryAttempted = false)

    private suspend fun triggerSosInternal(
        clientTriggerId: String,
        festivalId: String,
        location: SosLocation?,
        wristband: WristbandInfo,
        recoveryAttempted: Boolean
    ): Result<TriggerHandle> {
        val deviceId = identityStore.deviceId()
            ?: return Result.failure(
                IllegalStateException("SOS device not registered — call bootstrap() first")
            )

        // Do NOT regenerate clientTriggerId on retry — caller is responsible.
        val nonce = NonceGenerator.newNonce()
        val timestamp = Instant.now().let { DateTimeFormatter.ISO_INSTANT.format(it) }

        // Per spec the `location` block is always present in the payload.
        // SosLocationProvider is the sole owner of "what coordinate to send"
        // — it returns the staging mock when `USE_TEST_LOCATION` is enabled
        // and a real FLP fix otherwise. If the caller still hands us `null`
        // here (production GPS truly unavailable + last-known unavailable),
        // we send a 0,0 sentinel that dispatch can detect as "fix missing"
        // — never a hardcoded test coordinate.
        if (location == null) {
            Timber.tag(TAG).w(
                "No location fix available — sending 0,0 sentinel for trigger=%s",
                clientTriggerId.takeLast(8)
            )
        }
        val payload = SosTriggerRequest(
            clientTriggerId = clientTriggerId,
            festivalId = festivalId,
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            location = location ?: GPS_UNAVAILABLE_SENTINEL,
            wristband = wristband,
            deviceContext = DeviceContext(
                appVersion = appVersion,
                sentAt = timestamp
            )
        )

        // Facade enforces the serialize-once → hash → sign invariant.
        val signed = signatureManager.signPinchIngest(
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            payload = payload
        )

        Timber.tag(TAG).d(
            "pinch-ingest send | trigger=%s nonce=%s body_sha=%s…",
            clientTriggerId.takeLast(8), nonce.take(8), signed.bodySha256Hex.take(8)
        )

        val result = remote.pinchIngest(
            rawJson = signed.rawJson,
            signatureB64 = signed.signatureBase64,
            bodySha256Hex = signed.bodySha256Hex
        )
        val err = result.exceptionOrNull()
        if (err is AttestationExpiredException && !recoveryAttempted) {
            Timber.tag(TAG).w(
                "pinch-ingest 403 attestation expired — auto re-attesting | trigger=%s",
                clientTriggerId.takeLast(8)
            )
            val reattest = deviceRegistration.reattest()
            if (reattest is DeviceRegistrationManager.BootstrapResult.Ready) {
                Timber.tag(TAG).i("Re-attestation succeeded — retrying pinch-ingest once")
                return triggerSosInternal(
                    clientTriggerId = clientTriggerId,
                    festivalId = festivalId,
                    location = location,
                    wristband = wristband,
                    recoveryAttempted = true
                )
            } else {
                val cause = (reattest as? DeviceRegistrationManager.BootstrapResult.Failed)?.cause
                Timber.tag(TAG).e(cause, "Re-attestation FAILED — surfacing original 403")
                return Result.failure(err)
            }
        }
        return result.fold(
            onSuccess = { resp ->
                Result.success(
                    TriggerHandle(
                        clientTriggerId = clientTriggerId,
                        alertId = resp.alertId,
                        initialStatus = SosUserStatus.fromRaw(resp.status)
                    )
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun pollStatus(clientTriggerId: String): Result<SosAlert?> =
        // Backend response is FLAT — no `alert` wrapper. The full
        // SosStatusResponse IS the alert (SosAlert is a typealias).
        remote.pinchAlertStatus(clientTriggerId).map { it as SosAlert? }

    override suspend fun sendLocationUpdate(
        clientTriggerId: String,
        trackingSessionId: String,
        location: SosLocation
    ): Result<Unit> =
        sendLocationUpdateInternal(clientTriggerId, trackingSessionId, location, recoveryAttempted = false)

    private suspend fun sendLocationUpdateInternal(
        clientTriggerId: String,
        trackingSessionId: String,
        location: SosLocation,
        recoveryAttempted: Boolean
    ): Result<Unit> {
        val deviceId = identityStore.deviceId()
            ?: return Result.failure(
                IllegalStateException("SOS device not registered — call bootstrap() first")
            )
        val nonce = NonceGenerator.newNonce()
        val timestamp = Instant.now().let { DateTimeFormatter.ISO_INSTANT.format(it) }

        val payload = LocationUpdateRequest(
            clientTriggerId = clientTriggerId,
            trackingSessionId = trackingSessionId,
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            location = location,
            deviceContext = DeviceContext(
                appVersion = appVersion,
                sentAt = timestamp
            )
        )

        val signed = signatureManager.signPinchUpdateLocation(
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            payload = payload
        )

        Timber.tag(TAG).d(
            "pinch-update-location send | trigger=%s nonce=%s body_sha=%s…",
            clientTriggerId.takeLast(8), nonce.take(8), signed.bodySha256Hex.take(8)
        )

        val result = remote.pinchUpdateLocation(
            rawJson = signed.rawJson,
            signatureB64 = signed.signatureBase64,
            bodySha256Hex = signed.bodySha256Hex
        )
        val err = result.exceptionOrNull()
        if (err is AttestationExpiredException && !recoveryAttempted) {
            Timber.tag(TAG).w(
                "pinch-update-location 403 attestation expired — auto re-attesting | trigger=%s",
                clientTriggerId.takeLast(8)
            )
            val reattest = deviceRegistration.reattest()
            if (reattest is DeviceRegistrationManager.BootstrapResult.Ready) {
                Timber.tag(TAG).i("Re-attestation succeeded — retrying pinch-update-location once")
                return sendLocationUpdateInternal(
                    clientTriggerId, trackingSessionId, location, recoveryAttempted = true
                )
            } else {
                val cause = (reattest as? DeviceRegistrationManager.BootstrapResult.Failed)?.cause
                Timber.tag(TAG).e(cause, "Re-attestation FAILED — surfacing original 403")
                return Result.failure(err)
            }
        }
        return result.map { Unit }
    }

    override suspend fun resetTrustedDevice() = deviceRegistration.reset()

    private companion object {
        const val TAG = "SosRepository"

        /**
         * "GPS unavailable" sentinel — production builds where the device
         * genuinely couldn't get a fix (no permission, location services
         * disabled, indoors with no last-known). Dispatch can detect 0,0
         * and treat the case explicitly. Test coordinates are NOT used
         * here — those live exclusively inside [SosLocationProvider] behind
         * the `USE_TEST_LOCATION` build flag.
         */
        val GPS_UNAVAILABLE_SENTINEL = SosLocation(
            latitude = 0.0,
            longitude = 0.0,
            accuracyMeters = null
        )
    }
}

// ─── Retry classifier (used by use cases) ──────────────────────────────────

/**
 * Returns true only for transport-level failures or 5xx — we re-sign with a
 * fresh nonce/timestamp on these. 4xx (signature mismatch, malformed payload,
 * 401, etc.) is non-retryable: a re-sign produces the same failure.
 */
fun Throwable.isSosRetryable(): Boolean = when (this) {
    is IOException -> true       // network / DNS / read-timeout
    is HttpException -> code() in 500..599
    else -> false
}
