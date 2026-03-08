package com.faster.festival.data.repository

import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.models.Result
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.data.remote.FestivalApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Repository for App Home Bundle API and Festival Header
 * ✅ CONSOLIDATED: Merges functionality from FestivalHeaderRepository
 * Handles server-driven home screen configuration with ETag-based caching
 */
class AppHomeRepository(
    private val appHomeApi: AppHomeApi,
    private val festivalApiService: FestivalApiService  // ✅ Required for festival header
) {
    // In-memory cache for app home bundle
    private var cachedResponse: AppHomeBundleResponse? = null
    private var cachedETag: String? = null
    private var cachedFestivalSlug: String? = null

    // In-memory cache for festival header (consolidated from FestivalHeaderRepository)
    private var cachedFestivalHeader: FestivalHeader? = null

    /**
     * Fetch app home bundle for a festival
     * Implements ETag-based caching
     *
     * @param festivalSlug Festival identifier
     * @return Flow<AppHomeBundleResponse>
     */
    fun getAppHomeBundle(festivalSlug: String): Flow<AppHomeBundleResponse> = flow {
        try {
            // If slug changed, clear cache
            if (cachedFestivalSlug != festivalSlug) {
                cachedResponse = null
                cachedETag = null
            }
            cachedFestivalSlug = festivalSlug

            // Make API call with ETag header if available
            val response = appHomeApi.getAppHomeBundle(
                festivalSlug = festivalSlug,
                ifNoneMatch = cachedETag
            )

            when {
                response.code() == 304 && cachedResponse != null -> {
                    // 304 Not Modified - return cached response
                    emit(cachedResponse!!)
                }
                response.isSuccessful && response.code() == 200 -> {
                    // 200 OK - update cache and return
                    val body = response.body()
                    if (body != null) {
                        // Extract ETag from response headers
                        val eTag = response.headers()["ETag"]
                        cachedResponse = body
                        cachedETag = eTag
                        emit(body)
                    } else {
                        throw IOException("Empty response body from app-home-bundle")
                    }
                }
                response.code() == 404 -> {
                    throw IOException("Festival not found (404)")
                }
                response.code() == 400 -> {
                    throw IOException("Bad request (400)")
                }
                response.code() == 500 -> {
                    throw IOException("Server error (500)")
                }
                else -> {
                    throw IOException("API error: ${response.code()} - ${response.message()}")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Clear cache (useful for logout or manual refresh)
     */
    fun clearCache() {
        cachedResponse = null
        cachedETag = null
        cachedFestivalSlug = null
        cachedFestivalHeader = null  // ✅ Also clear festival header cache
    }

    // ============================================================================
    // ✅ CONSOLIDATED: Festival Header methods (merged from FestivalHeaderRepository)
    // ============================================================================

    /**
     * Fetch festival header by slug
     * Includes basic error handling and response code parsing
     * API: GET /functions/v1/festival-header?festival_slug=<slug>
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

            val response = festivalApiService.getFestivalHeader(slug, authHeader)

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
}
