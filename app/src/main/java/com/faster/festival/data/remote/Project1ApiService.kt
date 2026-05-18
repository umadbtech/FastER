package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Project 1 backend surface — wristband pairing CRUD, heartbeat / telemetry,
 * and SOS history. Hosted at `BuildConfig.VITE_SUPABASE_URL`
 * (https://mihgyfijfnbhypiraoxg.supabase.co) and consumed via the EXISTING
 * Project 1 Retrofit client built in
 * [com.faster.festival.di.NetworkModule] — no new OkHttp / interceptor chain.
 *
 * Auth: shared user JWT (Project 1) is injected by [AuthorizationInterceptor];
 * `apikey` header is the Project 1 anon key (`VITE_SUPABASE_ANON_KEY`).
 *
 * Mutating endpoints accept an optional `Idempotency-Key` header so callers
 * can safely retry without producing duplicate rows.
 *
 * 5.2 `getActiveWristband` returns HTTP 204 (no body) when the user has no
 * active pairing — wrapped in [Response] so callers don't trip on a null body
 * exception.
 */
interface Project1ApiService {

    // ─── Wristband pairing CRUD ────────────────────────────────────────────

    @POST("functions/v1/wristband-pair")
    @Headers("Content-Type: application/json")
    suspend fun pairWristband(
        @Body body: WristbandPairRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): WristbandPairing

    @GET("functions/v1/wristband-active")
    suspend fun getActiveWristband(): Response<WristbandPairing>

    @HTTP(
        method = "DELETE",
        path = "functions/v1/wristband-unpair",
        hasBody = true
    )
    @Headers("Content-Type: application/json")
    suspend fun unpairWristband(
        @Query("wristband_id") wristbandId: String,
        @Body body: WristbandUnpairRequest = WristbandUnpairRequest(),
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): WristbandUnpairResponse

    // ─── Telemetry / heartbeat ─────────────────────────────────────────────

    @POST("functions/v1/wristband-heartbeat")
    @Headers("Content-Type: application/json")
    suspend fun sendHeartbeat(
        @Query("wristband_id") wristbandId: String,
        @Body body: WristbandHeartbeatRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<Unit>

    @POST("functions/v1/wristband-telemetry-batch")
    @Headers("Content-Type: application/json")
    suspend fun uploadTelemetryBatch(
        @Body body: WristbandTelemetryBatchRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): WristbandTelemetryBatchResponse

    // ─── SOS event audit (optional Project 1 mirror; primary path is signed
    //     Project 2 pinch-ingest — see Wristband-Backend-API.md §5.5) ───────
    //
    // `(wristband_id, event_id)` is unique server-side: replaying the same
    // event_id is idempotent and may return HTTP 200 with the existing row.
    @POST("functions/v1/wristband-sos-record")
    @Headers("Content-Type: application/json")
    suspend fun recordSosEvent(
        @Query("wristband_id") wristbandId: String,
        @Body body: WristbandSosRecordRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): WristbandSosRecordResponse

    // ─── SOS history (optional — drives Routes.SOS_HISTORY) ────────────────

    @GET("functions/v1/wristband-sos-history")
    suspend fun listSosHistory(
        @Query("wristband_id") wristbandId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") beforeIsoTimestamp: String? = null
    ): SosHistoryResponse
}
