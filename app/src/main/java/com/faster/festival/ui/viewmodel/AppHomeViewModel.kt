package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.repository.AppHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production-grade AppHomeViewModel
 * Uses server-driven App Home Bundle API with category-based sections
 */
class AppHomeViewModel(
    private val appHomeRepository: AppHomeRepository,
    private val festivalSlug: String
) : ViewModel() {

    private val _bundleState = MutableStateFlow<UiState<AppHomeBundleResponse>>(UiState.Loading)
    val bundleState: StateFlow<UiState<AppHomeBundleResponse>> = _bundleState.asStateFlow()

    init {
        loadAppHomeBundle()
    }

    /**
     * Load app home bundle from API
     */
    private fun loadAppHomeBundle() {
        viewModelScope.launch {
            try {
                appHomeRepository.getAppHomeBundle(festivalSlug).collect { bundle ->
                    _bundleState.value = UiState.Success(bundle)
                }
            } catch (e: Exception) {
                _bundleState.value = UiState.Error(e.message ?: "Failed to load home bundle")
            }
        }
    }

    /**
     * Refresh app home bundle
     */
    fun refreshBundle() {
        _bundleState.value = UiState.Loading
        loadAppHomeBundle()
    }

    /**
     * Factory for creating instances with DI
     */
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
