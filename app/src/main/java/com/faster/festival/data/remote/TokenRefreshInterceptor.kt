package com.faster.festival.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import android.util.Log

/**
 * Interceptor that automatically refreshes expired access tokens
 * using the refresh token before retrying the request
 *
 * Flow:
 * 1. Make original request with current access token
 * 2. If response is 401 or contains "JWT expired":
 *    a. Use refresh token to get new access token
 *    b. Save new tokens
 *    c. Retry original request with new token
 * 3. If refresh fails, clear session (invalid refresh token)
 */
class TokenRefreshInterceptor(
    private val sessionManager: EncryptedSessionManager,
    private val getAuthApiService: () -> AuthApiService
) : Interceptor {

    private val lock = Any()
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Attempt 1: Make request with current token
        val response = chain.proceed(originalRequest)

        // Check if token refresh is needed:
        // 1. HTTP 401 (standard auth failure)
        // 2. Non-success response with "JWT expired" / "Invalid JWT" in body
        //    (Supabase Edge Functions may return 500/400 with JWT errors)
        val needsRefresh = when {
            response.code == 401 -> true
            !response.isSuccessful -> {
                val bodyString = peekResponseBody(response)
                bodyString != null && (
                    bodyString.contains("JWT expired", ignoreCase = true) ||
                    bodyString.contains("Invalid JWT", ignoreCase = true) ||
                    bodyString.contains("token is expired", ignoreCase = true)
                )
            }
            else -> false
        }

        if (needsRefresh) {
            Log.d("TokenRefresh", "Token refresh needed (HTTP ${response.code})")
            synchronized(lock) {
                if (!isRefreshing) {
                    isRefreshing = true
                    try {
                        val refreshToken = sessionManager.getRefreshToken()
                        if (!refreshToken.isNullOrBlank()) {
                            val refreshSuccess = runBlocking {
                                refreshAccessToken(refreshToken)
                            }

                            if (refreshSuccess) {
                                isRefreshing = false
                                val newToken = sessionManager.getAccessToken()
                                if (!newToken.isNullOrBlank()) {
                                    response.close()

                                    val retryRequest = originalRequest.newBuilder()
                                        .header("Authorization", "Bearer $newToken")
                                        .build()

                                    Log.d("TokenRefresh", "Retrying request with refreshed token")
                                    return chain.proceed(retryRequest)
                                }
                            }
                        }
                    } finally {
                        isRefreshing = false
                    }
                }
            }
        }

        return response
    }

    /**
     * Peek at the response body without consuming it.
     * Returns the body string (up to 4KB) or null if body can't be read.
     */
    private fun peekResponseBody(response: Response): String? {
        return try {
            val source = response.body?.source() ?: return null
            source.request(4096)
            source.buffer.clone().readUtf8(minOf(4096, source.buffer.size))
        } catch (e: Exception) {
            Log.w("TokenRefresh", "Failed to peek response body: ${e.message}")
            null
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): Boolean {
        return try {
            Log.d("TokenRefresh", "Attempting to refresh access token...")
            val request = RefreshTokenRequest(refreshToken = refreshToken)
            // ✅ Get authApiService lazily when actually needed
            val authApiService = getAuthApiService()
            val response = authApiService.refreshToken(request)

            if (response.isSuccessful) {
                val newTokens = response.body()
                if (newTokens != null && !newTokens.accessToken.isNullOrBlank()) {
                    Log.d("TokenRefresh", "✅ Token refreshed successfully")
                    sessionManager.saveAccessToken(newTokens.accessToken)
                    newTokens.refreshToken?.let {
                        sessionManager.saveRefreshToken(it)
                    }
                    true
                } else {
                    Log.w("TokenRefresh", "❌ Refresh response body is null or empty")
                    sessionManager.clearSession()
                    false
                }
            } else {
                // Refresh failed - token is invalid
                Log.w("TokenRefresh", "❌ Token refresh failed: ${response.code()}")
                sessionManager.clearSession()
                false
            }
        } catch (e: Exception) {
            Log.e("TokenRefresh", "❌ Token refresh exception: ${e.message}", e)
            sessionManager.clearSession()
            false
        }
    }
}

