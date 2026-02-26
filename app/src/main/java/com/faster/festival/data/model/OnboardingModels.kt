package com.faster.festival.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Request for saving username via Supabase Edge Function.
 */
@Serializable
data class SaveUsernameRequest(
    val username: String
)

/**
 * Request for saving demographics via Supabase Edge Function.
 */
@Serializable
data class SaveDemographicsRequest(
    val dob: String?, // YYYY-MM-DD
    val race_ethnicity: List<String>? = null,
    val race_ethnicity_text: String? = null,
    val gender_identity: String? = null,
    val gender_identity_text: String? = null,
    val wristband_code: String? = null
)

/**
 * Response from Supabase Edge Functions.
 */
@Serializable
data class OnboardingResponse(
    val saved: Boolean? = null,
    val activated: Boolean? = null,
    val status: String? = null,
    val missing: List<String>? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Response for ensure_festival_onboarding RPC call.
 */
@Serializable
data class EnsureOnboardingResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val error: String? = null
)
