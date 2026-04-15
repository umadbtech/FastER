package com.faster.festival.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Gender identity enum matching PostgreSQL gender_identity_enum values exactly.
 * [apiValue] is sent to the backend; [displayLabel] is shown in the UI.
 */
enum class GenderIdentity(val apiValue: String, val displayLabel: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    NON_BINARY("non_binary", "Non-binary"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say"),
    SELF_DESCRIBE("self_describe", "Self-describe");

    companion object {
        /** Map a display label (e.g. "Female") to the DB-safe API value (e.g. "female"). */
        fun toApiValue(displayLabel: String): String? =
            entries.find { it.displayLabel.equals(displayLabel, ignoreCase = true) }?.apiValue

        /** Map an API value (e.g. "female") back to the display label (e.g. "Female"). */
        fun toDisplayLabel(apiValue: String): String? =
            entries.find { it.apiValue.equals(apiValue, ignoreCase = true) }?.displayLabel

        /** All display labels for use in UI dropdowns. */
        val displayLabels: List<String> get() = entries.map { it.displayLabel }
    }
}

/**
 * Race / Ethnicity enum matching PostgreSQL race_ethnicity allowed values.
 */
enum class RaceEthnicity(val apiValue: String, val displayLabel: String) {
    AMERICAN_INDIAN("american_indian_or_alaska_native", "American Indian or Alaska Native"),
    ASIAN("asian", "Asian"),
    BLACK("black_or_african_american", "Black or African American"),
    HISPANIC("hispanic_or_latino", "Hispanic or Latino"),
    PACIFIC_ISLANDER("native_hawaiian_or_pacific_islander", "Native Hawaiian or Pacific Islander"),
    WHITE("white", "White"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say"),
    SELF_DESCRIBE("self_describe", "Self-describe");

    companion object {
        fun toApiValue(displayLabel: String): String? =
            entries.find { it.displayLabel.equals(displayLabel, ignoreCase = true) }?.apiValue

        fun toDisplayLabel(apiValue: String): String? =
            entries.find { it.apiValue.equals(apiValue, ignoreCase = true) }?.displayLabel

        val displayLabels: List<String> get() = entries.map { it.displayLabel }
    }
}

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
    val dob: String? = null, // YYYY-MM-DD, not future, within last 120 years
    val gender_identity: String? = null, // male, female, non_binary, prefer_not_to_say, self_describe
    val gender_identity_text: String? = null, // required if gender_identity = self_describe
    val race_ethnicity: List<String>? = null,
    val race_ethnicity_text: String? = null // required if race_ethnicity contains self_describe
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

/**
 * Request for saving profile name via Supabase Edge Function.
 */
@Serializable
data class SaveProfileNameRequest(
    @SerialName("legal_first_name") val legalFirstName: String,
    @SerialName("legal_last_name") val legalLastName: String
)

