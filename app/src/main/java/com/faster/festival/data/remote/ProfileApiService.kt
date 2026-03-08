package com.faster.festival.data.remote

import com.faster.festival.data.models.ProfileSummaryResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/** Retrofit API service for Profile endpoints */
interface ProfileApiService {

    /** GET /functions/v1/profile-summary Load user profile summary with all related data */
    @GET("functions/v1/profile-summary")
    suspend fun getProfileSummary(
            @Header("Authorization") authorization: String
    ): ProfileSummaryResponse

    /** POST /functions/v1/save-profile-name Save user's legal first and last name */
    @POST("functions/v1/save-profile-name")
    suspend fun saveLegalName(
            @Header("Authorization") authorization: String,
            @Body request: SaveLegalNameRequest
    ): Response<ProfileSummaryResponse>

    /** POST /functions/v1/update-onboarding-demographics Save user's demographics */
    @POST("functions/v1/update-onboarding-demographics")
    suspend fun saveDemographics(
            @Header("Authorization") authorization: String,
            @Body request: com.faster.festival.data.model.SaveDemographicsRequest
    ): Response<com.faster.festival.data.model.OnboardingResponse>

    /** POST /functions/v1/upload-avatar Upload profile avatar image (multipart/form-data) */
    @Multipart
    @POST("functions/v1/upload-avatar")
    suspend fun uploadAvatar(
            @Header("Authorization") authorization: String,
            @Part file: MultipartBody.Part
    ): Response<UploadAvatarResponse>

    /** GET /functions/v1/avatar-url Get signed avatar URL for current user */
    @GET("functions/v1/avatar-url")
    suspend fun getAvatarUrl(
            @Header("Authorization") authorization: String
    ): Response<AvatarUrlResponse>

    /** POST /functions/v1/save-emergency-contact Add or update emergency contact */
    @POST("functions/v1/save-emergency-contact")
    suspend fun saveEmergencyContact(
            @Header("Authorization") authorization: String,
            @Body request: SaveEmergencyContactRequest
    ): Response<ProfileSummaryResponse>

    /** DELETE /functions/v1/emergency-contact/{contactId} Delete emergency contact by ID */
    @DELETE("functions/v1/emergency-contact/{contactId}")
    suspend fun deleteEmergencyContact(
            @Header("Authorization") authorization: String,
            @Path("contactId") contactId: String
    ): Response<ProfileSummaryResponse>
}

/** Request/Response models for profile endpoints */
@kotlinx.serialization.Serializable
data class SaveLegalNameRequest(
        @kotlinx.serialization.SerialName("legal_first_name") val legalFirstName: String,
        @kotlinx.serialization.SerialName("legal_last_name") val legalLastName: String
)

@kotlinx.serialization.Serializable
data class UploadAvatarResponse(
        @kotlinx.serialization.SerialName("ok") val ok: Boolean,
        @kotlinx.serialization.SerialName("signed_url") val signedUrl: String? = null,
        @kotlinx.serialization.SerialName("avatar_path") val avatarPath: String? = null
)

@kotlinx.serialization.Serializable
data class AvatarUrlResponse(
        @kotlinx.serialization.SerialName("ok") val ok: Boolean,
        @kotlinx.serialization.SerialName("signed_url") val signedUrl: String? = null,
        @kotlinx.serialization.SerialName("expires_in_seconds") val expiresInSeconds: Int? = null
)

@kotlinx.serialization.Serializable
data class SaveEmergencyContactRequest(
        @kotlinx.serialization.SerialName("external_name") val name: String,
        @kotlinx.serialization.SerialName("external_phone_e164") val phone: String,
        @kotlinx.serialization.SerialName("relationship") val relationship: String? = null,
        @kotlinx.serialization.SerialName("is_primary") val isPrimary: Boolean = false,
        @kotlinx.serialization.SerialName("contact_id") val contactId: String? = null // For updates
)
