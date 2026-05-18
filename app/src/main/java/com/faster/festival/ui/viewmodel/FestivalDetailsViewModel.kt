package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FestivalDetailsState {
    object Loading : FestivalDetailsState()
    data class Success(
        val festival: FestivalHeader,
        val bannerUrls: List<String>,
        val location: String?
    ) : FestivalDetailsState()
    object Offline : FestivalDetailsState()
    data class Error(val message: String) : FestivalDetailsState()
}

class FestivalDetailsViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _state = MutableStateFlow<FestivalDetailsState>(FestivalDetailsState.Loading)
    val state: StateFlow<FestivalDetailsState> = _state.asStateFlow()
    private var autoRetryJob: Job? = null

    init { loadFestival() }

    fun loadFestival() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _state.value = FestivalDetailsState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadFestival() }
                return@launch
            }
            _state.value = FestivalDetailsState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val f = body.festival
                        val header = FestivalHeader(
                            id = f.id,
                            slug = f.slug,
                            name = f.name,
                            timezone = f.timezone,
                            startsAt = f.startsAt,
                            endsAt = f.endsAt,
                            logoUrl = f.logoUrl ?: "",
                            bannerUrl = f.bannerUrl ?: "",
                            accentColorHex = f.accentColorHex,
                            contextState = f.contextState
                        )
                        val bannerUrls = f.bannerUrls.ifEmpty {
                            listOfNotNull(f.bannerUrl)
                        }
                        _state.value = FestivalDetailsState.Success(
                            festival = header,
                            bannerUrls = bannerUrls,
                            location = f.location
                        )
                    } else {
                        _state.value = FestivalDetailsState.Error("Empty response")
                    }
                } else {
                    _state.value = FestivalDetailsState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _state.value = FestivalDetailsState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadFestival() }
                } else {
                    _state.value = FestivalDetailsState.Error(
                        e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun retry() = loadFestival()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FestivalDetailsViewModel(appHomeApi, festivalSlug, networkMonitor) as T
        }
    }
}
