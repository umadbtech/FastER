package com.faster.festival

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.RefreshTokenRequest
import com.faster.festival.data.remote.AuthApiService
import com.faster.festival.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-level ViewModel that determines the start destination.
 *
 * On startup:
 * 1. If no tokens exist → LOGIN
 * 2. If tokens exist but access token is likely expired (>50 min old) →
 *    proactively refresh before navigating to HOME
 * 3. If refresh fails → clear session → LOGIN
 *
 * This prevents the "first request after 1 hour" failure because
 * the token is already fresh by the time HomeScreen loads.
 */
class MainViewModel(
    private val sessionManager: EncryptedSessionManager,
    private val authApiService: AuthApiService
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        // Supabase default token lifetime is 3600s (1 hour).
        // If the token was saved more than 50 minutes ago, refresh it proactively.
        private const val TOKEN_MAX_AGE_MS = 50 * 60 * 1000L // 50 minutes
    }

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        checkSessionAndRefreshIfNeeded()
    }

    private fun checkSessionAndRefreshIfNeeded() {
        val accessToken = sessionManager.getAccessToken()
        val refreshToken = sessionManager.getRefreshToken()

        // No tokens at all → go to login
        if (accessToken == null) {
            Log.d(TAG, "No access token found → LOGIN")
            _startDestination.value = Routes.LOGIN
            return
        }

        // Check token age — if it's fresh enough, go straight to HOME
        val tokenAge = System.currentTimeMillis() - sessionManager.getAccessTokenTimestamp()
        if (tokenAge < TOKEN_MAX_AGE_MS) {
            Log.d(TAG, "Access token is fresh (${tokenAge / 1000}s old) → HOME")
            _startDestination.value = Routes.HOME
            return
        }

        // Token is stale — try to refresh before navigating
        if (refreshToken.isNullOrBlank()) {
            Log.w(TAG, "Access token expired and no refresh token → LOGIN")
            sessionManager.clearSession()
            _startDestination.value = Routes.LOGIN
            return
        }

        Log.d(TAG, "Access token is stale (${tokenAge / 1000}s old), refreshing proactively...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = authApiService.refreshToken(
                    RefreshTokenRequest(refreshToken = refreshToken)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && !body.accessToken.isNullOrBlank()) {
                        sessionManager.saveAccessToken(body.accessToken)
                        body.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                        Log.d(TAG, "✅ Proactive token refresh succeeded → HOME")
                        _startDestination.value = Routes.HOME
                    } else {
                        Log.w(TAG, "❌ Refresh response empty → LOGIN")
                        sessionManager.clearSession()
                        _startDestination.value = Routes.LOGIN
                    }
                } else {
                    Log.w(TAG, "❌ Refresh failed (HTTP ${response.code()}) → LOGIN")
                    sessionManager.clearSession()
                    _startDestination.value = Routes.LOGIN
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh exception: ${e.message}", e)
                // Network error — don't clear session, let the interceptor handle it later.
                // Navigate to HOME and let the request-level retry handle it.
                Log.d(TAG, "Network error during refresh, proceeding to HOME anyway")
                _startDestination.value = Routes.HOME
            }
        }
    }

    class Factory(
        private val sessionManager: EncryptedSessionManager,
        private val authApiService: AuthApiService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(sessionManager, authApiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
