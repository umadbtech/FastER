package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.network.ConnectivityState
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: AppHomeBundleResponse) : HomeUiState()
    /** Phone has no internet — render with NoInternetScreen and auto-retry on reconnect. */
    object Offline : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedETag: String? = null
    private var cachedResponse: AppHomeBundleResponse? = null
    private var autoRetryJob: Job? = null

    init {
        loadHomeBundle(festivalSlug)
    }

    fun loadHomeBundle(festivalSlug: String) {
        viewModelScope.launch {
            // Fast path — if we know we're offline, skip the request and
            // arm a one-shot auto-retry so the screen recovers automatically.
            if (networkMonitor?.current?.isOffline == true) {
                _uiState.value = HomeUiState.Offline
                armAutoRetry()
                return@launch
            }

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
                // Classify between connectivity loss and a server-side problem.
                val offlineNow = networkMonitor?.current?.isOffline == true || e is IOException
                _uiState.value = if (offlineNow) {
                    armAutoRetry()
                    HomeUiState.Offline
                } else {
                    HomeUiState.Error(e.localizedMessage ?: "Network error occurred")
                }
            }
        }
    }

    /** Wait once for the next Available transition, then retry. */
    private fun armAutoRetry() {
        val monitor = networkMonitor ?: return
        autoRetryJob?.cancel()
        autoRetryJob = viewModelScope.launch {
            monitor.state.first { it is ConnectivityState.Available }
            if (_uiState.value is HomeUiState.Offline) {
                loadHomeBundle(festivalSlug)
            }
        }
    }

    fun refresh() {
        cachedETag = null
        cachedResponse = null
        loadHomeBundle(festivalSlug)
    }

    fun retry() = refresh()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appHomeApi, festivalSlug, networkMonitor) as T
        }
    }
}
