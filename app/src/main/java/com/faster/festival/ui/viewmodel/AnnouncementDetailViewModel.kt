package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.Announcement
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AnnouncementDetailState {
    object Loading : AnnouncementDetailState()
    data class Success(val announcement: Announcement) : AnnouncementDetailState()
    object Offline : AnnouncementDetailState()
    data class Error(val message: String) : AnnouncementDetailState()
}

class AnnouncementDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val announcementId: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _state = MutableStateFlow<AnnouncementDetailState>(AnnouncementDetailState.Loading)
    val state: StateFlow<AnnouncementDetailState> = _state.asStateFlow()
    private var autoRetryJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _state.value = AnnouncementDetailState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { load() }
                return@launch
            }
            _state.value = AnnouncementDetailState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val announcement = body.announcements.find { it.id == announcementId }
                        if (announcement != null) {
                            _state.value = AnnouncementDetailState.Success(announcement)
                        } else {
                            _state.value = AnnouncementDetailState.Error("Announcement not found")
                        }
                    } else {
                        _state.value = AnnouncementDetailState.Error("Empty response")
                    }
                } else {
                    _state.value = AnnouncementDetailState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _state.value = AnnouncementDetailState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { load() }
                } else {
                    _state.value = AnnouncementDetailState.Error(
                        e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun retry() = load()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val announcementId: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnnouncementDetailViewModel(
                appHomeApi, festivalSlug, announcementId, networkMonitor
            ) as T
        }
    }
}
