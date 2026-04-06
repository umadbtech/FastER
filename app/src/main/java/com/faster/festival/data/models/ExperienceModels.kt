package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Festival Experience Categories API Response
 * GET /functions/v1/festival-experience-categories?festival_slug=<slug>
 */
@Serializable
data class ExperienceCategoriesResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("categories")
    val categories: List<ExperienceCategory> = emptyList()
)

@Serializable
data class ExperienceCategory(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("color_hex")
    val colorHex: String? = null,
    @SerialName("order")
    val order: Int = 0
)

/**
 * Festival Experience Locations API Response
 * GET /functions/v1/festival-experience-locations?festival_slug=<slug>&category=<category>
 */
@Serializable
data class ExperienceLocationsResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("category")
    val category: ExperienceCategory,
    @SerialName("locations")
    val locations: List<ExperienceLocation> = emptyList()
)

@Serializable
data class ExperienceLocation(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("features")
    val features: List<String>? = null,
    @SerialName("rating")
    val rating: Float? = null,
    @SerialName("review_count")
    val reviewCount: Int? = null,
    @SerialName("order")
    val order: Int = 0
)

/**
 * Festival Experience Location Detail API Response
 * GET /functions/v1/festival-experience-location?id=<id>
 */
@Serializable
data class ExperienceLocationDetailResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    @SerialName("location")
    val location: ExperienceLocationDetail
)

@Serializable
data class ExperienceLocationDetail(
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
    @SerialName("long_description")
    val longDescription: String? = null,
    @SerialName("images")
    val images: List<LocationImage> = emptyList(),
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("accessibility")
    val accessibility: AccessibilityInfo? = null,
    @SerialName("amenities")
    val amenities: List<String>? = null,
    @SerialName("features")
    val features: List<String>? = null,
    @SerialName("pricing")
    val pricing: PricingInfo? = null,
    @SerialName("contact")
    val contact: ContactInfo? = null,
    @SerialName("rating")
    val rating: Float? = null,
    @SerialName("reviews")
    val reviews: List<Review>? = null,
    @SerialName("events")
    val events: List<LocationEvent>? = null
)

@Serializable
data class LocationImage(
    @SerialName("id")
    val id: String,
    @SerialName("url")
    val url: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("caption")
    val caption: String? = null,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class AccessibilityInfo(
    @SerialName("wheelchair_accessible")
    val wheelchairAccessible: Boolean = false,
    @SerialName("has_elevator")
    val hasElevator: Boolean = false,
    @SerialName("has_restrooms")
    val hasRestrooms: Boolean = false,
    @SerialName("has_parking")
    val hasParking: Boolean = false,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class PricingInfo(
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("base_price")
    val basePrice: Double? = null,
    @SerialName("price_per_person")
    val pricePerPerson: Double? = null,
    @SerialName("group_discount")
    val groupDiscount: Double? = null,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class ContactInfo(
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("website")
    val website: String? = null,
    @SerialName("social_media")
    val socialMedia: Map<String, String>? = null
)

@Serializable
data class Review(
    @SerialName("id")
    val id: String,
    @SerialName("author_name")
    val authorName: String,
    @SerialName("rating")
    val rating: Float,
    @SerialName("comment")
    val comment: String? = null,
    @SerialName("date")
    val date: String
)

@Serializable
data class LocationEvent(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("max_attendees")
    val maxAttendees: Int? = null
)
