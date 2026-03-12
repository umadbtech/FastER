package com.faster.festival.data.repository

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.OnboardingResponse
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveProfileNameRequest
import com.faster.festival.data.model.SaveUsernameRequest
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.remote.OnboardingApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for onboarding-related API calls.
 */
class OnboardingRepository(
    private val onboardingApiService: OnboardingApiService,
    private val sessionManager: EncryptedSessionManager
) {

    /**
     * Maps HTTP error codes to user-friendly error messages
     */
    private fun mapErrorMessage(code: Int, defaultMessage: String = ""): String {
        return when (code) {
            400 -> "Invalid input. Please check your information and try again."
            401 -> "Your session has expired. Please log in again."
            403 -> "You don't have permission to perform this action."
            404 -> "The requested resource was not found."
            409 -> "This information is already taken. Please try another value."
            422 -> "Please check your information and try again."
            429 -> "Too many requests. Please wait a moment and try again."
            500 -> "Server error. Please try again later."
            else -> defaultMessage.ifEmpty { "An error occurred. Please try again." }
        }
    }

    // ...existing code...

    /**
     * Initialize onboarding (idempotent call) and retrieve festival ID.
     * Returns a Result containing the festival_id from the RPC response.
     */
    suspend fun ensureOnboarding(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val response = onboardingApiService.ensureOnboarding(authHeader)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null || body.isEmpty()) {
                        return@withContext Result.failure(Exception("Failed to initialize onboarding"))
                    }

                    // Extract festival_id from the first element in the response array
                    val festivalId = body.firstOrNull()?.festival_id
                    if (festivalId.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("Failed to retrieve festival information"))
                    }

                    Result.success(festivalId)
                } else {
                    val userMessage = mapErrorMessage(response.code())
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Save username.
     */
    suspend fun saveUsername(username: String): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val request = SaveUsernameRequest(username)
                val response = onboardingApiService.saveUsername(authHeader, request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(body)
                } else {
                    val userMessage = when (response.code()) {
                        409 -> "This username is already taken. Please choose another."
                        422 -> "Username must be 3-20 characters, alphanumeric and underscores only."
                        else -> mapErrorMessage(response.code())
                    }
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Save profile name (legal first and last name).
     */
    suspend fun saveProfileName(request: SaveProfileNameRequest): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val response = onboardingApiService.saveProfileName(authHeader, request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(body)
                } else {
                    val userMessage = mapErrorMessage(response.code())
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Save demographics (DOB, race/ethnicity, gender identity).
     */
    suspend fun saveDemographics(request: SaveDemographicsRequest): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val response = onboardingApiService.saveDemographics(authHeader, request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(body)
                } else {
                    val userMessage = mapErrorMessage(response.code())
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Save wristband pairing information.
     */
    suspend fun saveWristband(wristbandCode: String): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val body = mapOf("wristband_code" to wristbandCode)
                val response = onboardingApiService.saveWristband(authHeader, body)

                if (response.isSuccessful) {
                    val respBody = response.body()
                    if (respBody == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(respBody)
                } else {
                    val userMessage = when (response.code()) {
                        404 -> "Wristband not found. Please check the code and try again."
                        409 -> "This wristband is already paired. Please contact support."
                        else -> mapErrorMessage(response.code())
                    }
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Save emergency contact (create/update/delete).
     */
    suspend fun saveEmergencyContact(request: SaveEmergencyContactRequest): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val response = onboardingApiService.saveEmergencyContact(authHeader, request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(body)
                } else {
                    val userMessage = when (response.code()) {
                        400 -> "Please check your contact information and try again."
                        422 -> "Phone number format is invalid. Please enter a valid phone number."
                        else -> mapErrorMessage(response.code())
                    }
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Accept terms and conditions.
     */
    suspend fun acceptTerms(): Result<OnboardingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val body = mapOf("accepted" to true)
                val response = onboardingApiService.acceptTerms(authHeader, body)

                if (response.isSuccessful) {
                    val respBody = response.body()
                    if (respBody == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(respBody)
                } else {
                    val userMessage = mapErrorMessage(response.code())
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }

    /**
     * Get profile summary for current user.
     */
    suspend fun getProfileSummary(): Result<com.faster.festival.data.models.ProfileSummaryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAccessToken() ?: return@withContext Result.failure(Exception("No access token"))
                val authHeader = "Bearer $token"
                val response = onboardingApiService.getProfileSummary(authHeader)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response"))
                    Result.success(body)
                } else {
                    val userMessage = mapErrorMessage(response.code())
                    Result.failure(Exception(userMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error. Please check your connection and try again."))
            }
        }
    }
}
