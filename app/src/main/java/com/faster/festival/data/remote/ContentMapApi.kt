package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Map endpoint
 * GET /functions/v1/content-map?festival_slug=<slug>
 *
 * Returns: ContentMapResponse with festival info and map points
 */
interface ContentMapApi {

    @GET("functions/v1/content-map")
    suspend fun getContentMap(
        @Query("festival_slug") festivalSlug: String
    ): Response<ContentMapResponse>
}

@Serializable
data class ContentMapResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentMapFestival,
    @SerialName("map_points")
    val mapPoints: List<MapPoint> = emptyList()
)

@Serializable
data class ContentMapFestival(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("timezone")
    val timezone: String
)

@Serializable
data class MapPoint(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("kind")
    val kind: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("lat")
    val lat: Double? = null,
    @SerialName("lng")
    val lng: Double? = null,
    @SerialName("map_point_key")
    val mapPointKey: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("next_event")
    val nextEvent: MapPointEvent? = null
)

@Serializable
data class MapPointEvent(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("starts_at")
    val startsAt: String,
    @SerialName("ends_at")
    val endsAt: String,
    @SerialName("status")
    val status: String
)
