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
                            val heroMediaArray = item["media_urls"] as? JsonArray
                            val heroMediaList = heroMediaArray?.mapNotNull { el ->
                                (el as? JsonPrimitive)?.content
                            } ?: emptyList()
                            HeroCarouselItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                kind = (item["kind"] as? JsonPrimitive)?.content,
                                refId = (item["ref_id"] as? JsonPrimitive)?.content,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                subtitle = (item["subtitle"] as? JsonPrimitive)?.content,
                                description = (item["description"] as? JsonPrimitive)?.content,
                                imageUrl = (item["image_url"] as? JsonPrimitive)?.content,
                                ctaLabel = (item["cta_label"] as? JsonPrimitive)?.content,
                                ctaUrl = (item["cta_url"] as? JsonPrimitive)?.content,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
                                startsAt = (item["starts_at"] as? JsonPrimitive)?.content,
                                endsAt = (item["ends_at"] as? JsonPrimitive)?.content,
                                locationText = (item["location_text"] as? JsonPrimitive)?.content,
                                venueName = (item["venue_name"] as? JsonPrimitive)?.content,
                                mediaUrls = heroMediaList
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
                                content = (item["body"] as? JsonPrimitive)?.content
                                    ?: (item["content"] as? JsonPrimitive)?.content,
                                imageUrl = (item["image_url"] as? JsonPrimitive)?.content,
                                publishedAt = (item["published_at"] as? JsonPrimitive)?.content,
                                startsAt = (item["starts_at"] as? JsonPrimitive)?.content,
                                endsAt = (item["ends_at"] as? JsonPrimitive)?.content,
                                priority = (item["priority"] as? JsonPrimitive)?.intOrNull ?: 0,
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

    /**
     * Extract FAQ items from modules
     */
    val faqItems: List<FaqItem>
        get() {
            val moduleData = modules.find { it.key == "faq" }?.data
            return if (moduleData is JsonObject) {
                val itemsArray = moduleData["items"] as? JsonArray
                itemsArray?.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            FaqItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                question = (item["question"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                answer = (item["answer"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }?.sortedBy { it.sortOrder } ?: emptyList()
            } else {
                emptyList()
            }
        }

    /**
     * Extract promotions from modules
     */
    val promotions: List<PromotionItem>
        get() {
            val moduleData = modules.find { it.key == "promotions" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            val mediaArray = item["media_urls"] as? JsonArray
                            val mediaList = mediaArray?.mapNotNull { el ->
                                (el as? JsonPrimitive)?.content
                            } ?: emptyList()
                            PromotionItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                subtitle = (item["subtitle"] as? JsonPrimitive)?.content,
                                slug = (item["slug"] as? JsonPrimitive)?.content,
                                offerText = (item["offer_text"] as? JsonPrimitive)?.content,
                                description = (item["description"] as? JsonPrimitive)?.content,
                                thumbnailUrl = (item["thumbnail_url"] as? JsonPrimitive)?.content,
                                isExclusive = (item["is_exclusive"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
                                isActive = (item["is_active"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
                                vendorName = (item["vendor_name"] as? JsonPrimitive)?.content,
                                locationText = (item["location_text"] as? JsonPrimitive)?.content,
                                phone = (item["phone"] as? JsonPrimitive)?.content,
                                website = (item["website"] as? JsonPrimitive)?.content,
                                address = (item["address"] as? JsonPrimitive)?.content,
                                hoursText = (item["hours_text"] as? JsonPrimitive)?.content,
                                menuUrl = (item["menu_url"] as? JsonPrimitive)?.content,
                                ctaLabel = (item["cta_label"] as? JsonPrimitive)?.content,
                                ctaUrl = (item["cta_url"] as? JsonPrimitive)?.content,
                                mediaUrls = mediaList
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        }

    /**
     * Extract sponsor offers from modules
     * API nests sponsor info under a "sponsors" object within each offer
     */
    val sponsorOffers: List<SponsorOffer>
        get() {
            val moduleData = modules.find { it.key == "sponsors" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            val sponsorObj = item["sponsors"] as? JsonObject
                            val mediaArray = item["media_urls"] as? JsonArray
                            val mediaList = mediaArray?.mapNotNull { el ->
                                (el as? JsonPrimitive)?.content
                            } ?: emptyList()
                            SponsorOffer(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                sponsorName = (sponsorObj?.get("name") as? JsonPrimitive)?.content
                                    ?: (item["sponsor_name"] as? JsonPrimitive)?.content
                                    ?: return@mapNotNull null,
                                sponsorLogoUrl = (sponsorObj?.get("logo_url") as? JsonPrimitive)?.content
                                    ?: (item["sponsor_logo_url"] as? JsonPrimitive)?.content,
                                title = (item["title"] as? JsonPrimitive)?.content,
                                subtitle = (item["subtitle"] as? JsonPrimitive)?.content,
                                description = (item["description"] as? JsonPrimitive)?.content,
                                offerText = (item["offer_text"] as? JsonPrimitive)?.content,
                                ctaLabel = (item["cta_label"] as? JsonPrimitive)?.content,
                                ctaUrl = (item["cta_url"] as? JsonPrimitive)?.content,
                                isExclusive = (item["is_exclusive"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
                                primaryMediaUrl = (item["primary_media_url"] as? JsonPrimitive)?.content,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
                                vendorName = (item["vendor_name"] as? JsonPrimitive)?.content,
                                locationText = (item["location_text"] as? JsonPrimitive)?.content,
                                phone = (item["phone"] as? JsonPrimitive)?.content,
                                website = (item["website"] as? JsonPrimitive)?.content,
                                address = (item["address"] as? JsonPrimitive)?.content,
                                hoursText = (item["hours_text"] as? JsonPrimitive)?.content,
                                mediaUrls = mediaList
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        }

    /**
     * Extract perks from modules
     */
    val perks: List<PerkItem>
        get() {
            val moduleData = modules.find { it.key == "perks" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            PerkItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                description = (item["description"] as? JsonPrimitive)?.content,
                                imageUrl = (item["image_url"] as? JsonPrimitive)?.content,
                                ctaLabel = (item["cta_label"] as? JsonPrimitive)?.content,
                                ctaUrl = (item["cta_url"] as? JsonPrimitive)?.content,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        }

    /**
     * Extract alerts from modules
     */
    val alerts: List<AlertItem>
        get() {
            val moduleData = modules.find { it.key == "alerts" }?.data
            return if (moduleData is JsonArray) {
                moduleData.mapNotNull { item ->
                    if (item is JsonObject) {
                        try {
                            AlertItem(
                                id = (item["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                title = (item["title"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                                body = (item["body"] as? JsonPrimitive)?.content
                                    ?: (item["message"] as? JsonPrimitive)?.content,
                                severity = (item["severity"] as? JsonPrimitive)?.content ?: "info",
                                startsAt = (item["starts_at"] as? JsonPrimitive)?.content,
                                endsAt = (item["ends_at"] as? JsonPrimitive)?.content,
                                sortOrder = (item["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }.sortedBy { it.sortOrder }
            } else {
                emptyList()
            }
        }

    /**
     * Check if a module is enabled
     */
    fun isModuleEnabled(key: String): Boolean =
        modules.find { it.key == key }?.enabled ?: false

    /**
     * Get a module by key
     */
    fun moduleByKey(key: String): HomeModule? = modules.find { it.key == key }

    /**
     * Get enabled modules in the order specified by ui_config.module_order
     */
    val orderedModules: List<HomeModule>
        get() {
            val enabledMap = modules.filter { it.enabled }.associateBy { it.key }
            return if (uiConfig.moduleOrder.isNotEmpty()) {
                uiConfig.moduleOrder.mapNotNull { key -> enabledMap[key] }
            } else {
                enabledMap.values.toList()
            }
        }
}

/**
 * Promotion item from modules[key="promotions"].data[n]
 */
@Serializable
data class PromotionItem(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("slug")
    val slug: String? = null,
    @SerialName("offer_text")
    val offerText: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("is_exclusive")
    val isExclusive: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("vendor_name")
    val vendorName: String? = null,
    @SerialName("location_text")
    val locationText: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("website")
    val website: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("hours_text")
    val hoursText: String? = null,
    @SerialName("menu_url")
    val menuUrl: String? = null,
    @SerialName("cta_label")
    val ctaLabel: String? = null,
    @SerialName("cta_url")
    val ctaUrl: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String> = emptyList()
)

/**
 * Sponsor offer from modules[key="sponsors"].data[n]
 * Sponsor name/logo come from the nested "sponsors" object in the API response
 */
@Serializable
data class SponsorOffer(
    @SerialName("id")
    val id: String,
    val sponsorName: String,
    val sponsorLogoUrl: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("offer_text")
    val offerText: String? = null,
    @SerialName("cta_label")
    val ctaLabel: String? = null,
    @SerialName("cta_url")
    val ctaUrl: String? = null,
    @SerialName("is_exclusive")
    val isExclusive: Boolean = false,
    val primaryMediaUrl: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("vendor_name")
    val vendorName: String? = null,
    @SerialName("location_text")
    val locationText: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("website")
    val website: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("hours_text")
    val hoursText: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String> = emptyList()
)

/**
 * FAQ item from modules[key="faq"].data.items[n]
 */
@Serializable
data class FaqItem(
    @SerialName("id")
    val id: String,
    @SerialName("question")
    val question: String,
    @SerialName("answer")
    val answer: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

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
    @SerialName("banner_urls")
    val bannerUrls: List<String> = emptyList(),  // Array of banner images for carousel
    @SerialName("accent_color_hex")
    val accentColorHex: String? = null,
    @SerialName("location")
    val location: String? = null,
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
    @SerialName("description")
    val description: String? = null,
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
    val endsAt: String? = null,
    @SerialName("location_text")
    val locationText: String? = null,
    @SerialName("venue_name")
    val venueName: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String> = emptyList()
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
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("priority")
    val priority: Int = 0,
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
 * Perk item from modules[key="perks"].data[n]
 */
@Serializable
data class PerkItem(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("cta_label")
    val ctaLabel: String? = null,
    @SerialName("cta_url")
    val ctaUrl: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

/**
 * Alert item from modules[key="alerts"].data[n]
 */
@Serializable
data class AlertItem(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String? = null,
    @SerialName("severity")
    val severity: String = "info", // "info", "warning", "critical"
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0
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
    @SerialName("title")
    val title: String? = null,
    @SerialName("display_title")
    val displayTitle: String? = null,
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
    val order: Int = 0,
    @SerialName("label")
    val label: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("icon")
    val icon: String? = null
)
