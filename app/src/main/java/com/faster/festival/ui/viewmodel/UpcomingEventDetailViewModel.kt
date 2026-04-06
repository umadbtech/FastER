package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.UpcomingEvent
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpcomingEventDetailState {
    object Loading : UpcomingEventDetailState()
    data class Success(val event: UpcomingEvent) : UpcomingEventDetailState()
    data class Error(val message: String) : UpcomingEventDetailState()
}

class UpcomingEventDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val eventId: String
) : ViewModel() {

    private val _state = MutableStateFlow<UpcomingEventDetailState>(UpcomingEventDetailState.Loading)
    val state: StateFlow<UpcomingEventDetailState> = _state.asStateFlow()

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
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
                _state.value = UpcomingEventDetailState.Error(
                    e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val eventId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpcomingEventDetailViewModel(appHomeApi, festivalSlug, eventId) as T
        }
    }
}
