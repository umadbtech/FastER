package com.faster.festival.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import android.util.Log

/**
 * OkHttp interceptor that automatically refreshes expired Supabase access tokens.
 *
 * Flow:
 * 1. Original request proceeds with current access token
 * 2. If response indicates token expiry (401, "JWT expired", etc.):
 *    a. Acquire lock — only ONE thread performs the actual refresh
 *    b. Other threads wait, then retry with the new token
 *    c. If refresh fails, clear session (forces re-login)
 * 3. Retry the original request exactly ONCE with the new token
 *
 * Thread safety:
 * Uses a monitor lock so concurrent requests that all hit 401 don't
 * fire multiple refresh calls. The first thread refreshes; others wait
 * and pick up the new token from SessionManager.
 */
class TokenRefreshInterceptor(
    private val sessionManager: EncryptedSessionManager,
    private val getAuthApiService: () -> AuthApiService
) : Interceptor {

    companion object {
        private const val TAG = "TokenRefresh"
    }

    // Shared lock — all interceptor instances on the same OkHttp client share this
    private val refreshLock = Object()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Remember which token we sent so we can detect if another thread already refreshed
        val tokenUsed = originalRequest.header("Authorization")

        // Attempt 1: make request with current token
        val response = chain.proceed(originalRequest)

        if (!needsTokenRefresh(response)) {
            return response
        }

        Log.d(TAG, "⏳ Token refresh needed (HTTP ${response.code}) for ${originalRequest.url.encodedPath}")

        // Enter the refresh critical section
        synchronized(refreshLock) {
            // Check if another thread already refreshed while we were waiting
            val currentToken = sessionManager.getAccessToken()
            val currentAuthHeader = if (!currentToken.isNullOrBlank()) "Bearer $currentToken" else null

            if (currentAuthHeader != null && currentAuthHeader != tokenUsed) {
                // Another thread already refreshed — just retry with the new token
                Log.d(TAG, "✅ Token was already refreshed by another thread, retrying")
                response.close()
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", currentAuthHeader)
                    .build()
                return chain.proceed(retryRequest)
            }

            // We're the first thread — perform the refresh
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "❌ No refresh token available — cannot refresh")
                sessionManager.clearSession()
                return response
            }

            val refreshSuccess = runBlocking {
                performRefresh(refreshToken)
            }

            if (!refreshSuccess) {
                Log.w(TAG, "❌ Refresh failed — session cleared, user must re-login")
                // Session already cleared inside performRefresh on failure
                return response
            }
        }

        // Refresh succeeded — retry original request with new token
        val newToken = sessionManager.getAccessToken()
        if (newToken.isNullOrBlank()) {
            Log.w(TAG, "❌ Access token is null after successful refresh")
            return response
        }

        response.close()
        val retryRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()

        Log.d(TAG, "🔄 Retrying ${originalRequest.url.encodedPath} with refreshed token")
        return chain.proceed(retryRequest)
    }

    /**
     * Check whether the response indicates the access token has expired.
     * Supabase can return:
     * - HTTP 401 (standard)
     * - HTTP 500/400 with "JWT expired" in the body (Edge Functions)
     */
    private fun needsTokenRefresh(response: Response): Boolean {
        if (response.code == 401) return true
        if (response.isSuccessful) return false

        val bodyString = peekResponseBody(response) ?: return false
        return bodyString.contains("JWT expired", ignoreCase = true) ||
                bodyString.contains("Invalid JWT", ignoreCase = true) ||
                bodyString.contains("token is expired", ignoreCase = true)
    }

    /**
     * Peek at the response body without consuming it (up to 4KB).
     */
    private fun peekResponseBody(response: Response): String? {
        return try {
            val source = response.body?.source() ?: return null
            source.request(4096)
            source.buffer.clone().readUtf8(minOf(4096, source.buffer.size))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to peek response body: ${e.message}")
            null
        }
    }

    /**
     * Call Supabase refresh endpoint. Returns true on success.
     * On failure, clears the session so the user is redirected to login.
     */
    private suspend fun performRefresh(refreshToken: String): Boolean {
        return try {
            Log.d(TAG, "🔑 Calling Supabase refresh endpoint...")
            val request = RefreshTokenRequest(refreshToken = refreshToken)
            val authApiService = getAuthApiService()
            val response = authApiService.refreshToken(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && !body.accessToken.isNullOrBlank()) {
                    sessionManager.saveAccessToken(body.accessToken)
                    body.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                    Log.d(TAG, "✅ Token refreshed successfully (expires_in=${body.expiresIn}s)")
                    true
                } else {
                    Log.w(TAG, "❌ Refresh response body is null or has no access token")
                    sessionManager.clearSession()
                    false
                }
            } else {
                Log.w(TAG, "❌ Refresh endpoint returned HTTP ${response.code()}")
                sessionManager.clearSession()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Refresh exception: ${e.message}", e)
            sessionManager.clearSession()
            false
        }
    }
}
