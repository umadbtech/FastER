package com.faster.festival.data.repository

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.OnboardingResponse
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveUsernameRequest
import com.faster.festival.data.model.EnsureOnboardingResponse
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.model.EnsureFestivalOnboardingResponse
import com.faster.festival.data.remote.OnboardingApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Repository for onboarding-related API calls.
 */
class OnboardingRepository(
    private val onboardingApiService: OnboardingApiService,
    private val sessionManager: EncryptedSessionManager
) {
    private val json = Json { ignoreUnknownKeys = true }

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
                        return@withContext Result.failure(Exception("Empty response from ensure_festival_onboarding"))
                    }

                    // Extract festival_id from the first element in the response array
                    val festivalId = body.firstOrNull()?.festival_id
                    if (festivalId.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("No festival_id in response"))
                    }

                    Result.success(festivalId)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ensure onboarding failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Save username failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Save demographics failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Save wristband failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
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
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Save emergency contact failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }
}
