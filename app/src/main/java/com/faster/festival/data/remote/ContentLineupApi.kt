package com.faster.festival.data.remote

import com.faster.festival.data.models.ContentLineupResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Lineup endpoint
 * GET /functions/v1/content-lineup?festival_slug=<slug>
 *
 * Returns: ContentLineupResponse with artists list
 */
interface ContentLineupApi {

    /**
     * Fetch lineup/artists content
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with lineup data
     */
    @GET("functions/v1/content-lineup")
    suspend fun getContentLineup(
        @Query("festival_slug") festivalSlug: String
    ): Response<ContentLineupResponse>
}
