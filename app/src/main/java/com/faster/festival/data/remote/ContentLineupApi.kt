package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Lineup endpoint
 * GET /functions/v1/content-lineup?festival_slug=<slug>
 *
 * Returns: ContentLineupResponse with featured artists, lineup schedule
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

    data class ContentLineupResponse(
        val schema_version: String,
        val generated_at: String,
        val featured_artists: List<FeaturedArtist>,
        val lineup_schedule: List<LineupScheduleItem>,
        val stages: List<Stage>
    )

    data class FeaturedArtist(
        val id: String,
        val name: String,
        val bio: String?,
        val image_url: String?,
        val genres: List<String>?,
        val social_links: Map<String, String>?,
        val order: Int
    )

    data class LineupScheduleItem(
        val id: String,
        val artist_id: String,
        val artist_name: String,
        val stage_id: String,
        val stage_name: String,
        val start_time: String,
        val end_time: String,
        val day: Int
    )

    data class Stage(
        val id: String,
        val name: String,
        val location: String?,
        val image_url: String?
    )
}
