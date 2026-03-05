package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Artist Detail endpoint
 * GET /functions/v1/content-artist-detail?festival_slug=<slug>&artist_slug=<slug>
 *
 * Returns: ContentArtistDetailResponse with artist bio, performances, media
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

    data class ContentArtistDetailResponse(
        val schema_version: String,
        val artist: ArtistDetail,
        val performances: List<Performance>,
        val media: ArtistMedia?
    )

    data class ArtistDetail(
        val id: String,
        val slug: String,
        val name: String,
        val bio: String?,
        val image_url: String?,
        val cover_image_url: String?,
        val genres: List<String>?,
        val origin: String?,
        val founded_year: Int?,
        val member_count: Int?
    )

    data class Performance(
        val id: String,
        val stage_id: String,
        val stage_name: String,
        val start_time: String,
        val end_time: String,
        val day: Int,
        val description: String?
    )

    data class ArtistMedia(
        val website: String?,
        val spotify: String?,
        val instagram: String?,
        val twitter: String?,
        val youtube: String?,
        val videos: List<Video>?
    )

    data class Video(
        val id: String,
        val title: String,
        val url: String,
        val thumbnail_url: String?,
        val duration_seconds: Int?
    )
}
