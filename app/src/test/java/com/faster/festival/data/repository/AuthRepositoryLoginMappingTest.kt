package com.faster.festival.data.repository

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.local.EncryptedSessionManager

class FakeAuthApiService(private val response: Response<LoginResponse>) : AuthApiService {
    override suspend fun signUp(request: com.faster.festival.data.model.SignupRequest): Response<com.faster.festival.data.model.SignupResponse> {
        throw NotImplementedError()
    }
    override suspend fun getUser(token: String) = throw NotImplementedError()
    override suspend fun enrollFactor(token: String, body: Map<String, String>) = throw NotImplementedError()
    override suspend fun verifyFactor(token: String, factorId: String, body: Map<String, String>) = throw NotImplementedError()
    override suspend fun sendOtp(body: Map<String, String>) = throw NotImplementedError()
    override suspend fun verifyOtp(body: Map<String, String>) = throw NotImplementedError()
    override suspend fun login(request: com.faster.festival.data.model.LoginRequest): Response<LoginResponse> = response
}

class SimpleSessionManager : EncryptedSessionManagerPlaceholderBase()

class AuthRepositoryLoginMappingTest {

    @Test
    fun login_invalidCredentials_mapsToFriendlyMessage() = runBlocking {
        val json = "{\"msg\": \"Invalid credentials\"}"
        val body = ResponseBody.create("application/json".toMediaType(), json)
        val resp = Response.error<LoginResponse>(401, body)
        val api = FakeAuthApiService(resp)
        val repo = AuthRepository(api, SimpleSessionManager())

        val result = repo.login("test@example.com", "badpass")
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("Invalid email or password") || msg.contains("Invalid credentials"))
    }

    @Test
    fun login_rateLimit_mapsToFriendlyMessage() = runBlocking {
        val json = "{\"msg\": \"Rate limit\"}"
        val body = ResponseBody.create("application/json".toMediaType(), json)
        val resp = Response.error<LoginResponse>(429, body)
        val api = FakeAuthApiService(resp)
        val repo = AuthRepository(api, SimpleSessionManager())

        val result = repo.login("test@example.com", "badpass")
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("Too many requests"))
    }
}
