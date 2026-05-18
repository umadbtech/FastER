package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.HeroCarouselItem
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HeroDetailState {
    object Loading : HeroDetailState()
    data class Success(val heroItem: HeroCarouselItem) : HeroDetailState()
    object Offline : HeroDetailState()
    data class Error(val message: String) : HeroDetailState()
}

class HeroDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val heroItemId: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _heroState = MutableStateFlow<HeroDetailState>(HeroDetailState.Loading)
    val heroState: StateFlow<HeroDetailState> = _heroState.asStateFlow()
    private var autoRetryJob: Job? = null

    init { loadHeroItem() }

    fun loadHeroItem() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _heroState.value = HeroDetailState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadHeroItem() }
                return@launch
            }
            _heroState.value = HeroDetailState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val item = body.heroCarouselItems.find { it.id == heroItemId }
                        if (item != null) {
                            _heroState.value = HeroDetailState.Success(item)
                        } else {
                            _heroState.value = HeroDetailState.Error("Item not found")
                        }
                    } else {
                        _heroState.value = HeroDetailState.Error("Empty response")
                    }
                } else {
                    _heroState.value = HeroDetailState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _heroState.value = HeroDetailState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadHeroItem() }
                } else {
                    _heroState.value = HeroDetailState.Error(
                        e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun retry() = loadHeroItem()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val heroItemId: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HeroDetailViewModel(
                appHomeApi, festivalSlug, heroItemId, networkMonitor
            ) as T
        }
    }
}
