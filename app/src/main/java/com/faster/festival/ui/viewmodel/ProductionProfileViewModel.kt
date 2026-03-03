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
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Error type classification for Profile API failures
 */
sealed class ProfileErrorType {
    data object NoInternet : ProfileErrorType()
    data class ServerError(val code: Int) : ProfileErrorType()
    data class ClientError(val code: Int) : ProfileErrorType()
    data class Timeout(val message: String = "Request timed out") : ProfileErrorType()
    data class Unknown(val message: String) : ProfileErrorType()
}

/**
 * UI State for Profile Screen - Production-grade with single StateFlow
 */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val profile: ProfileSummary) : ProfileUiState()
    data class Error(
        val errorType: ProfileErrorType,
        val message: String,
        val retryAction: () -> Unit
    ) : ProfileUiState()
    data object Empty : ProfileUiState()
}

/**
 * Production-grade ViewModel for Profile Screen
 * - NO hardcoded/fake data
 * - NO default values for API data
 * - Real API calls only via ProfileRepository
 * - Explicit error type handling
 * - Stateless retry logic
 */
class ProductionProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val accessToken: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Load profile from API
     * - Called on init
     * - Called on retry
     * - Called on refresh (pull-to-refresh)
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            profileRepository.loadProfileSummary(accessToken).collect { result ->
                result.onSuccess { profile ->
                    if (profile.isValid()) {
                        _uiState.value = ProfileUiState.Success(profile)
                    } else {
                        _uiState.value = ProfileUiState.Empty
                    }
                }.onFailure { error ->
                    val (errorType, message) = classifyError(error)
                    _uiState.value = ProfileUiState.Error(
                        errorType = errorType,
                        message = message,
                        retryAction = { loadProfile() }  // Stateless retry
                    )
                }
            }
        }
    }

    /**
     * Refresh profile (called from pull-to-refresh)
     */
    fun refreshProfile() {
        loadProfile()
    }

    /**
     * Classify network error into specific types for UI messaging
     */
    private fun classifyError(error: Throwable): Pair<ProfileErrorType, String> {
        return when (error) {
            is IOException -> {
                ProfileErrorType.NoInternet to
                    "No internet connection. Check your network and retry."
            }
            is TimeoutException -> {
                ProfileErrorType.Timeout("Request timed out") to
                    "Request timed out. Please retry."
            }
            is HttpException -> {
                when {
                    error.code() >= 500 -> {
                        ProfileErrorType.ServerError(error.code()) to
                            "Server error. Please try again later."
                    }
                    error.code() == 401 -> {
                        ProfileErrorType.ClientError(error.code()) to
                            "Session expired. Please log in again."
                    }
                    error.code() == 404 -> {
                        ProfileErrorType.ClientError(error.code()) to
                            "Profile not found."
                    }
                    error.code() >= 400 -> {
                        ProfileErrorType.ClientError(error.code()) to
                            "Invalid request. Please try again."
                    }
                    else -> {
                        ProfileErrorType.Unknown(error.message ?: "Unknown") to
                            "Something went wrong. Please try again."
                    }
                }
            }
            else -> {
                ProfileErrorType.Unknown(error.message ?: "Unknown") to
                    "Something went wrong. Please try again."
            }
        }
    }

    /**
     * Factory for ViewModel creation
     */
    companion object {
        fun createFactory(
            profileRepository: ProfileRepository,
            accessToken: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductionProfileViewModel(profileRepository, accessToken) as T
                }
            }
        }
    }
}

/**
 * Extension function to validate profile data
 */
private fun ProfileSummary.isValid(): Boolean {
    return userId.isNotBlank()
}
