package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Festival Experience endpoints
 *
 * 1. GET /functions/v1/festival-experience-categories?festival_slug=<slug>
 * 2. GET /functions/v1/festival-experience-locations?festival_slug=<slug>&category=<category>
 * 3. GET /functions/v1/festival-experience-location?id=<id>
 */
interface FestivalExperienceApi {

    /**
     * Fetch experience categories
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with experience categories
     */
    @GET("functions/v1/festival-experience-categories")
    suspend fun getExperienceCategories(
        @Query("festival_slug") festivalSlug: String
    ): Response<ExperienceCategoriesResponse>

    /**
     * Fetch experience locations by category
     *
     * @param festivalSlug Festival identifier (required)
     * @param category Category identifier (required)
     * @return Response with experience locations for the category
     */
    @GET("functions/v1/festival-experience-locations")
    suspend fun getExperienceLocationsByCategory(
        @Query("festival_slug") festivalSlug: String,
        @Query("category") category: String
    ): Response<ExperienceLocationsResponse>

    /**
     * Fetch single experience location details
     *
     * @param id Location identifier (required)
     * @return Response with experience location details
     */
    @GET("functions/v1/festival-experience-location")
    suspend fun getExperienceLocation(
        @Query("id") id: String
    ): Response<ExperienceLocationDetailResponse>

    // ========== Response Models ==========

    data class ExperienceCategoriesResponse(
        val schema_version: String,
        val categories: List<ExperienceCategory>
    )

    data class ExperienceCategory(
        val id: String,
        val name: String,
        val slug: String,
        val description: String?,
        val icon_url: String?,
        val color_hex: String?,
        val order: Int
    )

    // ========== Locations by Category Response ==========

    data class ExperienceLocationsResponse(
        val schema_version: String,
        val category: ExperienceCategory,
        val locations: List<ExperienceLocation>
    )

    data class ExperienceLocation(
        val id: String,
        val name: String,
        val slug: String,
        val category_id: String,
        val description: String?,
        val image_url: String?,
        val thumbnail_url: String?,
        val latitude: Double?,
        val longitude: Double?,
        val address: String?,
        val opening_hours: String?,
        val capacity: Int?,
        val features: List<String>?,
        val rating: Float?,
        val review_count: Int?,
        val order: Int
    )

    // ========== Location Detail Response ==========

    data class ExperienceLocationDetailResponse(
        val schema_version: String,
        val location: ExperienceLocationDetail
    )

    data class ExperienceLocationDetail(
        val id: String,
        val name: String,
        val slug: String,
        val category_id: String,
        val category_name: String,
        val description: String?,
        val long_description: String?,
        val images: List<LocationImage>,
        val latitude: Double?,
        val longitude: Double?,
        val address: String?,
        val opening_hours: String?,
        val capacity: Int?,
        val accessibility: AccessibilityInfo?,
        val amenities: List<String>?,
        val features: List<String>?,
        val pricing: PricingInfo?,
        val contact: ContactInfo?,
        val rating: Float?,
        val reviews: List<Review>?,
        val events: List<LocationEvent>?
    )

    data class LocationImage(
        val id: String,
        val url: String,
        val thumbnail_url: String?,
        val caption: String?,
        val order: Int
    )

    data class AccessibilityInfo(
        val wheelchair_accessible: Boolean,
        val has_elevator: Boolean,
        val has_restrooms: Boolean,
        val has_parking: Boolean,
        val notes: String?
    )

    data class PricingInfo(
        val currency: String?,
        val base_price: Double?,
        val price_per_person: Double?,
        val group_discount: Double?,
        val notes: String?
    )

    data class ContactInfo(
        val phone: String?,
        val email: String?,
        val website: String?,
        val social_media: Map<String, String>?
    )

    data class Review(
        val id: String,
        val author_name: String,
        val rating: Float,
        val comment: String?,
        val date: String
    )

    data class LocationEvent(
        val id: String,
        val name: String,
        val description: String?,
        val start_time: String,
        val end_time: String,
        val max_attendees: Int?
    )
}
