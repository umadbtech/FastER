package com.faster.festival.data.remote

import com.faster.festival.data.model.OnboardingResponse
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveUsernameRequest
import com.faster.festival.data.model.EnsureOnboardingResponse
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.model.EnsureFestivalOnboardingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Supabase Edge Functions and RPC calls related to onboarding.
 */
interface OnboardingApiService {

    /**
     * POST /rest/v1/rpc/ensure_festival_onboarding
     * Idempotent call to initialize onboarding state and retrieve festival info.
     * Returns a list with a single object containing festival_id and onboarding status.
     */
    @POST("rest/v1/rpc/ensure_festival_onboarding")
    suspend fun ensureOnboarding(
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String> = mapOf()
    ): Response<List<EnsureFestivalOnboardingResponse>>

    /**
     * POST /functions/v1/save-username
     * Save username via Edge Function.
     */
    @POST("functions/v1/save-username")
    suspend fun saveUsername(
        @Header("Authorization") authorization: String,
        @Body request: SaveUsernameRequest
    ): Response<OnboardingResponse>

    /**
     * POST /functions/v1/save-demographics
     * Save demographics (DOB, race, gender) via Edge Function.
     */
    @POST("functions/v1/save-demographics")
    suspend fun saveDemographics(
        @Header("Authorization") authorization: String,
        @Body request: SaveDemographicsRequest
    ): Response<OnboardingResponse>

    /**
     * POST /functions/v1/save-wristband
     * Save wristband pairing info via Edge Function.
     */
    @POST("functions/v1/save-wristband")
    suspend fun saveWristband(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, String>
    ): Response<OnboardingResponse>

    /**
     * POST /functions/v1/save-emergency-contact
     * Save emergency contact via Edge Function (create/update/delete).
     */
    @POST("functions/v1/save-emergency-contact")
    suspend fun saveEmergencyContact(
        @Header("Authorization") authorization: String,
        @Body request: SaveEmergencyContactRequest
    ): Response<OnboardingResponse>
}
