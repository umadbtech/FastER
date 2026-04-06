package com.faster.festival.data.remote

import com.faster.festival.data.models.ContentStageScheduleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Stage Schedule endpoint
 * GET /functions/v1/content-stage-schedule?festival_slug=<slug>
 *
 * Returns: ContentStageScheduleResponse with stages and schedule events
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
}
