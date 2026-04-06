package com.faster.festival.data.remote

import com.faster.festival.data.models.AppHomeBundleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit API service for Content Home endpoint
 * GET /functions/v1/content-home?festival_slug=<slug>
 *
 * Returns: ContentHomeResponse with featured content, announcements, events
 */
interface ContentHomeApi {

    /**
     * Fetch home screen content (featured items, announcements, events)
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with home content data
     */
    @GET("functions/v1/content-home")
    suspend fun getContentHome(
        @Query("festival_slug") festivalSlug: String
    ): Response<ContentHomeResponse>

    data class ContentHomeResponse(
        val schema_version: String,
        val generated_at: String,
        val festival: HomeFestival,
        val featured_items: List<FeaturedItem>,
        val announcements: List<Announcement>,
        val upcoming_events: List<UpcomingEvent>,
        val quick_actions: List<QuickAction>?
    )

    data class HomeFestival(
        val id: String,
        val slug: String,
        val name: String,
        val timezone: String,
        val starts_at: String,
        val ends_at: String
    )

    data class FeaturedItem(
        val id: String,
        val title: String,
        val subtitle: String?,
        val image_url: String?,
        val cta_url: String?,
        val order: Int
    )

    data class Announcement(
        val id: String,
        val title: String,
        val content: String?,
        val image_url: String?,
        val published_at: String?,
        val order: Int
    )

    data class UpcomingEvent(
        val id: String,
        val title: String,
        val description: String?,
        val start_time: String?,
        val end_time: String?,
        val venue: Venue?,
        val order: Int
    )

    data class Venue(
        val id: String?,
        val name: String,
        val location: String?
    )

    data class QuickAction(
        val id: String,
        val label: String,
        val icon: String?,
        val action_type: String,
        val target_url: String?
    )
}
