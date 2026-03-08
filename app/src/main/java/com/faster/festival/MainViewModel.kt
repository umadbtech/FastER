package com.faster.festival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faster.festival.data.local.EncryptedSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val sessionManager: EncryptedSessionManager) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        val token = sessionManager.getAccessToken()

        // ✅ FIXED: If token exists, user is authenticated → go to Home
        // Email confirmation is handled within the onboarding flow if needed
        // This prevents users from getting stuck after successful authentication
        _startDestination.value = if (token != null) {
            "home"
        } else {
            "login"
        }
    }

    class Factory(private val sessionManager: EncryptedSessionManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
