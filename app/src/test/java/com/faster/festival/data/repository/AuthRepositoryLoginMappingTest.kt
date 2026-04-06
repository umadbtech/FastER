package com.faster.festival.data.repository

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Response
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.model.SignupRequest
import com.faster.festival.data.model.SignupResponse
import com.faster.festival.data.model.SendOtpRequest
import com.faster.festival.data.model.LoginRequest
import com.faster.festival.data.model.VerifyOtpRequest
import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.model.User
import com.faster.festival.data.model.EnrollFactorResponse
import com.faster.festival.data.model.RefreshTokenRequest
import com.faster.festival.data.model.RefreshTokenResponse
import com.faster.festival.data.local.EncryptedSessionManager

/**
 * Fake implementation of AuthApiService for testing
 * Used to test AuthRepository login response mapping
 */
class FakeAuthApiService(private val loginResponse: Response<LoginResponse>) : AuthApiService {
    override suspend fun signUp(request: SignupRequest): Response<SignupResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun getUser(token: String): Response<User> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun enrollFactor(token: String, body: Map<String, String>): Response<EnrollFactorResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun verifyFactor(token: String, factorId: String, body: Map<String, String>): Response<AuthResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun sendPhoneOtp(request: SendOtpRequest): Response<Unit> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun verifyPhoneOtp(request: VerifyOtpRequest): Response<AuthResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun sendOtp(body: Map<String, String>): Response<Unit> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun verifyOtp(body: Map<String, String>): Response<AuthResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun recover(body: Map<String, String>): Response<Unit> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun updateUser(authorization: String, body: Map<String, String>): Response<AuthResponse> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun logout(authorization: String): Response<Unit> {
        throw NotImplementedError("Not needed for login test")
    }

    override suspend fun login(request: LoginRequest): Response<LoginResponse> = loginResponse

    override suspend fun refreshToken(request: RefreshTokenRequest): Response<RefreshTokenResponse> {
        throw NotImplementedError("Not needed for login test")
    }
}

/**
 * Test helper for AuthRepository login response mapping
 *
 * Note: To enable actual unit tests, add these dependencies to build.gradle.kts:
 *   testImplementation(libs.junit)
 *   androidTestImplementation(libs.androidx.test.ext.junit)
 *   androidTestImplementation(libs.androidx.test.runner)
 *   testImplementation(libs.mockito.core)
 *
 * Then convert the helper functions to @Test methods with @RunWith(AndroidJUnit4::class)
 */
object AuthRepositoryLoginMappingTestHelper {

    /**
     * Test login with invalid credentials (401 error)
     * @param api FakeAuthApiService with 401 response
     * @param sessionManager EncryptedSessionManager to store session
     * @return Result with test outcome
     */
    fun testLoginInvalidCredentials(
        api: AuthApiService,
        sessionManager: EncryptedSessionManager
    ): Result<String> {
        return runBlocking {
            val repo = AuthRepository(api, sessionManager)
            val result = repo.login("test@example.com", "badpass")

            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: ""
                if (msg.contains("Invalid email or password") || msg.contains("Invalid credentials")) {
                    Result.success("✓ Invalid credentials error message is user-friendly")
                } else {
                    Result.failure(Exception("✗ Expected invalid credentials message, got: $msg"))
                }
            } else {
                Result.failure(Exception("✗ Expected login to fail"))
            }
        }
    }

    /**
     * Test login with rate limit (429 error)
     * @param api FakeAuthApiService with 429 response
     * @param sessionManager EncryptedSessionManager to store session
     * @return Result with test outcome
     */
    fun testLoginRateLimit(
        api: AuthApiService,
        sessionManager: EncryptedSessionManager
    ): Result<String> {
        return runBlocking {
            val repo = AuthRepository(api, sessionManager)
            val result = repo.login("test@example.com", "password")

            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: ""
                if (msg.contains("Too many requests")) {
                    Result.success("✓ Rate limit error message is user-friendly")
                } else {
                    Result.failure(Exception("✗ Expected rate limit message, got: $msg"))
                }
            } else {
                Result.failure(Exception("✗ Expected login to fail"))
            }
        }
    }

    /**
     * Helper to create mock response for testing
     */
    fun createErrorResponse(statusCode: Int, errorMessage: String): Response<LoginResponse> {
        val json = "{\"msg\": \"$errorMessage\"}"
        val body = ResponseBody.create("application/json".toMediaType(), json)
        return Response.error(statusCode, body)
    }
}
