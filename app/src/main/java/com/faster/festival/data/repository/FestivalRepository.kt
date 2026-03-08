package com.faster.festival.data.repository

import com.faster.festival.data.models.*
import kotlinx.coroutines.flow.Flow

// ============================================================================
// FESTIVAL REPOSITORY INTERFACE - DEPRECATED
// ============================================================================

/**
 * ⚠️ DEPRECATED: Festival Repository Interface
 *
 * This interface is deprecated. Use AppHomeRepository instead.
 *
 * AppHomeRepository provides server-driven UI with festival header + modules,
 * which is the new standard for content delivery in the app.
 *
 * Migration path:
 * 1. Replace FestivalRepository usage with AppHomeRepository
 * 2. Use AppHomeViewModel for loading home content
 * 3. Access festival data from AppHomeBundleResponse.festival
 *
 * Legacy implementations:
 * - SupabaseFestivalRepository: DELETED (Phase 2)
 * - ContentRepository: Use AppHomeRepository instead
 *
 * This interface is kept for backward compatibility with legacy screens.
 * Will be removed in Phase 3 once all screens are migrated.
 */
@Deprecated(
    message = "Use AppHomeRepository instead for server-driven UI",
    replaceWith = ReplaceWith("AppHomeRepository")
)
interface FestivalRepository {
    /**
     * Get festival header information
     * API: GET /functions/v1/festival-header?festival_slug=<slug>
     */
    fun getFestival(): Flow<Festival>

    /**
     * Get list of featured artists for the festival
     * API: GET /functions/v1/content-lineup?festival_slug=<slug>
     */
    fun getArtists(): Flow<List<Artist>>

    /**
     * Get specific artist by ID
     * API: GET /functions/v1/content-lineup?festival_slug=<slug>
     * (filters by ID locally)
     */
    fun getArtistById(id: String): Flow<Artist?>

    /**
     * Get points of interest on festival map
     * API: GET /functions/v1/content-map?festival_slug=<slug>
     */
    fun getPois(): Flow<List<Poi>>

    /**
     * Get festival schedule/lineup timing
     * API: GET /functions/v1/content-stage-schedule?festival_slug=<slug>
     */
    fun getSchedule(): Flow<List<ScheduleItem>>

    /**
     * Get user profile information
     * API: GET /functions/v1/profile-summary with Authorization header
     */
    fun getProfile(): Flow<AccountProfile>

    /**
     * Update user profile
     * API: PUT /functions/v1/profile-summary with Authorization header
     * (Not yet implemented - API endpoint needed)
     */
    fun updateProfile(profile: AccountProfile): Flow<AccountProfile>
}

