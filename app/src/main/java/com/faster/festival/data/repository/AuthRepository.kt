package com.faster.festival.data.repository

import android.util.Base64
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.ErrorResponse
import com.faster.festival.data.model.SignupRequest
import com.faster.festival.data.model.User
import com.faster.festival.data.remote.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AuthRepository(
        private val authApiService: AuthApiService,
        private val sessionManager: EncryptedSessionManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun signUp(fullName: String, email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request =
                        SignupRequest(
                                email = email,
                                password = password,
                                data = mapOf("full_name" to fullName)
                        )

                val response = authApiService.signUp(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Map SignupResponse to User
                        val user =
                                body.user
                                        ?: User(
                                                id = body.id,
                                                email = body.email,
                                                emailConfirmedAt = body.emailConfirmedAt,
                                                userMetadata = body.userMetadata
                                        )
                        // Save user ID and email for later verification
                        sessionManager.saveUserID(user.id)
                        sessionManager.saveUserEmail(email)
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Signup successful but no body returned"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse =
                            try {
                                if (errorBody != null)
                                        json.decodeFromString<ErrorResponse>(errorBody)
                                else null
                            } catch (e: Exception) {
                                null
                            }

                    val errorMessage =
                            when (response.code()) {
                                400 -> "Invalid email or weak password. ${errorResponse?.msg ?: ""}"
                                422 -> "Email already exists."
                                else -> errorResponse?.msg
                                                ?: errorResponse?.errorDescription
                                                        ?: "Signup failed: ${response.code()}"
                            }

                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // Network error or other exception
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    suspend fun processMagicLinkCallback(
        accessToken: String,
        refreshToken: String,
        email: String? = null
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Decode JWT to extract user info and email_confirmed_at
                val userInfo = decodeJwtPayload(accessToken)
                val userId = userInfo["sub"] as? String
                val emailFromToken = userInfo["email"] as? String ?: email
                val emailConfirmedAt = userInfo["email_confirmed_at"] as? String

                if (userId == null || emailFromToken == null) {
                    return@withContext Result.failure(
                        Exception("Invalid token: missing user ID or email")
                    )
                }

                // Validate email is confirmed
                if (emailConfirmedAt == null) {
                    return@withContext Result.failure(
                        Exception("Email not confirmed by Supabase")
                    )
                }

                // Save tokens and session data
                sessionManager.saveAccessToken(accessToken)
                sessionManager.saveRefreshToken(refreshToken)
                sessionManager.saveUserID(userId)
                sessionManager.saveUserEmail(emailFromToken)
                sessionManager.setEmailConfirmed(true)

                // Create user object
                val user = User(
                    id = userId,
                    email = emailFromToken,
                    emailConfirmedAt = emailConfirmedAt
                )

                Result.success(user)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to process magic link: ${e.localizedMessage}"))
            }
        }
    }

    private fun decodeJwtPayload(token: String): Map<String, Any> {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return emptyMap()
            }

            val payload = parts[1]
            // Add padding for Base64 URL decode
            val paddedPayload = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decodedBytes = Base64.decode(paddedPayload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            json.decodeFromString<Map<String, Any>>(decodedString)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
