package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Stage Schedule endpoint
 * GET /functions/v1/content-stage-schedule?festival_slug=<slug>
 *
 * Returns: ContentStageScheduleResponse with stages and their performance schedule
 */
interface ContentStageScheduleApi {

    /**
     * Fetch stage schedule content
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with stage schedule data
     */
    @GET("functions/v1/content-stage-schedule")
    suspend fun getStageSchedule(
        @Query("festival_slug") festivalSlug: String
    ): Response<ContentStageScheduleResponse>

    data class ContentStageScheduleResponse(
        val schema_version: String,
        val generated_at: String,
        val stages: List<StageWithSchedule>,
        val days_count: Int
    )

    data class StageWithSchedule(
        val id: String,
        val name: String,
        val location: String?,
        val image_url: String?,
        val capacity: Int?,
        val description: String?,
        val schedule: List<ScheduleSlot>
    )

    data class ScheduleSlot(
        val id: String,
        val artist_id: String,
        val artist_name: String,
        val artist_image_url: String?,
        val start_time: String,
        val end_time: String,
        val day: Int,
        val genres: List<String>?
    )
}
