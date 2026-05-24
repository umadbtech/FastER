package com.faster.festival.data.sos

import com.faster.festival.BuildConfig
import com.faster.festival.core.crypto.DeviceSignatureManager
import com.faster.festival.core.crypto.NonceGenerator
import com.faster.festival.data.sos.local.DeviceIdentityStore
import com.faster.festival.data.sos.remote.AttestationExpiredException
import com.faster.festival.data.sos.remote.CancelRequest
import com.faster.festival.data.sos.remote.DeviceRegistrationRequiredException
import com.faster.festival.data.sos.remote.DeviceContext
import com.faster.festival.data.sos.remote.LocationUpdateRequest
import com.faster.festival.data.sos.remote.SosAlert
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.SosTriggerRequest
import com.faster.festival.data.sos.remote.WristbandInfo
import com.faster.festival.domain.sos.PinchAlertDetail
import com.faster.festival.domain.sos.PinchUiStatus
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.domain.sos.TriggerHandle
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
        // SosLocationProvider is the sole owner of "what coordinate to send" —
        // it returns a real, validated FLP fix or nothing at all (there is no
        // mock/test path in any build). If the caller hands us `null` here
        // (GPS truly unavailable: no permission, services off, or only a
        // stale/invalid fix), we send the 0,0 sentinel that dispatch can detect
        // as "fix missing" so the emergency still goes through — never a
        // fabricated or hardcoded coordinate.
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
        if (err is DeviceRegistrationRequiredException && !recoveryAttempted) {
            Timber.tag(TAG).w(
                "pinch-ingest 403 device registration required — auto re-registering | trigger=%s",
                clientTriggerId.takeLast(8)
            )
            val rereg = deviceRegistration.reregister()
            if (rereg is DeviceRegistrationManager.BootstrapResult.Ready) {
                Timber.tag(TAG).i("Re-registration succeeded — retrying pinch-ingest once")
                return triggerSosInternal(
                    clientTriggerId = clientTriggerId,
                    festivalId = festivalId,
                    location = location,
                    wristband = wristband,
                    recoveryAttempted = true
                )
            } else {
                val cause = (rereg as? DeviceRegistrationManager.BootstrapResult.Failed)?.cause
                Timber.tag(TAG).e(cause, "Re-registration FAILED — surfacing original 403")
                return Result.failure(err)
            }
        }
        return result.fold(
            onSuccess = { resp ->
                Result.success(
                    TriggerHandle(
                        clientTriggerId = clientTriggerId,
                        alertId = resp.alertId,
                        initialStatus = SosUserStatus.fromRaw(resp.status),
                        trackingSessionId = resp.trackingSessionId,
                        initialUiStatus = PinchUiStatus.fromRaw(resp.uiStatus)
                    )
                )
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun pollStatus(
        alertId: String?,
        clientTriggerId: String?
    ): Result<SosAlert?> =
        remote.pinchAlertStatus(alertId, clientTriggerId).map { it as SosAlert? }

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
            trackingSessionId = trackingSessionId,
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            location = location
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
        if (err is DeviceRegistrationRequiredException && !recoveryAttempted) {
            Timber.tag(TAG).w(
                "pinch-update-location 403 device registration required — auto re-registering | trigger=%s",
                clientTriggerId.takeLast(8)
            )
            val rereg = deviceRegistration.reregister()
            if (rereg is DeviceRegistrationManager.BootstrapResult.Ready) {
                return sendLocationUpdateInternal(
                    clientTriggerId, trackingSessionId, location, recoveryAttempted = true
                )
            }
            return Result.failure(err)
        }
        return result.map { Unit }
    }

    // ─── pinch-alert-details (signed partial update) ────────────────────────

    override suspend fun sendAlertDetails(
        alertId: String,
        detail: PinchAlertDetail,
        clientUpdateId: String
    ): Result<Unit> =
        sendAlertDetailsInternal(alertId, detail, clientUpdateId, recoveryAttempted = false)

    private suspend fun sendAlertDetailsInternal(
        alertId: String,
        detail: PinchAlertDetail,
        clientUpdateId: String,
        recoveryAttempted: Boolean
    ): Result<Unit> {
        val deviceId = identityStore.deviceId()
            ?: return Result.failure(
                IllegalStateException("SOS device not registered — call bootstrap() first")
            )
        val nonce = NonceGenerator.newNonce()
        val timestamp = Instant.now().let { DateTimeFormatter.ISO_INSTANT.format(it) }

        // Build ONLY the keys the contract specifies for this detail kind so the
        // signed byte string matches exactly (no null padding).
        val payload = buildJsonObject {
            put("alert_id", alertId)
            put("client_update_id", clientUpdateId)
            put("device_id", deviceId)
            put("nonce", nonce)
            put("timestamp", timestamp)
            when (detail) {
                is PinchAlertDetail.Phone -> {
                    put("contact_phone_e164", detail.phoneE164)
                    put("contact_phone_source", detail.source)
                }
                is PinchAlertDetail.Medical -> putJsonObject("details") {
                    putJsonArray("medical_info") { detail.medicalInfo.forEach { add(it) } }
                }
                is PinchAlertDetail.Incident -> {
                    putJsonArray("emergency_categories") { detail.categories.forEach { add(it) } }
                    detail.severityHint?.let { put("severity_hint", it) }
                    detail.additionalNotes?.let { put("additional_notes", it) }
                }
                PinchAlertDetail.IncidentDeclined -> put("additional_notes_declined", true)
                is PinchAlertDetail.LocationChoice -> {
                    put("location_choice", detail.choice)
                    put("location_description", detail.description)
                }
            }
        }

        val signed = signatureManager.signPinchAlertDetails(
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            payload = payload
        )

        Timber.tag(TAG).d(
            "pinch-alert-details send | kind=%s alert=%s update=%s",
            detail.kind, alertId.takeLast(8), clientUpdateId.takeLast(8)
        )

        val result = remote.pinchAlertDetails(
            rawJson = signed.rawJson,
            signatureB64 = signed.signatureBase64,
            bodySha256Hex = signed.bodySha256Hex
        )
        val err = result.exceptionOrNull()
        if (err is AttestationExpiredException && !recoveryAttempted) {
            val reattest = deviceRegistration.reattest()
            if (reattest is DeviceRegistrationManager.BootstrapResult.Ready) {
                return sendAlertDetailsInternal(alertId, detail, clientUpdateId, recoveryAttempted = true)
            }
            return Result.failure(err)
        }
        if (err is DeviceRegistrationRequiredException && !recoveryAttempted) {
            val rereg = deviceRegistration.reregister()
            if (rereg is DeviceRegistrationManager.BootstrapResult.Ready) {
                return sendAlertDetailsInternal(alertId, detail, clientUpdateId, recoveryAttempted = true)
            }
            return Result.failure(err)
        }
        return result.map { Unit }
    }

    // ─── pinch-cancel (unsigned) ────────────────────────────────────────────

    override suspend fun cancelSos(
        alertId: String,
        clientRequestId: String,
        reason: String
    ): Result<PinchUiStatus> =
        remote.pinchCancel(
            CancelRequest(alertId = alertId, clientRequestId = clientRequestId, reason = reason)
        ).map { PinchUiStatus.fromRaw(it.uiStatus) }

    // ─── pinch-alert-history (unsigned) ─────────────────────────────────────

    override suspend fun fetchAlertHistory(
        limit: Int,
        festivalId: String?,
        cursor: String?
    ): Result<com.faster.festival.data.sos.remote.PinchAlertHistoryResponse> =
        remote.pinchAlertHistory(limit = limit, festivalId = festivalId, cursor = cursor)

    override suspend fun resetTrustedDevice() = deviceRegistration.reset()

    private companion object {
        const val TAG = "SosRepository"

        /**
         * "GPS unavailable" sentinel — used only when the device genuinely
         * couldn't produce a valid fix (no permission, location services
         * disabled, or indoors with no recent last-known). Dispatch detects 0,0
         * and treats the case explicitly. This is an honest "unknown" marker,
         * NOT a fake coordinate: there is no mock/test/hardcoded location path
         * anywhere — [SosLocationProvider] only ever returns real, validated
         * GPS or `null`.
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
