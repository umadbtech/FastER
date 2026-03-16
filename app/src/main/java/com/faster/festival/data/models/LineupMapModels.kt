package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content Lineup API Response
 * GET /functions/v1/content-lineup?festival_slug=<slug>
 */
@Serializable
data class ContentLineupResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentFestivalInfo,
    @SerialName("artists")
    val artists: List<ContentArtistLineup> = emptyList()
)

@Serializable
data class ContentArtistLineup(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("genres")
    val genres: List<String>? = null,
    @SerialName("social_links")
    val socialLinks: Map<String, String>? = null,
    @SerialName("order")
    val order: Int = 0
)

/**
 * Content Artist Detail API Response
 * GET /functions/v1/content-artist-detail?festival_slug=<slug>&artist_slug=<slug>
 */
@Serializable
data class ContentArtistDetailResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentFestivalInfo,
    @SerialName("artist")
    val artist: ContentArtistDetail,
    @SerialName("events")
    val events: List<ContentArtistEvent> = emptyList(),
    @SerialName("media")
    val media: ContentArtistMedia? = null
)

@Serializable
data class ContentArtistMedia(
    @SerialName("website")
    val website: String? = null,
    @SerialName("spotify")
    val spotify: String? = null,
    @SerialName("instagram")
    val instagram: String? = null,
    @SerialName("twitter")
    val twitter: String? = null,
    @SerialName("youtube")
    val youtube: String? = null
)

@Serializable
data class ContentArtistDetail(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,
    @SerialName("genres")
    val genres: List<String>? = null,
    @SerialName("origin")
    val origin: String? = null,
    @SerialName("founded_year")
    val foundedYear: Int? = null,
    @SerialName("member_count")
    val memberCount: Int? = null
)

@Serializable
data class ContentArtistEvent(
    @SerialName("id")
    val id: String,
    @SerialName("stage_id")
    val stageId: String,
    @SerialName("stage_name")
    val stageName: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("day")
    val day: Int,
    @SerialName("description")
    val description: String? = null
)

/**
 * Content Stage Schedule API Response
 * GET /functions/v1/content-stage-schedule?festival_slug=<slug>
 */
@Serializable
data class ContentStageScheduleResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentFestivalInfo,
    @SerialName("stages")
    val stages: List<ContentStageSchedule> = emptyList(),
    @SerialName("events")
    val events: List<ContentScheduleEvent> = emptyList()
)

@Serializable
data class ContentStageSchedule(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("location")
    val location: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class ContentScheduleEvent(
    @SerialName("id")
    val id: String,
    @SerialName("artist_id")
    val artistId: String,
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("artist_image_url")
    val artistImageUrl: String? = null,
    @SerialName("stage_id")
    val stageId: String,
    @SerialName("stage_name")
    val stageName: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("day")
    val day: Int,
    @SerialName("genres")
    val genres: List<String>? = null
)

/**
 * Content Map API Response
 * GET /functions/v1/content-map?festival_slug=<slug>
 */
@Serializable
data class ContentMapResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentFestivalInfo,
    @SerialName("venues")
    val venues: List<ContentMapVenue> = emptyList()
)

@Serializable
data class ContentMapVenue(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)
