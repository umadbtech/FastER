package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.models.Result
import com.faster.festival.data.repository.AppHomeRepository
import com.faster.festival.ui.util.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI State for Festival Header */
sealed class FestivalHeaderUiState {
    object Loading : FestivalHeaderUiState()
    data class Success(val header: FestivalHeader) : FestivalHeaderUiState()
    data class Error(val message: String, val code: Int? = null) : FestivalHeaderUiState()
}

/**
 * Production-grade AppHomeViewModel ✅ CONSOLIDATED: Now handles both App Home Bundle AND Festival
 * Header Uses server-driven App Home Bundle API with category-based sections
 *
 * ✅ Features:
 * - Automatic token refresh on 401 (via TokenRefreshInterceptor)
 * - Unified error mapping via ErrorMapper
 * - Proper error state clearing on manual retry
 * - Festival header loading (consolidated from FestivalHeaderViewModel)
 */
class AppHomeViewModel(
        private val appHomeRepository: AppHomeRepository,
        private val festivalSlug: String
) : ViewModel() {

    private val _bundleState = MutableStateFlow<UiState<AppHomeBundleResponse>>(UiState.Loading)
    val bundleState: StateFlow<UiState<AppHomeBundleResponse>> = _bundleState.asStateFlow()

    // ✅ CONSOLIDATED: Festival header state (merged from FestivalHeaderViewModel)
    private val _festivalHeaderState =
            MutableStateFlow<FestivalHeaderUiState>(FestivalHeaderUiState.Loading)
    val festivalHeaderState: StateFlow<FestivalHeaderUiState> = _festivalHeaderState.asStateFlow()

    init {
        loadAppHomeBundle()
    }

    /**
     * Load app home bundle from API
     *
     * ✅ Error handling:
     * - 401: TokenRefreshInterceptor retries automatically
     * - 500+: User sees error with manual retry button
     * - Network: User sees connection error with retry button
     */
    private fun loadAppHomeBundle() {
        viewModelScope.launch {
            try {
                appHomeRepository.getAppHomeBundle(festivalSlug).collect { bundle ->
                    _bundleState.value = UiState.Success(bundle)
                }
            } catch (e: Exception) {
                // ✅ Use centralized ErrorMapper for consistent error messages
                val errorMessage = ErrorMapper.mapThrowableToMessage(e)
                _bundleState.value = UiState.Error(errorMessage)
            }
        }
    }

    /**
     * Refresh app home bundle - called when user clicks "Retry" button
     *
     * ✅ Important: Always reset to Loading first to ensure error banner clears
     */
    fun refreshBundle() {
        _bundleState.value = UiState.Loading // ✅ Clear error state before retry
        loadAppHomeBundle()
    }

    // ============================================================================
    // ✅ CONSOLIDATED: Festival Header methods (merged from FestivalHeaderViewModel)
    // ============================================================================

    /**
     * Load festival header data (consolidated from FestivalHeaderViewModel) Uses cached data if
     * available to avoid refetch on rotation
     *
     * @param accessToken Optional access token for authenticated requests
     */
    fun loadFestivalHeader(accessToken: String? = null) {
        // If already loaded and cached, return cached version
        val cached = appHomeRepository.getCachedFestivalHeader()
        if (cached != null && _festivalHeaderState.value is FestivalHeaderUiState.Success) {
            return
        }

        viewModelScope.launch {
            _festivalHeaderState.value = FestivalHeaderUiState.Loading

            val result = appHomeRepository.getFestivalHeader(festivalSlug, accessToken)

            _festivalHeaderState.value =
                    when (result) {
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

    /** Retry loading festival header if previous attempt failed */
    fun retryFestivalHeader() {
        loadFestivalHeader()
    }

    /** Factory for creating instances with DI */
    companion object {
        fun createFactory(
                appHomeRepository: AppHomeRepository,
                festivalSlug: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppHomeViewModel(appHomeRepository, festivalSlug) as T
                }
            }
        }
    }
}
