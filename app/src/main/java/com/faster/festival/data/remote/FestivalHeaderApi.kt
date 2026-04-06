package com.faster.festival.data.remote

import com.faster.festival.data.models.AppHomeBundleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit API service for Festival Header endpoint
 * GET /functions/v1/festival-header?festival_slug=<slug>
 *
 * Returns: FestivalHeaderResponse with festival object
 */
interface FestivalHeaderApi {

    /**
     * Fetch festival header information
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with festival header data (name, dates, banner, logo, colors, etc.)
     */
    @GET("functions/v1/festival-header")
    suspend fun getFestivalHeader(
        @Query("festival_slug") festivalSlug: String
    ): Response<FestivalHeaderResponse>

    data class FestivalHeaderResponse(
        val schema_version: String,
        val festival: FestivalHeader
    )

    data class FestivalHeader(
        val id: String,
        val slug: String,
        val name: String,
        val timezone: String,
        val starts_at: String,
        val ends_at: String,
        val logo_url: String?,
        val banner_url: String?,
        val accent_color_hex: String?,
        val context_state: String,
        val status: String
    )
}
