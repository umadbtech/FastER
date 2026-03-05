package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for App Experience Bundle endpoint
 * GET /functions/v1/app-experience-bundle?festival_slug=<slug>
 *
 * Returns: AppExperienceBundleResponse with all experience-related content
 */
interface AppExperienceBundleApi {

    /**
     * Fetch complete experience bundle (categories, featured locations, bundles)
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with experience bundle data
     */
    @GET("functions/v1/app-experience-bundle")
    suspend fun getExperienceBundle(
        @Query("festival_slug") festivalSlug: String
    ): Response<AppExperienceBundleResponse>

    data class AppExperienceBundleResponse(
        val schema_version: String,
        val generated_at: String,
        val festival: ExperienceBundleFestival,
        val categories: List<ExperienceCategory>,
        val featured_locations: List<FeaturedLocation>,
        val experience_bundles: List<ExperienceBundle>,
        val ui_config: UIConfig
    )

    data class ExperienceBundleFestival(
        val id: String,
        val slug: String,
        val name: String
    )

    data class ExperienceCategory(
        val id: String,
        val name: String,
        val slug: String,
        val description: String?,
        val icon_url: String?,
        val color_hex: String?,
        val location_count: Int,
        val order: Int
    )

    data class FeaturedLocation(
        val id: String,
        val name: String,
        val slug: String,
        val category_id: String,
        val category_name: String,
        val description: String?,
        val image_url: String?,
        val rating: Float?,
        val latitude: Double?,
        val longitude: Double?,
        val featured: Boolean
    )

    data class ExperienceBundle(
        val id: String,
        val name: String,
        val description: String?,
        val theme: String?,
        val duration_minutes: Int?,
        val price: Double?,
        val currency: String?,
        val included_locations: List<String>,
        val image_url: String?,
        val difficulty_level: String?,
        val max_group_size: Int?
    )

    data class UIConfig(
        val show_map: Boolean,
        val show_ratings: Boolean,
        val show_pricing: Boolean,
        val featured_category: String?,
        val category_order: List<String>?
    )
}
