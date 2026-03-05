package com.faster.festival.data.remote

import com.faster.festival.data.models.AppHomeBundleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit API service for App Home Bundle endpoint
 * GET /functions/v1/app-home-bundle?festival_slug=<slug>
 */
interface AppHomeApi {

    /**
     * Fetch app home bundle with server-driven configuration
     *
     * @param festivalSlug Festival identifier
     * @param ifNoneMatch ETag for cache revalidation (optional)
     * @return Response with AppHomeBundleResponse body and ETag header
     */
    @GET("functions/v1/app-home-bundle")
    suspend fun getAppHomeBundle(
        @Query("festival_slug") festivalSlug: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<AppHomeBundleResponse>
}
