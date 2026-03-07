package com.faster.festival.data.repository

import com.faster.festival.data.models.*
import kotlinx.coroutines.flow.Flow

// ============================================================================
// FESTIVAL REPOSITORY INTERFACE
// ============================================================================

/**
 * Festival Repository Interface
 *
 * All implementations must fetch data from Supabase Edge Function APIs
 * NO hardcoded default values or fake data allowed
 *
 * Implementations:
 * - SupabaseFestivalRepository: Implements all endpoints with real API calls
 * - ContentRepository: Separate repository for app-home-bundle and content endpoints
 */
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

