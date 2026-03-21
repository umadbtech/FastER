package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Account Profile API Response — GET /functions/v1/account-profile
 */

@Serializable
data class AccountProfileResponse(
    @SerialName("schema_version")
    val schemaVersion: String? = null,
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("account")
    val account: Account? = null,
    @SerialName("profile")
    val profile: Profile,
    @SerialName("demographics")
    val demographics: Demographics? = null,
    @SerialName("verification")
    val verification: Verification? = null,
    @SerialName("emergency_contacts")
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    @SerialName("terms")
    val terms: Terms? = null,
    @SerialName("membership")
    val membership: Membership? = null,
    @SerialName("onboarding")
    val onboarding: Onboarding? = null,
    @SerialName("context")
    val context: Context? = null
)

@Serializable
data class Account(
    @SerialName("user_id")
    val userId: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("email_verified")
    val emailVerified: Boolean = false,
    @SerialName("phone_e164")
    val phoneE164: String? = null,
    @SerialName("phone_verified")
    val phoneVerified: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("last_sign_in_at")
    val lastSignInAt: String? = null
)

@Serializable
data class Profile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("legal_first_name")
    val legalFirstName: String? = null,
    @SerialName("legal_last_name")
    val legalLastName: String? = null,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @SerialName("signed_avatar_url")
    val signedAvatarUrl: String? = null,
    @SerialName("signed_avatar_url_expires_in_seconds")
    val signedAvatarUrlExpiresInSeconds: Int? = null,
    @SerialName("is_minor")
    val isMinor: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Demographics(
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerialName("gender_identity")
    val genderIdentity: String? = null,
    @SerialName("gender_identity_text")
    val genderIdentityText: String? = null,
    @SerialName("race_ethnicity")
    val raceEthnicity: List<String>? = null,
    @SerialName("race_ethnicity_text")
    val raceEthnicityText: String? = null,
    @SerialName("is_minor")
    val isMinor: Boolean? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Verification(
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("email_verified")
    val emailVerified: Boolean = false,
    @SerialName("phone_verified")
    val phoneVerified: Boolean = false,
    @SerialName("phone_e164")
    val phoneE164: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
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
    @SerialName("required_version")
    val requiredVersion: String? = null,
    @SerialName("accepted_version")
    val acceptedVersion: String? = null,
    @SerialName("accepted_at")
    val acceptedAt: String? = null,
    @SerialName("complete")
    val complete: Boolean = false
)

@Serializable
data class Membership(
    @SerialName("id")
    val id: String? = null,
    @SerialName("festival_id")
    val festivalId: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("role")
    val role: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("activated_at")
    val activatedAt: String? = null
)

@Serializable
data class Onboarding(
    @SerialName("profile_complete")
    val profileComplete: Boolean = false,
    @SerialName("demographics_complete")
    val demographicsComplete: Boolean = false,
    @SerialName("emergency_contact_complete")
    val emergencyContactComplete: Boolean = false,
    @SerialName("terms_complete")
    val termsComplete: Boolean = false,
    @SerialName("membership_status")
    val membershipStatus: String? = null,
    @SerialName("is_minor")
    val isMinor: Boolean = false
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
    val membershipStatus: String? = null,
    val onboarding: Onboarding? = null,
    val email: String? = null,
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val genderIdentity: String? = null,
    val connectionStatus: String = "Strong Connection",
    val batteryPercentage: Int = 82
)

fun AccountProfileResponse.toProfileSummary(): ProfileSummary {
    return ProfileSummary(
        userId = profile.userId,
        username = profile.username,
        legalFirstName = profile.legalFirstName,
        legalLastName = profile.legalLastName,
        avatarUrl = profile.signedAvatarUrl,
        isMinor = profile.isMinor,
        emergencyContactsCount = emergencyContacts.size,
        emergencyContacts = emergencyContacts,
        termsComplete = terms?.complete ?: false,
        festivalId = context?.festivalId,
        membershipStatus = membership?.status,
        onboarding = onboarding,
        email = account?.email,
        phone = account?.phoneE164,
        dateOfBirth = demographics?.dateOfBirth,
        genderIdentity = demographics?.genderIdentity ?: demographics?.genderIdentityText
    )
}
