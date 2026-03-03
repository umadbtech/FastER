package com.faster.festival.data.repository

import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.models.Result
import com.faster.festival.data.remote.FestivalApiService

/**
 * Repository for festival data operations
 * Handles API calls, error parsing, and caching
 */
class FestivalHeaderRepository(
    private val apiService: FestivalApiService
) {

    // Simple in-memory cache for last successful fetch
    private var cachedFestivalHeader: FestivalHeader? = null

    /**
     * Fetch festival header by slug
     * Includes basic error handling and response code parsing
     *
     * @param slug Festival slug (e.g., "floydfest-26")
     * @param accessToken Optional access token for authenticated requests
     * @return Result<FestivalHeader> with success or error state
     */
    suspend fun getFestivalHeader(
        slug: String,
        accessToken: String? = null
    ): Result<FestivalHeader> {
        return try {
            if (slug.isBlank()) {
                return Result.Error("Missing festival slug", 400)
            }

            val authHeader = if (!accessToken.isNullOrBlank()) {
                "Bearer $accessToken"
            } else {
                null
            }

            val response = apiService.getFestivalHeader(slug, authHeader)

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) {
                        cachedFestivalHeader = body.festival
                        Result.Success(body.festival)
                    } else {
                        Result.Error("Empty response body", response.code())
                    }
                }
                response.code() == 400 -> {
                    Result.Error("Missing festival slug", 400)
                }
                response.code() == 404 -> {
                    Result.Error("Festival not found", 404)
                }
                response.code() == 500 -> {
                    Result.Error("Server error", 500)
                }
                else -> {
                    val errorMessage = response.errorBody()?.string()
                        ?: "Unknown error (${response.code()})"
                    Result.Error(errorMessage, response.code())
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get cached festival header (used for quick access on rotation/recomposition)
     *
     * @return Cached FestivalHeader or null if not yet loaded
     */
    fun getCachedFestivalHeader(): FestivalHeader? = cachedFestivalHeader

    /**
     * Clear the in-memory cache
     */
    fun clearCache() {
        cachedFestivalHeader = null
    }
}
