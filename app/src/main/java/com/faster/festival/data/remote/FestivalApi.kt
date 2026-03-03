package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Festival API calls to Supabase Edge Function
 */
interface FestivalApi {

    /**
     * Fetch festival header information from Supabase Edge Function
     *
     * @param festivalSlug Festival slug (e.g., "floydfest-26")
     * @return Response containing FestivalHeaderResponse with festival data
     */
    @GET("functions/v1/festival-header")
    suspend fun getFestivalHeader(
        @Query("festival_slug") festivalSlug: String
    ): Response<FestivalHeaderResponse>
}

/**
 * API response wrapper from Supabase Edge Function
 */
data class FestivalHeaderResponse(
    val schema_version: String,
    val festival: FestivalHeaderData
)

/**
 * Festival header data from API
 * Note: Using snake_case to match API response, @SerializedName not needed with Kotlinx Serialization
 */
data class FestivalHeaderData(
    val id: String,
    val slug: String,
    val name: String,
    val timezone: String,
    val starts_at: String,
    val ends_at: String,
    val logo_url: String,
    val banner_url: String,
    val accent_color_hex: String? = null,  // Nullable - defaults to Navy Blue if null
    val context_state: String
)
