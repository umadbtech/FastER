package com.faster.festival.data.remote

import com.faster.festival.data.models.ProfileSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit API service for Profile endpoints
 */
interface ProfileApiService {

    /**
     * GET /functions/v1/profile-summary
     * Load user profile summary with all related data
     */
    @GET("functions/v1/profile-summary")
    suspend fun getProfileSummary(
        @Header("Authorization") authorization: String
    ): ProfileSummaryResponse
}
