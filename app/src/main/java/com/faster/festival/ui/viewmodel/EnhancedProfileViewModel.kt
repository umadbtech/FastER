package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.ProfileSummary
import com.faster.festival.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Profile Screen
 */
sealed class ProfileState {
    data object Loading : ProfileState()
    data object Idle : ProfileState()
    data class Success(val profile: ProfileSummary) : ProfileState()
    data class Error(val message: String, val code: Int? = null) : ProfileState()
}

/**
 * ViewModel for Profile Screen
 */
class EnhancedProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _fullName = MutableStateFlow("User")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _emergencyContactsCount = MutableStateFlow(0)
    val emergencyContactsCount: StateFlow<Int> = _emergencyContactsCount.asStateFlow()

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    /**
     * Load profile data from API
     */
    fun loadProfile(accessToken: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            profileRepository.loadProfileSummary(accessToken).collect { result ->
                result.onSuccess { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    _fullName.value = profileRepository.getFullName(profile)
                    _emergencyContactsCount.value = profile.emergencyContactsCount
                    _termsAccepted.value = profile.termsComplete
                }.onFailure { error ->
                    val message = when {
                        error.message?.contains("401") == true -> "Unauthorized. Please login again."
                        error.message?.contains("Network") == true -> "Network error. Please check your connection."
                        else -> error.message ?: "Failed to load profile"
                    }
                    val code = when {
                        error.message?.contains("401") == true -> 401
                        else -> null
                    }
                    _profileState.value = ProfileState.Error(message, code)
                }
            }
        }
    }

    /**
     * Retry loading profile
     */
    fun retryLoadProfile(accessToken: String) {
        loadProfile(accessToken)
    }

    /**
     * Handle logout
     */
    fun logout(onLogout: () -> Unit) {
        // Clear state
        _profileState.value = ProfileState.Idle
        _fullName.value = "User"
        _emergencyContactsCount.value = 0
        _termsAccepted.value = false

        // Call logout callback
        onLogout()
    }

    /**
     * Factory for creating instances
     */
    companion object {
        fun createFactory(profileRepository: ProfileRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EnhancedProfileViewModel(profileRepository) as T
                }
            }
        }
    }
}
