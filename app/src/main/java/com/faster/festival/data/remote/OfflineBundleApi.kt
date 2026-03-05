package com.faster.festival.data.remote

import com.faster.festival.data.models.OfflineBundleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit API service for Offline Bundle endpoint
 * GET /functions/v1/offline-bundle?festival_slug=<slug>
 *
 * Returns: OfflineBundleResponse with all content needed for offline functionality
 */
interface OfflineBundleApi {

    /**
     * Fetch offline bundle with all essential content
     *
     * Used for offline-first functionality. Contains full copies of content
     * that users can access without internet connectivity.
     *
     * Supports ETag-based caching:
     * - Send If-None-Match header with cached ETag to check for updates
     * - Server returns 304 Not Modified if content hasn't changed
     * - Returns 200 OK with new body and ETag header if content updated
     *
     * @param festivalSlug Festival identifier (required)
     * @param ifNoneMatch ETag value for cache revalidation (optional)
     * @return Response with offline bundle data (compressed/optimized) or 304 if cached
     */
    @GET("functions/v1/offline-bundle")
    suspend fun getOfflineBundle(
        @Query("festival_slug") festivalSlug: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<OfflineBundleResponse>
}
