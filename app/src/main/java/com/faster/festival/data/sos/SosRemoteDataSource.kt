package com.faster.festival.data.sos

import com.faster.festival.data.sos.remote.AttestationExpiredException
import com.faster.festival.data.sos.remote.LocationUpdateResponse
import com.faster.festival.data.sos.remote.RegisterDeviceRequest
import com.faster.festival.data.sos.remote.RegisterDeviceResponse
import com.faster.festival.data.sos.remote.SosApiService
import com.faster.festival.data.sos.remote.SosIngestResponse
import com.faster.festival.data.sos.remote.SosStatusResponse
import com.faster.festival.data.sos.remote.VerifyAttestationRequest
import com.faster.festival.data.sos.remote.VerifyAttestationResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber

/**
 * Routes SOS calls to the right project — see [com.faster.festival.data.sos.remote.SosNetworkClient].
 *
 *   • [project1Api] — sos-register-device, sos-verify-attestation
 *   • [project2Api] — pinch-ingest, pinch-alert-status
 *
 * `pinch-ingest` takes the EXACT pre-serialized JSON byte string as
 * [okhttp3.RequestBody] so the body that gets hashed is the body that gets
 * sent over the wire. Re-serialization would invalidate the signature.
 */
class SosRemoteDataSource(
    private val project1Api: SosApiService,
    private val project2Api: SosApiService
) {

    // ─── Project 1 (app config + device registry + attestation) ────────────

    suspend fun registerDevice(req: RegisterDeviceRequest): Result<RegisterDeviceResponse> =
        runCatching { project1Api.registerDevice(req) }

    suspend fun verifyAttestation(
        req: VerifyAttestationRequest
    ): Result<VerifyAttestationResponse> =
        runCatching { project1Api.verifyAttestation(req) }

    // ─── Project 2 (signed SOS ingest + status polling) ────────────────────

    suspend fun pinchIngest(
        rawJson: String,
        signatureB64: String,
        bodySha256Hex: String
    ): Result<SosIngestResponse> = runCatching {
        val body = rawJson.toRequestBody(JSON_MEDIA)
        project2Api.pinchIngest(
            signature = signatureB64,
            signatureAlg = "ed25519",
            bodySha256 = bodySha256Hex,
            body = body
        )
    }.recoverCatching { throw it.translateOrPassthrough("pinch-ingest") }

    suspend fun pinchAlertStatus(clientTriggerId: String): Result<SosStatusResponse> =
        runCatching { project2Api.pinchAlertStatus(clientTriggerId) }
            .recoverCatching { throw it.translateOrPassthrough("pinch-alert-status") }

    suspend fun pinchUpdateLocation(
        rawJson: String,
        signatureB64: String,
        bodySha256Hex: String
    ): Result<LocationUpdateResponse> = runCatching {
        val body = rawJson.toRequestBody(JSON_MEDIA)
        project2Api.pinchUpdateLocation(
            signature = signatureB64,
            signatureAlg = "ed25519",
            bodySha256 = bodySha256Hex,
            body = body
        )
    }.recoverCatching { throw it.translateOrPassthrough("pinch-update-location") }

    /**
     * Translates a raw transport/HTTP failure into a richer typed exception
     * when the backend's 4xx body carries a marker we know how to react to;
     * otherwise returns the original throwable for upstream classifiers.
     *
     * Currently recognized markers:
     *  • HTTP 403 with `error` text containing "attestation" → [AttestationExpiredException].
     *    Triggers automatic re-attest + retry inside [SosRepositoryImpl].
     *
     * All other 4xx bodies are logged once (signature_mismatch,
     * device_revoked, app_version_blocked, etc.) so they're not opaque in
     * logcat. Body read is bounded to [ERROR_BODY_MAX_BYTES] so a misbehaving
     * server can't OOM us.
     */
    private fun Throwable.translateOrPassthrough(endpoint: String): Throwable {
        val http = this as? HttpException ?: return this

        val errorBody = if (http.code() in 400..499) {
            runCatching {
                http.response()?.errorBody()?.source()?.let { src ->
                    src.request(ERROR_BODY_MAX_BYTES)
                    src.buffer.snapshot(
                        minOf(ERROR_BODY_MAX_BYTES.toInt(), src.buffer.size.toInt())
                    ).utf8()
                }
            }.getOrNull()
        } else null

        if (errorBody != null) {
            Timber.tag(TAG).w(
                "%s HTTP %d — body=%s",
                endpoint, http.code(), errorBody
            )
        }

        if (http.code() == 403 && errorBody != null && isAttestationExpired(errorBody)) {
            Timber.tag(TAG).w(
                "%s — attestation expired marker detected; signaling auto-reattest",
                endpoint
            )
            return AttestationExpiredException(
                backendMessage = errorBody.take(200),
                cause = http
            )
        }

        return this
    }

    private fun isAttestationExpired(body: String): Boolean {
        // Match the literal backend wording AND a slightly more tolerant form
        // ("attestation_expired", "attestation has expired", "expired
        // attestation"). The Project 2 server today emits:
        //   {"error":"Device attestation is expired"}
        // — keep this check tolerant so a phrase tweak doesn't break recovery.
        val lc = body.lowercase()
        if ("attestation" !in lc) return false
        return "expir" in lc
    }

    private companion object {
        const val TAG = "SosRemote"
        const val ERROR_BODY_MAX_BYTES = 4_096L
        val JSON_MEDIA = "application/json".toMediaType()
    }
}
