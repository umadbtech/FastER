package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Project 2 backend surface — hosted at `BuildConfig.PROJECT2_SOS_URL`
 * (https://tlwaffkoleqljanpopvn.supabase.co). Project 2 holds the dispatch
 * console; mobile clients talk to it for two purposes:
 *
 *  1. **Signed dispatch (pinch-ingest)** — owned by the existing
 *     [com.faster.festival.data.sos.remote.SosApiService]. The Ed25519
 *     signing envelope means [Project2ApiService] does NOT re-declare those
 *     endpoints. [com.faster.festival.data.repository.PinchIngestRepository]
 *     is the thin caller-facing facade.
 *  2. **Unsigned read-only auxiliary endpoints** — declared here. Today only
 *     a health probe; the seam is in place for future Project 2 reads (e.g.
 *     dispatch-side config) without spinning up another Retrofit interface.
 *
 * Built from the SAME Retrofit client as [SosApiService] via
 * [com.faster.festival.data.sos.remote.SosNetworkClient.project2Retrofit] —
 * one OkHttp / one interceptor chain for the entire Project 2 surface. The
 * shared chain installs apikey (`PROJECT2_SOS_ANON_KEY`), the shared Project
 * 1 user JWT, and [com.faster.festival.data.remote.TokenRefreshInterceptor]
 * for 401 recovery — matching the existing implementation.
 */
interface Project2ApiService {

    /**
     * Lightweight health probe. Returns HTTP 200 on success; mobile code uses
     * it to verify Project 2 reachability before driving a manual SOS so the
     * user gets a clean "backend unreachable" rather than an SOS failure mid-
     * flight.
     */
    @GET("functions/v1/pinch-health")
    suspend fun pinchHealth(
        @Query("client_trigger_id") clientTriggerId: String? = null,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): Response<Unit>
}
