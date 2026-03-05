package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * App Home Bundle API Response - Server-driven home screen configuration
 * GET /functions/v1/app-home-bundle?festival_slug=<slug>
 *
 * NOTE: hero_carousel_items, announcements, and upcoming_events are extracted
 * from the modules array by module key and converted from JsonElement
 */
@Serializable
data class AppHomeBundleResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("generated_at")
    val generatedAt: String,
    @SerialName("festival")
    val festival: AppFestivalHeader,
    @SerialName("modules")
    val modules: List<HomeModule> = emptyList(),
    @SerialName("ui_config")
    val uiConfig: UiConfig = UiConfig()
) {
    /**
     * Extract hero carousel items from modules
     */
    val heroCarouselItems: List<HeroCarouselItem>
        get() {
            val moduleData = modules.find { it.key == "hero_carousel" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            HeroCarouselItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                kind = (item["kind"] as? JsonPrimitive)?.content,
                                refId = (item["ref_id"] as? JsonPrimitive)?.content,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                subtitle = (item["subtitle"] as? JsonPrimitive)?.content,
                                imageUrl = (item["image_url"] as? JsonPrimitive)?.content,
                                ctaLabel = (item["cta_label"] as? JsonPrimitive)?.content,
                                ctaUrl = (item["cta_url"] as? JsonPrimitive)?.content,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
                                startsAt = (item["starts_at"] as? JsonPrimitive)?.content,
                                endsAt = (item["ends_at"] as? JsonPrimitive)?.content
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
        }

    /**
     * Extract announcements from modules
     */
    val announcements: List<Announcement>
        get() {
            val moduleData = modules.find { it.key == "announcements" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            Announcement(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                content = (item["content"] as? JsonPrimitive)?.content,
                                imageUrl = (item["image_url"] as? JsonPrimitive)?.content,
                                publishedAt = (item["published_at"] as? JsonPrimitive)?.content,
                                order = (item["order"] as? JsonPrimitive)?.intOrNull ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
        }

    /**
     * Extract upcoming events from modules
     */
    val upcomingEvents: List<UpcomingEvent>
        get() {
            val moduleData = modules.find { it.key == "upcoming_events" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            val venueData = item["venue"] as? JsonObject
                            val venue = if (venueData != null) {
                                Venue(
                                    id = (venueData["id"] as? JsonPrimitive)?.content,
                                    kind = (venueData["kind"] as? JsonPrimitive)?.content,
                                    name = (venueData["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                    slug = (venueData["slug"] as? JsonPrimitive)?.content,
                                    location = (venueData["location"] as? JsonPrimitive)?.content
                                )
                            } else {
                                null
                            }
                            UpcomingEvent(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                name = (item["name"] as? JsonPrimitive)?.content,
                                description = (item["description"] as? JsonPrimitive)?.content,
                                startsAt = (item["starts_at"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                endsAt = (item["ends_at"] as? JsonPrimitive)?.content,
                                status = (item["status"] as? JsonPrimitive)?.content,
                                venue = venue
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
        }
}

/**
 * Festival header from App Home Bundle
 */
@Serializable
data class AppFestivalHeader(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("timezone")
    val timezone: String,
    @SerialName("starts_at")
    val startsAt: String,
    @SerialName("ends_at")
    val endsAt: String,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("banner_url")
    val bannerUrl: String? = null,
    @SerialName("accent_color_hex")
    val accentColorHex: String? = null,
    @SerialName("context_state")
    val contextState: String = "PRE",
    @SerialName("status")
    val status: String = "draft"  // "draft" or "published"
)

/**
 * Hero carousel item for banner/hero sections
 * Matches API response structure from modules[key="hero_carousel"].data[n]
 */
@Serializable
data class HeroCarouselItem(
    @SerialName("id")
    val id: String,
    @SerialName("kind")
    val kind: String? = null, // "artist", "event", "custom"
    @SerialName("ref_id")
    val refId: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("cta_label")
    val ctaLabel: String? = null,
    @SerialName("cta_url")
    val ctaUrl: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null
)

/**
 * Announcement item
 */
@Serializable
data class Announcement(
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

/**
 * Upcoming event item from modules[key="upcoming_events"].data[n]
 */
@Serializable
data class UpcomingEvent(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("starts_at")
    val startsAt: String,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("status")
    val status: String? = null, // "published", "draft"
    @SerialName("venue")
    val venue: Venue? = null
)

/**
 * Venue information from event data
 */
@Serializable
data class Venue(
    @SerialName("id")
    val id: String? = null,
    @SerialName("kind")
    val kind: String? = null, // "stage", "area", "venue"
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String? = null,
    @SerialName("location")
    val location: String? = null
)

/**
 * Dynamically configured module from backend
 * The data field can contain arrays of HeroCarouselItem, Announcement, UpcomingEvent, etc.
 * Stored as JsonElement to preserve structure until conversion
 */
@Serializable
data class HomeModule(
    @SerialName("key")
    val key: String,
    @SerialName("enabled")
    val enabled: Boolean = true,
    @SerialName("ttl_seconds")
    val ttlSeconds: Int? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("version")
    val version: String? = null,
    @SerialName("data")
    val data: JsonElement? = null  // Stores the raw JSON structure
)

/**
 * UI configuration for home screen
 */
@Serializable
data class UiConfig(
    @SerialName("tiles")
    val tiles: List<TileConfig> = emptyList(),
    @SerialName("module_order")
    val moduleOrder: List<String> = emptyList()
)

/**
 * Individual tile configuration
 */
@Serializable
data class TileConfig(
    @SerialName("key")
    val key: String,
    @SerialName("enabled")
    val enabled: Boolean = true,
    @SerialName("order")
    val order: Int = 0
)
