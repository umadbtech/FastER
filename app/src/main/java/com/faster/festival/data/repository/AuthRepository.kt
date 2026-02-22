package com.faster.festival.data.repository

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.model.ErrorResponse
import com.faster.festival.data.model.SignupRequest
import com.faster.festival.data.model.User
import com.faster.festival.data.model.LoginRequest
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.remote.AuthApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AuthRepository(
    private val authApiService: AuthApiService,
    private val sessionManager: EncryptedSessionManager
) : AuthRepositoryContract {
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
                    // Map SignupResponse to User
                    val user = body?.user ?: User(id = body?.id ?: "", email = body?.email, emailConfirmedAt = body?.emailConfirmedAt)
                    // Save user ID and email for later verification
                    sessionManager.saveUserID(user.id)
                    sessionManager.saveUserEmail(email)
                    Result.success(user)
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

    /**
     * Reuse the Supabase signup endpoint to trigger a resend of the verification email/OTP.
     * This method constructs a SignupRequest with only the email (password left blank)
     * to call the same /auth/v1/signup endpoint. It returns a Result<Unit> like sendOtp.
     */
    suspend fun resendSignup(email: String, fullName: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Reuse the SignupRequest model. Password is intentionally left blank for resend.
                val request = SignupRequest(email = email, password = "", data = fullName?.let { mapOf("full_name" to it) })
                val response = authApiService.signUp(request)

                if (response.isSuccessful) {
                    Result.success(Unit)
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

                    val message = when (response.code()) {
                        400 -> "Invalid email. ${errorResponse?.msg ?: ""}"
                        422 -> "Email already exists."
                        429 -> "Too many requests. Please try again later."
                        else -> errorResponse?.msg
                                        ?: errorResponse?.errorDescription
                                                ?: "Resend failed: ${response.code()}"
                    }

                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Login with email/password using Supabase password grant. Persist tokens and user on success.
     */
    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email = email, password = password)
                val response = authApiService.login(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        return@withContext Result.failure(Exception("Empty response from server"))
                    }

                    // Persist tokens and user safely
                    body.accessToken?.let { sessionManager.saveAccessToken(it) }
                    body.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                    body.user?.let { user ->
                        sessionManager.saveUserID(user.id)
                        user.email?.let { sessionManager.saveUserEmail(it) }
                    }

                    Result.success(body)
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

                    val message = when (response.code()) {
                        400, 401 -> "Invalid email or password. ${errorResponse?.msg ?: ""}"
                        429 -> "Too many requests. Please try again later."
                        else -> errorResponse?.msg
                                        ?: errorResponse?.errorDescription
                                                ?: "Login failed: ${response.code()}"
                    }

                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Send a one-time 6-digit OTP to the user's email via backend endpoint.
     */
    suspend fun sendOtp(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.sendOtp(mapOf("email" to email))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "Failed to send OTP: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Call the backend verify endpoint and return the full AuthResponse (tokens + user).
     * Does not persist anything; persistence is performed explicitly by the caller via persistSession().
     */
    suspend fun verifyOtp(email: String, code: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "email" to email,
                    "token" to code,
                    "type" to "signup"
                )
                val response = authApiService.verifyOtp(payload)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        return@withContext Result.failure(Exception("Empty response from server"))
                    }

                    Result.success(body)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "OTP verification failed: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Persist session tokens and user info coming from AuthResponse in a secure storage.
     */
    fun persistSession(authResponse: AuthResponse) {
        val accessToken = authResponse.accessToken
        val refreshToken = authResponse.refreshToken
        val user = authResponse.user

        if (!accessToken.isNullOrEmpty()) sessionManager.saveAccessToken(accessToken)
        if (!refreshToken.isNullOrEmpty()) sessionManager.saveRefreshToken(refreshToken)
        if (user != null) {
            sessionManager.saveUserID(user.id)
            user.email?.let { sessionManager.saveUserEmail(it) }
            user.phone?.let { sessionManager.saveUserPhone(it) }
            // If email is confirmed/verified, mark it in session manager
            val metadataVerified = user.userMetadata?.get("email_verified")
            val isVerified = metadataVerified?.toBoolean() ?: false
            sessionManager.setEmailConfirmed(isVerified)
        }
    }

    // Return saved email if available (used to pre-populate login form)
    override fun getSavedEmail(): String? = sessionManager.getUserEmail()

    /**
     * Request a password recovery email
     */
    suspend fun requestPasswordReset(email: String, redirectTo: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = mutableMapOf<String, String>("email" to email)
                if (!redirectTo.isNullOrBlank()) body["redirect_to"] = redirectTo
                val response = authApiService.recover(body)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "Password reset request failed: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Verify recovery OTP/token and return AuthResponse (tokens + user)
     */
    suspend fun verifyRecoveryOtp(email: String, token: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = mapOf("email" to email, "token" to token, "type" to "recovery")
                val response = authApiService.verifyOtp(payload)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response from server"))
                    Result.success(body)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "OTP verification failed: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Update password using access token obtained from verifyRecoveryOtp (Authorization: Bearer <token>)
     */
    suspend fun updatePassword(accessToken: String, newPassword: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val header = "Bearer $accessToken"
                val body = mapOf("password" to newPassword)
                val response = authApiService.updateUser(header, body)
                if (response.isSuccessful) {
                    val respBody = response.body()
                    if (respBody == null) return@withContext Result.failure(Exception("Empty response from server"))
                    // Persist new session if tokens provided
                    persistSession(respBody)
                    Result.success(respBody)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "Update password failed: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Send OTP to phone using Supabase auth/v1/otp endpoint.
     */
    suspend fun sendPhoneOtp(phone: String, createUser: Boolean = true): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.sendPhoneOtp(com.faster.festival.data.model.SendOtpRequest(phone = phone, createUser = createUser))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "Failed to send OTP: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Verify phone OTP (type = "sms") and return AuthResponse (tokens + user)
     */
    suspend fun verifyPhoneOtp(phone: String, token: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.faster.festival.data.model.VerifyOtpRequest(phone = phone, token = token, type = "sms")
                val response = authApiService.verifyPhoneOtp(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) return@withContext Result.failure(Exception("Empty response from server"))
                    // Persist session tokens
                    persistSession(body)
                    Result.success(body)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = errorBody ?: "OTP verification failed: ${response.code()}"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Send OTP to phone using Supabase auth/v1/otp endpoint.
     * This wrapper maps Supabase error responses into typed exceptions to allow UI to react.
     */
    suspend fun sendPhoneOtpWithErrorMapping(phone: String, createUser: Boolean = true): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.sendPhoneOtp(com.faster.festival.data.model.SendOtpRequest(phone = phone, createUser = createUser))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = try {
                        if (!errorBody.isNullOrBlank()) json.decodeFromString<ErrorResponse>(errorBody) else null
                    } catch (e: Exception) { null }

                    // Map based on HTTP code + error_code (if present) or message
                    when {
                        response.code() == 422 && (errorResponse?.error?.contains("sms_send_failed", true) == true || errorResponse?.msg?.contains("sms_send_failed", true) == true || errorResponse?.errorCode == "sms_send_failed") -> {
                            Result.failure(SmsSendFailedException(errorResponse.msg ?: "SMS delivery failed. Try again or use email signup."))
                        }
                        response.code() == 422 && (errorResponse?.errorCode == "invalid_phone" || errorResponse?.msg?.contains("invalid phone", true) == true) -> {
                            Result.failure(PhoneValidationException(errorResponse.msg ?: "Please check your phone number and try again."))
                        }
                        response.code() == 422 && (errorResponse?.msg?.contains("already in use", true) == true || errorResponse?.msg?.contains("already exists", true) == true) -> {
                            Result.failure(PhoneValidationException(errorResponse.msg ?: "This phone number is already in use"))
                        }
                        response.code() == 429 || (errorResponse?.errorCode == "rate_limited" || errorResponse?.msg?.contains("rate limit", true) == true) -> {
                            // try to parse retry information from msg if present
                            val retryAfter = try {
                                // crude: look for digits in the message
                                Regex("(\\d+)").find(errorResponse?.msg ?: "")?.groupValues?.get(1)?.toIntOrNull()
                            } catch (e: Exception) { null }
                            Result.failure(RateLimitException(errorResponse?.msg ?: "Too many attempts. Wait 60 seconds before retrying.", retryAfter))
                        }
                        response.code() == 422 -> {
                            Result.failure(GenericAuthException(errorResponse?.msg ?: "Verification unavailable. Try email signup."))
                        }
                        else -> {
                            Result.failure(GenericAuthException(errorResponse?.msg ?: "Failed to send OTP: ${response.code()}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.localizedMessage}"))
            }
        }
    }

    companion object {
        // Expose login error mapping to allow unit testing without creating the full repository.
        fun mapLoginError(code: Int, errorBody: String?): String {
            // Try to decode JSON-like bodies roughly for a friendly message; fall back to defaults
            val msgFromBody = try {
                if (errorBody.isNullOrBlank()) null
                else {
                    // crude extraction: look for "msg" or "error_description" or "error"
                    val lower = errorBody.lowercase()
                    when {
                        "msg" in lower -> Regex("\"msg\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)?.groupValues?.get(1)
                        "error_description" in lower -> Regex("\"error_description\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)?.groupValues?.get(1)
                        "error" in lower -> Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)?.groupValues?.get(1)
                        else -> null
                    }
                }
            } catch (e: Exception) {
                null
            }

            return when (code) {
                400, 401 -> "Invalid email or password. ${msgFromBody ?: ""}".trim()
                429 -> "Too many requests. Please try again later."
                else -> msgFromBody ?: "Login failed: $code"
            }
        }
    }
}
