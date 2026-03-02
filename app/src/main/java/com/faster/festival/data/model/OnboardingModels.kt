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
    val wristband_code: String? = null,
    val terms_acceptance: Boolean? = null
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

/**
 * Response body from ensure_festival_onboarding RPC.
 * The RPC returns an array of objects with festival and onboarding info.
 */
@Serializable
data class EnsureFestivalOnboardingResponse(
    val festival_id: String,
    val user_id: String,
    val festival_membership_id: String,
    val festival_membership_status: String,
    val onboarding_state_exists: Boolean
)

/**
 * Request for saving emergency contact via Supabase Edge Function.
 */
@Serializable
data class SaveEmergencyContactRequest(
    val festival_id: String? = null,  // Required for CREATE
    val id: String? = null,            // Required for UPDATE/DELETE
    val external_name: String? = null,
    val external_phone_e164: String? = null,
    val relationship: String? = null,
    val is_primary: Boolean? = null,
    val delete: Boolean? = null        // True for DELETE operation
)

/**
 * Emergency contact model for display.
 */
@Serializable
data class EmergencyContact(
    val id: String,
    val external_name: String,
    val external_phone_e164: String,
    val relationship: String? = null,
    val is_primary: Boolean = false
)

