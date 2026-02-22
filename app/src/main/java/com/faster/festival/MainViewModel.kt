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
        val isEmailConfirmed = sessionManager.isEmailConfirmed()

        _startDestination.value = if (token != null && isEmailConfirmed) {
            // Session exists and email is confirmed -> Full access to Home
            "home"
        } else {
            // No token or email not confirmed -> Show login (previously signup)
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
