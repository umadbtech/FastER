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
    private val authApiService: AuthApiService
) : Interceptor {

    private val lock = Any()
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Attempt 1: Make request with current token
        val response = chain.proceed(originalRequest)

        // Check if token is expired (401 status code indicates authentication issue)
        if (response.code == 401) {
            synchronized(lock) {
                // Try to refresh token
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
                                // Token refreshed successfully, retry original request
                                val newToken = sessionManager.getAccessToken()
                                if (!newToken.isNullOrBlank()) {
                                    // Close the failed response
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

    private suspend fun refreshAccessToken(refreshToken: String): Boolean {
        return try {
            Log.d("TokenRefresh", "Attempting to refresh access token...")
            val request = RefreshTokenRequest(refreshToken = refreshToken)
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

