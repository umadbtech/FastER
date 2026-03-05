package com.faster.festival.data.repository

import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Repository for App Home Bundle API
 * Handles server-driven home screen configuration with ETag-based caching
 */
class AppHomeRepository(
    private val appHomeApi: AppHomeApi
) {
    // In-memory cache
    private var cachedResponse: AppHomeBundleResponse? = null
    private var cachedETag: String? = null
    private var cachedFestivalSlug: String? = null

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
    }
}
