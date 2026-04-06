package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * App Experience Bundle API Response
 * GET /functions/v1/app-experience-bundle?festival_slug=<slug>
 */
@Serializable
data class AppExperienceBundleResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("generated_at")
    val generatedAt: String,
    @SerialName("festival")
    val festival: ExperienceBundleFestival,
    @SerialName("categories")
    val categories: List<ExperienceCategory> = emptyList(),
    @SerialName("featured_locations")
    val featuredLocations: List<FeaturedLocation> = emptyList(),
    @SerialName("experience_bundles")
    val experienceBundles: List<ExperienceBundle> = emptyList(),
    @SerialName("ui_config")
    val uiConfig: ExperienceUIConfig = ExperienceUIConfig()
)

@Serializable
data class ExperienceBundleFestival(
    @SerialName("id")
    val id: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class FeaturedLocation(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("category_name")
    val categoryName: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("rating")
    val rating: Float? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("featured")
    val featured: Boolean = false
)

@Serializable
data class ExperienceBundle(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("theme")
    val theme: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    @SerialName("price")
    val price: Double? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("included_locations")
    val includedLocations: List<String> = emptyList(),
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("difficulty_level")
    val difficultyLevel: String? = null,
    @SerialName("max_group_size")
    val maxGroupSize: Int? = null
)

@Serializable
data class ExperienceUIConfig(
    @SerialName("show_map")
    val showMap: Boolean = true,
    @SerialName("show_ratings")
    val showRatings: Boolean = true,
    @SerialName("show_pricing")
    val showPricing: Boolean = true,
    @SerialName("featured_category")
    val featuredCategory: String? = null,
    @SerialName("category_order")
    val categoryOrder: List<String>? = null
)

/**
 * Offline Bundle API Response
 * GET /functions/v1/offline-bundle?festival_slug=<slug>
 *
 * Contains all essential content for offline-first functionality
 * Includes critical_sets map with keys: medical, water, restrooms, stages
 */
@Serializable
data class OfflineBundleResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("generated_at")
    val generatedAt: String,
    @SerialName("bundle_version")
    val bundleVersion: String,
    @SerialName("bundle_size_kb")
    val bundleSizeKb: Int,
    @SerialName("festival")
    val festival: OfflineFestival,
    @SerialName("artists")
    val artists: List<OfflineArtist> = emptyList(),
    @SerialName("schedule")
    val schedule: List<OfflineScheduleItem> = emptyList(),
    @SerialName("map")
    val map: OfflineMapData? = null,
    @SerialName("poi")
    val poi: List<OfflinePOI> = emptyList(),
    @SerialName("venues")
    val venues: List<OfflineVenue> = emptyList(),
    @SerialName("static_content")
    val staticContent: StaticContent? = null,
    @SerialName("critical_sets")
    val criticalSets: Map<String, List<String>> = emptyMap(),
    @SerialName("cache_metadata")
    val cacheMetadata: CacheMetadata? = null
)

@Serializable
data class OfflineFestival(
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
    val accentColorHex: String? = null
)

@Serializable
data class OfflineArtist(
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
    val socialLinks: Map<String, String>? = null
)

@Serializable
data class OfflineScheduleItem(
    @SerialName("id")
    val id: String,
    @SerialName("artist_id")
    val artistId: String,
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("stage_id")
    val stageId: String,
    @SerialName("stage_name")
    val stageName: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("day")
    val day: Int
)

@Serializable
data class OfflineMapData(
    @SerialName("id")
    val id: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("height")
    val height: Int? = null
)

@Serializable
data class OfflinePOI(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("x_coordinate")
    val xCoordinate: Int? = null,
    @SerialName("y_coordinate")
    val yCoordinate: Int? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class OfflineVenue(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null
)

@Serializable
data class StaticContent(
    @SerialName("faqs")
    val faqs: List<FAQ> = emptyList(),
    @SerialName("terms_conditions")
    val termsConditions: String? = null,
    @SerialName("privacy_policy")
    val privacyPolicy: String? = null,
    @SerialName("emergency_contacts")
    val emergencyContacts: List<OfflineEmergencyContact> = emptyList(),
    @SerialName("parking_info")
    val parkingInfo: String? = null,
    @SerialName("transportation_info")
    val transportationInfo: String? = null,
    @SerialName("health_safety_info")
    val healthSafetyInfo: String? = null
)

@Serializable
data class FAQ(
    @SerialName("id")
    val id: String,
    @SerialName("category")
    val category: String,
    @SerialName("question")
    val question: String,
    @SerialName("answer")
    val answer: String,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class OfflineEmergencyContact(
    @SerialName("id")
    val id: String,
    @SerialName("service_type")
    val serviceType: String,
    @SerialName("name")
    val name: String,
    @SerialName("phone")
    val phone: String,
    @SerialName("email")
    val email: String? = null
)

@Serializable
data class CacheMetadata(
    @SerialName("last_updated")
    val lastUpdated: String,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("etag")
    val etag: String? = null,
    @SerialName("compressed")
    val compressed: Boolean = false,
    @SerialName("compression_ratio")
    val compressionRatio: Float? = null
)
