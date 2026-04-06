package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content Home API Response
 * GET /functions/v1/content-home?festival_slug=<slug>
 */
@Serializable
data class ContentHomeResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("festival")
    val festival: ContentFestivalInfo,
    @SerialName("announcements")
    val announcements: List<ContentAnnouncement> = emptyList(),
    @SerialName("hero_carousel_items")
    val heroCarouselItems: List<ContentHeroCarouselItem> = emptyList(),
    @SerialName("upcoming_events")
    val upcomingEvents: List<ContentUpcomingEvent> = emptyList()
)

@Serializable
data class ContentFestivalInfo(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("timezone")
    val timezone: String = "",
    @SerialName("starts_at")
    val startsAt: String = "",
    @SerialName("ends_at")
    val endsAt: String = ""
)

@Serializable
data class ContentAnnouncement(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class ContentHeroCarouselItem(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("cta_url")
    val ctaUrl: String? = null,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class ContentUpcomingEvent(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("venue")
    val venue: ContentVenue? = null,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class ContentVenue(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("location")
    val location: String? = null
)
