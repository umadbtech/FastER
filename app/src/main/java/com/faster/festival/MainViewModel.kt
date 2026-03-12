package com.faster.festival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.ui.navigation.Routes
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
        _startDestination.value = if (token != null) {
            Routes.HOME
        } else {
            Routes.LOGIN
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
