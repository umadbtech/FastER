package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: AppHomeBundleResponse) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedETag: String? = null
    private var cachedResponse: AppHomeBundleResponse? = null

    init {
        loadHomeBundle(festivalSlug)
    }

    fun loadHomeBundle(festivalSlug: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = cachedETag
                )
                when {
                    response.code() == 304 && cachedResponse != null -> {
                        _uiState.value = HomeUiState.Success(cachedResponse!!)
                    }
                    response.isSuccessful -> {
                        val body = response.body()
                        if (body != null) {
                            cachedETag = response.headers()["ETag"]
                            cachedResponse = body
                            _uiState.value = HomeUiState.Success(body)
                        } else {
                            _uiState.value = HomeUiState.Error("Empty response from server")
                        }
                    }
                    response.code() == 401 -> {
                        _uiState.value = HomeUiState.Error("Authorization required (401)")
                    }
                    response.code() == 404 -> {
                        _uiState.value = HomeUiState.Error("Festival not found (404)")
                    }
                    response.code() == 500 -> {
                        _uiState.value = HomeUiState.Error("Server error (500)")
                    }
                    else -> {
                        _uiState.value = HomeUiState.Error(
                            "API error: ${response.code()} - ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.localizedMessage ?: "Network error occurred"
                )
            }
        }
    }

    fun refresh() {
        cachedETag = null
        cachedResponse = null
        loadHomeBundle(festivalSlug)
    }

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appHomeApi, festivalSlug) as T
        }
    }
}
