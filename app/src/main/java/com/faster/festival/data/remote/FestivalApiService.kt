package com.faster.festival.data.remote

import com.faster.festival.data.models.FestivalHeaderResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Festival API Service for Supabase Edge Function calls
 */
interface FestivalApiService {

    /**
     * Get festival header information
     *
     * @param festivalSlug Festival slug (e.g., "floydfest-26")
     * @param authorization Optional Bearer token if user is logged in
     * @return FestivalHeaderResponse containing festival details
     */
    @GET("functions/v1/festival-header")
    suspend fun getFestivalHeader(
        @Query("festival_slug") festivalSlug: String,
        @Header("Authorization") authorization: String? = null
    ): Response<FestivalHeaderResponse>
}
