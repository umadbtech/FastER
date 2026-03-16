package com.faster.festival.data.remote

import com.faster.festival.data.models.ContentArtistDetailResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Artist Detail endpoint
 * GET /functions/v1/content-artist-detail?festival_slug=<slug>&artist_slug=<slug>
 *
 * Returns: ContentArtistDetailResponse with artist bio, events, media
 */
interface ContentArtistDetailApi {

    /**
     * Fetch detailed artist information
     *
     * @param festivalSlug Festival identifier (required)
     * @param artistSlug Artist identifier (required)
     * @return Response with artist detail data
     */
    @GET("functions/v1/content-artist-detail")
    suspend fun getArtistDetail(
        @Query("festival_slug") festivalSlug: String,
        @Query("artist_slug") artistSlug: String
    ): Response<ContentArtistDetailResponse>
}
