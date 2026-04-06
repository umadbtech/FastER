package com.faster.festival.data.repository

import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.ContentArtistDetailResponse
import com.faster.festival.data.models.ContentLineupResponse
import com.faster.festival.data.models.ContentStageScheduleResponse
import com.faster.festival.data.models.OfflineBundleResponse
import com.faster.festival.data.remote.AppExperienceBundleApi
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.data.remote.ContentArtistDetailApi
import com.faster.festival.data.remote.ContentHomeApi
import com.faster.festival.data.remote.ContentLineupApi
import com.faster.festival.data.remote.ContentMapApi
import com.faster.festival.data.remote.ContentMapResponse
import com.faster.festival.data.remote.ContentStageScheduleApi
import com.faster.festival.data.remote.FestivalExperienceApi
import com.faster.festival.data.remote.FestivalHeaderApi
import com.faster.festival.data.remote.OfflineBundleApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Repository for all Content and Experience API endpoints
 * Provides centralized access with Flow-based reactive operations
 */
class ContentRepository(
    private val festivalHeaderApi: FestivalHeaderApi,
    private val contentHomeApi: ContentHomeApi,
    private val contentLineupApi: ContentLineupApi,
    private val contentArtistDetailApi: ContentArtistDetailApi,
    private val contentStageScheduleApi: ContentStageScheduleApi,
    private val contentMapApi: ContentMapApi,
    private val appHomeApi: AppHomeApi,
    private val festivalExperienceApi: FestivalExperienceApi,
    private val appExperienceBundleApi: AppExperienceBundleApi,
    private val offlineBundleApi: OfflineBundleApi
) {

    // ========== Festival Header ==========
    fun getFestivalHeader(festivalSlug: String): Flow<FestivalHeaderApi.FestivalHeader> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = festivalHeaderApi.getFestivalHeader(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body.festival)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Content Home ==========
    fun getContentHome(festivalSlug: String): Flow<ContentHomeApi.ContentHomeResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = contentHomeApi.getContentHome(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Content Lineup ==========
    fun getContentLineup(festivalSlug: String): Flow<ContentLineupResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = contentLineupApi.getContentLineup(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Content Artist Detail ==========
    fun getArtistDetail(
        festivalSlug: String,
        artistSlug: String
    ): Flow<ContentArtistDetailResponse> = flow {
        if (festivalSlug.isBlank() || artistSlug.isBlank()) throw IOException("Missing required parameters")
        val response = contentArtistDetailApi.getArtistDetail(festivalSlug, artistSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Artist not found (404)")
            response.code() == 400 -> throw IOException("Missing parameters (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Content Stage Schedule ==========
    fun getStageSchedule(festivalSlug: String): Flow<ContentStageScheduleResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = contentStageScheduleApi.getStageSchedule(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Content Map ==========
    fun getContentMap(festivalSlug: String): Flow<ContentMapResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = contentMapApi.getContentMap(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== App Home Bundle ==========
    private var cachedAppHomeBundle: AppHomeBundleResponse? = null
    private var cachedAppHomeBundleETag: String? = null
    private var cachedAppHomeBundleSlug: String? = null

    fun getAppHomeBundle(festivalSlug: String): Flow<AppHomeBundleResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")

        if (cachedAppHomeBundleSlug != festivalSlug) {
            cachedAppHomeBundle = null
            cachedAppHomeBundleETag = null
        }
        cachedAppHomeBundleSlug = festivalSlug

        val response = appHomeApi.getAppHomeBundle(
            festivalSlug = festivalSlug,
            ifNoneMatch = cachedAppHomeBundleETag
        )

        when {
            response.code() == 304 && cachedAppHomeBundle != null -> {
                emit(cachedAppHomeBundle!!)
            }
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) {
                    val eTag = response.headers()["ETag"]
                    cachedAppHomeBundle = body
                    cachedAppHomeBundleETag = eTag
                    emit(body)
                } else {
                    throw IOException("Empty response body")
                }
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    fun clearAppHomeBundleCache() {
        cachedAppHomeBundle = null
        cachedAppHomeBundleETag = null
        cachedAppHomeBundleSlug = null
    }

    // ========== Experience Categories ==========
    fun getExperienceCategories(
        festivalSlug: String
    ): Flow<FestivalExperienceApi.ExperienceCategoriesResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = festivalExperienceApi.getExperienceCategories(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Experience Locations by Category ==========
    fun getExperienceLocationsByCategory(
        festivalSlug: String,
        category: String
    ): Flow<FestivalExperienceApi.ExperienceLocationsResponse> = flow {
        if (festivalSlug.isBlank() || category.isBlank()) throw IOException("Missing required parameters")
        val response = festivalExperienceApi.getExperienceLocationsByCategory(festivalSlug, category)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Not found (404)")
            response.code() == 400 -> throw IOException("Missing parameters (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Experience Location Detail ==========
    fun getExperienceLocationDetail(
        id: String
    ): Flow<FestivalExperienceApi.ExperienceLocationDetailResponse> = flow {
        if (id.isBlank()) throw IOException("Missing location id")
        val response = festivalExperienceApi.getExperienceLocation(id)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Location not found (404)")
            response.code() == 400 -> throw IOException("Missing location id (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== App Experience Bundle ==========
    fun getAppExperienceBundle(
        festivalSlug: String
    ): Flow<AppExperienceBundleApi.AppExperienceBundleResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")
        val response = appExperienceBundleApi.getExperienceBundle(festivalSlug)
        when {
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) emit(body)
                else throw IOException("Empty response body")
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    // ========== Offline Bundle ==========
    private var cachedOfflineBundle: OfflineBundleResponse? = null
    private var cachedOfflineBundleETag: String? = null
    private var cachedOfflineBundleSlug: String? = null

    fun getOfflineBundle(festivalSlug: String): Flow<OfflineBundleResponse> = flow {
        if (festivalSlug.isBlank()) throw IOException("Missing festival slug")

        if (cachedOfflineBundleSlug != festivalSlug) {
            cachedOfflineBundle = null
            cachedOfflineBundleETag = null
        }
        cachedOfflineBundleSlug = festivalSlug

        val response = offlineBundleApi.getOfflineBundle(
            festivalSlug = festivalSlug,
            ifNoneMatch = cachedOfflineBundleETag
        )

        when {
            response.code() == 304 && cachedOfflineBundle != null -> {
                emit(cachedOfflineBundle!!)
            }
            response.isSuccessful && response.code() == 200 -> {
                val body = response.body()
                if (body != null) {
                    val eTag = response.headers()["ETag"]
                    cachedOfflineBundle = body
                    cachedOfflineBundleETag = eTag
                    emit(body)
                } else {
                    throw IOException("Empty response body")
                }
            }
            response.code() == 404 -> throw IOException("Festival not found (404)")
            response.code() == 400 -> throw IOException("Missing festival slug (400)")
            response.code() == 500 -> throw IOException("Server error (500)")
            else -> throw IOException("API error: ${response.code()}")
        }
    }

    fun clearOfflineBundleCache() {
        cachedOfflineBundle = null
        cachedOfflineBundleETag = null
        cachedOfflineBundleSlug = null
    }

    fun clearAllCaches() {
        clearAppHomeBundleCache()
        clearOfflineBundleCache()
    }
}
