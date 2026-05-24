package com.faster.festival.data.sos.remote

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
// LocationUpdateResponse lives in SosDtos.kt — same package, no import needed.

/**
 * Project 2 SOS Retrofit interface — base URL is `BuildConfig.PROJECT2_SOS_URL`.
 *
 * Two surfaces live here:
 *  • Project 1 endpoints (`sos-register-device`, `sos-verify-attestation`) which
 *    happen to share the same Bearer JWT with everything else.
 *  • Project 2 endpoints (`pinch-ingest`, `pinch-alert-status`) which require
 *    the per-request signature headers `x-device-signature*`.
 *
 * `pinch-ingest` takes a [RequestBody] (NOT a serialized DTO) so the caller
 * controls the EXACT JSON byte string — that exact string is what was hashed
 * and signed. Re-serializing inside Retrofit would invalidate the signature.
 */
interface SosApiService {

    @POST("functions/v1/sos-register-device")
    @Headers("Content-Type: application/json")
    suspend fun registerDevice(
        @Body body: RegisterDeviceRequest
    ): RegisterDeviceResponse

    @POST("functions/v1/sos-verify-attestation")
    @Headers("Content-Type: application/json")
    suspend fun verifyAttestation(
        @Body body: VerifyAttestationRequest
    ): VerifyAttestationResponse

    /**
     * Sends the EXACT pre-serialized JSON [body]. The hash header MUST be
     * SHA-256 of this exact byte string and the signature MUST cover the
     * canonical string built around that hash.
     */
    @POST("functions/v1/pinch-ingest")
    suspend fun pinchIngest(
        @Header("x-device-signature") signature: String,
        @Header("x-device-signature-alg") signatureAlg: String,
        @Header("x-device-body-sha256") bodySha256: String,
        @Body body: RequestBody
    ): SosIngestResponse

    /**
     * Polls the dispatch status for an in-flight SOS.
     *
     * Preferred lookup is by `id` (the alert id). `client_trigger_id` is the
     * fallback used before the alert id is known (e.g. a re-dispatch still
     * pending). `view=help_on_the_way` returns the live-tracking payload the new
     * Pinch SOS screen renders (`ui_status`, `responder`, `eta`, `cancellation`).
     * Both query keys are nullable so callers pass exactly one.
     */
    @GET("functions/v1/pinch-alert-status")
    suspend fun pinchAlertStatus(
        @Query("id") alertId: String? = null,
        @Query("client_trigger_id") clientTriggerId: String? = null,
        @Query("view") view: String = "help_on_the_way"
    ): SosStatusResponse

    /**
     * Signed partial-details submit (phone / medical info / what-happened /
     * manual location). Same Ed25519 envelope as [pinchIngest]: [body] is the
     * EXACT pre-serialized JSON byte string that was hashed and signed.
     */
    @POST("functions/v1/pinch-alert-details")
    suspend fun pinchAlertDetails(
        @Header("x-device-signature") signature: String,
        @Header("x-device-signature-alg") signatureAlg: String,
        @Header("x-device-body-sha256") bodySha256: String,
        @Body body: RequestBody
    ): AlertDetailsResponse

    /**
     * UNSIGNED cancel — `apikey` + Bearer only (no `x-device-signature*`).
     * After this the client keeps polling [pinchAlertStatus] and renders the
     * exact `ui_status` (CANCEL_REQUESTED → CANCELLED / CANCEL_DENIED).
     */
    @POST("functions/v1/pinch-cancel")
    @Headers("Content-Type: application/json")
    suspend fun pinchCancel(
        @Body body: CancelRequest
    ): CancelResponse

    /**
     * Periodic GPS push — same signing envelope as [pinchIngest]. Body is the
     * pre-serialized JSON byte string so the SHA-256 we hash matches the
     * SHA-256 the backend computes.
     */
    @POST("functions/v1/pinch-update-location")
    suspend fun pinchUpdateLocation(
        @Header("x-device-signature") signature: String,
        @Header("x-device-signature-alg") signatureAlg: String,
        @Header("x-device-body-sha256") bodySha256: String,
        @Body body: RequestBody
    ): LocationUpdateResponse

    /**
     * UNSIGNED list of the user's past SOS alerts — `apikey` + Bearer only
     * (auth scopes the rows to this user). Cursor-paginated: pass the prior
     * response's `next_cursor` back as [cursor]. [festivalId] optionally
     * filters to a single festival.
     */
    @GET("functions/v1/pinch-alert-history")
    suspend fun pinchAlertHistory(
        @Query("limit") limit: Int = 20,
        @Query("festival_id") festivalId: String? = null,
        @Query("cursor") cursor: String? = null
    ): PinchAlertHistoryResponse
}
