package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.models.Result
import com.faster.festival.data.repository.FestivalHeaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Festival Header
 */
sealed class FestivalHeaderUiState {
    object Loading : FestivalHeaderUiState()
    data class Success(val festival: FestivalHeader) : FestivalHeaderUiState()
    data class Error(val message: String, val code: Int? = null) : FestivalHeaderUiState()
}

/**
 * ViewModel for Festival Header screen
 * Manages loading, caching, and error handling for festival header data
 */
class FestivalHeaderViewModel(
    private val repository: FestivalHeaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FestivalHeaderUiState>(FestivalHeaderUiState.Loading)
    val uiState: StateFlow<FestivalHeaderUiState> = _uiState.asStateFlow()

    private var currentSlug: String = ""
    private var currentAccessToken: String? = null

    /**
     * Load festival header data
     * Uses cached data if available to avoid refetch on rotation
     *
     * @param slug Festival slug (e.g., "floydfest-26")
     * @param accessToken Optional access token for authenticated requests
     */
    fun loadFestivalHeader(slug: String, accessToken: String? = null) {
        // If already loaded for same slug, return cached version
        if (currentSlug == slug && _uiState.value is FestivalHeaderUiState.Success) {
            return
        }

        currentSlug = slug
        currentAccessToken = accessToken

        viewModelScope.launch {
            _uiState.value = FestivalHeaderUiState.Loading

            val result = repository.getFestivalHeader(slug, accessToken)

            _uiState.value = when (result) {
                is Result.Success -> {
                    FestivalHeaderUiState.Success(result.data)
                }
                is Result.Error -> {
                    FestivalHeaderUiState.Error(result.message, result.code)
                }
                is Result.Loading -> {
                    FestivalHeaderUiState.Loading
                }
            }
        }
    }

    /**
     * Retry loading if previous attempt failed
     */
    fun retry() {
        if (currentSlug.isNotBlank()) {
            loadFestivalHeader(currentSlug, currentAccessToken)
        }
    }

    /**
     * Clear the cached data
     */
    fun clearCache() {
        repository.clearCache()
        currentSlug = ""
    }
}
