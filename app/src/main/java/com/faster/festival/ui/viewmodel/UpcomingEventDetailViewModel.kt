package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.UpcomingEvent
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpcomingEventDetailState {
    object Loading : UpcomingEventDetailState()
    data class Success(val event: UpcomingEvent) : UpcomingEventDetailState()
    object Offline : UpcomingEventDetailState()
    data class Error(val message: String) : UpcomingEventDetailState()
}

class UpcomingEventDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val eventId: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _state = MutableStateFlow<UpcomingEventDetailState>(UpcomingEventDetailState.Loading)
    val state: StateFlow<UpcomingEventDetailState> = _state.asStateFlow()
    private var autoRetryJob: Job? = null

    init { loadEvent() }

    fun loadEvent() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _state.value = UpcomingEventDetailState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadEvent() }
                return@launch
            }
            _state.value = UpcomingEventDetailState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val event = body.upcomingEvents.find { it.id == eventId }
                        if (event != null) {
                            _state.value = UpcomingEventDetailState.Success(event)
                        } else {
                            _state.value = UpcomingEventDetailState.Error("Event not found")
                        }
                    } else {
                        _state.value = UpcomingEventDetailState.Error("Empty response")
                    }
                } else {
                    _state.value = UpcomingEventDetailState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _state.value = UpcomingEventDetailState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadEvent() }
                } else {
                    _state.value = UpcomingEventDetailState.Error(
                        e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun retry() = loadEvent()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val eventId: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpcomingEventDetailViewModel(
                appHomeApi, festivalSlug, eventId, networkMonitor
            ) as T
        }
    }
}
