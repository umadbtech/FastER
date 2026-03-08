package com.faster.festival.data.repository

import android.util.Log
import com.faster.festival.data.models.ProfileSummary
import com.faster.festival.data.models.toProfileSummary
import com.faster.festival.data.remote.ProfileApiService
import com.faster.festival.data.remote.SaveEmergencyContactRequest
import com.faster.festival.data.remote.SaveLegalNameRequest
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/** Repository for Profile-related API calls */
class ProfileRepository(private val profileApiService: ProfileApiService) {

    /** Load profile summary from API */
    fun loadProfileSummary(accessToken: String): Flow<Result<ProfileSummary>> = flow {
        try {
            val response =
                    profileApiService.getProfileSummary(authorization = "Bearer $accessToken")

            if (response.ok) {
                val profileSummary = response.toProfileSummary()
                emit(Result.success(profileSummary))
            } else {
                emit(Result.failure(Exception("Profile response not ok")))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error loading profile summary: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Save legal first and last name */
    fun saveLegalName(
            firstName: String,
            lastName: String,
            accessToken: String
    ): Flow<Result<ProfileSummary>> = flow {
        try {
            val request = SaveLegalNameRequest(firstName, lastName)
            val response =
                    profileApiService.saveLegalName(
                            authorization = "Bearer $accessToken",
                            request = request
                    )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok) {
                    val profileSummary = body.toProfileSummary()
                    emit(Result.success(profileSummary))
                } else {
                    emit(Result.failure(Exception("Save legal name failed")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving legal name: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Save demographics (DOB, gender identity, race/ethnicity) */
    fun saveDemographics(
            dateOfBirth: String?,
            genderIdentity: String?,
            raceEthnicity: List<String>?,
            accessToken: String
    ): Flow<Result<Unit>> = flow {
        try {
            val request =
                    com.faster.festival.data.model.SaveDemographicsRequest(
                            dob = dateOfBirth, // <-- FIXED: was date_of_birth
                            gender_identity = genderIdentity,
                            race_ethnicity = raceEthnicity
                    )
            val response =
                    profileApiService.saveDemographics(
                            authorization = "Bearer $accessToken",
                            request = request
                    )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.saved == true) {
                    emit(Result.success(Unit))
                } else {
                    emit(Result.failure(Exception("Save demographics failed")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving demographics: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Upload profile avatar image */
    fun uploadAvatar(imageFile: File, accessToken: String): Flow<Result<String>> = flow {
        try {
            val requestBody = imageFile.asRequestBody("image/*".toMediaType())
            val part = MultipartBody.Part.createFormData("avatar", imageFile.name, requestBody)

            val response =
                    profileApiService.uploadAvatar(
                            authorization = "Bearer $accessToken",
                            file = part
                    )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok && !body.signedUrl.isNullOrBlank()) {
                    emit(Result.success(body.signedUrl))
                } else {
                    emit(Result.failure(Exception("No signed URL in response")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error uploading avatar: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Get signed avatar URL */
    fun getAvatarUrl(accessToken: String): Flow<Result<String>> = flow {
        try {
            val response = profileApiService.getAvatarUrl(authorization = "Bearer $accessToken")

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok && !body.signedUrl.isNullOrBlank()) {
                    emit(Result.success(body.signedUrl))
                } else {
                    emit(Result.failure(Exception("No signed URL in response")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error getting avatar URL: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Save (add or update) emergency contact */
    fun saveEmergencyContact(
            name: String,
            phone: String,
            relationship: String? = null,
            isPrimary: Boolean = false,
            contactId: String? = null,
            accessToken: String
    ): Flow<Result<ProfileSummary>> = flow {
        try {
            val request =
                    SaveEmergencyContactRequest(
                            name = name,
                            phone = phone,
                            relationship = relationship,
                            isPrimary = isPrimary,
                            contactId = contactId
                    )

            val response =
                    profileApiService.saveEmergencyContact(
                            authorization = "Bearer $accessToken",
                            request = request
                    )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok) {
                    val profileSummary = body.toProfileSummary()
                    emit(Result.success(profileSummary))
                } else {
                    emit(Result.failure(Exception("Save emergency contact failed")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error saving emergency contact: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Delete emergency contact by ID */
    fun deleteEmergencyContact(
            contactId: String,
            accessToken: String
    ): Flow<Result<ProfileSummary>> = flow {
        try {
            val response =
                    profileApiService.deleteEmergencyContact(
                            authorization = "Bearer $accessToken",
                            contactId = contactId
                    )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok) {
                    val profileSummary = body.toProfileSummary()
                    emit(Result.success(profileSummary))
                } else {
                    emit(Result.failure(Exception("Delete emergency contact failed")))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error ${response.code()}"
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error deleting emergency contact: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /** Check if user has completed terms */
    fun hasCompletedTerms(profileSummary: ProfileSummary): Boolean {
        return profileSummary.termsComplete
    }

    /** Derive full name from profile */
    fun getFullName(profileSummary: ProfileSummary): String {
        return when {
            profileSummary.legalFirstName != null && profileSummary.legalLastName != null ->
                    "${profileSummary.legalFirstName} ${profileSummary.legalLastName}"
            profileSummary.legalFirstName != null -> profileSummary.legalFirstName
            profileSummary.username != null -> profileSummary.username
            else -> "User"
        }
    }
}
