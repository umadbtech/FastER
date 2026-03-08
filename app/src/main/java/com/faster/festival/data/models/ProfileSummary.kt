package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Profile Summary API Response Data Classes
 */

@Serializable
data class ProfileSummaryResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("profile")
    val profile: Profile,
    @SerialName("demographics")
    val demographics: Demographics? = null,
    @SerialName("emergency_contacts")
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    @SerialName("signed_avatar_url")
    val signedAvatarUrl: String? = null,
    @SerialName("terms")
    val terms: Terms? = null,
    @SerialName("context")
    val context: Context? = null
)

@Serializable
data class Profile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("legal_first_name")
    val legalFirstName: String? = null,
    @SerialName("legal_last_name")
    val legalLastName: String? = null,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @SerialName("is_minor")
    val isMinor: Boolean? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Demographics(
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerialName("race_ethnicity")
    val raceEthnicity: List<String>? = null,
    @SerialName("gender_identity")
    val genderIdentity: String? = null,
    @SerialName("wristband_code")
    val wristbandCode: String? = null
)

@Serializable
data class EmergencyContact(
    @SerialName("id")
    val id: String,
    @SerialName("festival_id")
    val festivalId: String,
    @SerialName("owner_user_id")
    val ownerUserId: String,
    @SerialName("contact_user_id")
    val contactUserId: String? = null,
    @SerialName("external_id")
    val externalId: String? = null,
    @SerialName("external_name")
    val externalName: String? = null,
    @SerialName("external_phone_e164")
    val externalPhoneE164: String? = null,
    @SerialName("relationship")
    val relationship: String? = null,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Terms(
    @SerialName("terms_version")
    val termsVersion: String? = null,
    @SerialName("accepted_at")
    val acceptedAt: String? = null,
    @SerialName("complete")
    val complete: Boolean = false
)

@Serializable
data class Context(
    @SerialName("festival_id")
    val festivalId: String? = null
)

/**
 * UI representation of profile data
 */
data class ProfileSummary(
    val userId: String,
    val username: String?,
    val legalFirstName: String?,
    val legalLastName: String?,
    val avatarUrl: String?,
    val isMinor: Boolean?,
    val emergencyContactsCount: Int,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val termsComplete: Boolean,
    val festivalId: String?,
    val connectionStatus: String = "Strong Connection",
    val batteryPercentage: Int = 82
)

fun ProfileSummaryResponse.toProfileSummary(): ProfileSummary {
    return ProfileSummary(
        userId = profile.userId,
        username = profile.username,
        legalFirstName = profile.legalFirstName,
        legalLastName = profile.legalLastName,
        avatarUrl = signedAvatarUrl,
        isMinor = profile.isMinor,
        emergencyContactsCount = emergencyContacts.size,
        emergencyContacts = emergencyContacts,
        termsComplete = terms?.complete ?: false,
        festivalId = context?.festivalId
    )
}
