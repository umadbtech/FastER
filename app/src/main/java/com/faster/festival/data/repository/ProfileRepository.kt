package com.faster.festival.data.repository

import android.util.Log
import com.faster.festival.data.models.ProfileSummary
import com.faster.festival.data.models.ProfileSummaryResponse
import com.faster.festival.data.models.toProfileSummary
import com.faster.festival.data.remote.ProfileApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for Profile-related API calls
 */
class ProfileRepository(private val profileApiService: ProfileApiService) {

    /**
     * Load profile summary from API
     */
    fun loadProfileSummary(accessToken: String): Flow<Result<ProfileSummary>> = flow {
        try {
            val response = profileApiService.getProfileSummary(
                authorization = "Bearer $accessToken"
            )

            if (response.ok) {
                val profileSummary = response.toProfileSummary()
                emit(Result.success(profileSummary))
            } else {
                emit(Result.failure(Exception("Profile response not ok")))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error loading profile summary: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Check if user has completed terms
     */
    fun hasCompletedTerms(profileSummary: ProfileSummary): Boolean {
        return profileSummary.termsComplete
    }

    /**
     * Derive full name from profile
     */
    fun getFullName(profileSummary: ProfileSummary): String {
        return when {
            profileSummary.legalFirstName != null && profileSummary.legalLastName != null ->
                "${profileSummary.legalFirstName} ${profileSummary.legalLastName}"
            profileSummary.legalFirstName != null ->
                profileSummary.legalFirstName
            profileSummary.username != null ->
                profileSummary.username
            else -> "User"
        }
    }
}
