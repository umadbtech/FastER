package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.ContentScheduleEvent
import com.faster.festival.data.models.ContentStageSchedule
import com.faster.festival.data.remote.ContentStageScheduleApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StageScheduleUiState(
    val isLoading: Boolean = true,
    val stages: List<ContentStageSchedule> = emptyList(),
    val allEvents: List<ContentScheduleEvent> = emptyList(),
    val filteredEvents: List<ContentScheduleEvent> = emptyList(),
    val selectedStageId: String? = null,
    val selectedDay: Int? = null,
    val days: List<Int> = emptyList(),
    val error: String? = null
)

class StageScheduleViewModel(
    private val contentStageScheduleApi: ContentStageScheduleApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(StageScheduleUiState())
    val uiState: StateFlow<StageScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = contentStageScheduleApi.getStageSchedule(festivalSlug)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val stages = body.stages
                        val events = body.events.sortedWith(
                            compareBy<ContentScheduleEvent> { it.day }
                                .thenBy { it.startTime }
                        )
                        val days = events.map { it.day }.distinct().sorted()
                        _uiState.value = StageScheduleUiState(
                            isLoading = false,
                            stages = stages,
                            allEvents = events,
                            filteredEvents = events,
                            days = days,
                            selectedStageId = null,
                            selectedDay = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Empty response"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "API error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    fun selectStage(stageId: String?) {
        _uiState.value = _uiState.value.copy(selectedStageId = stageId)
        applyFilters()
    }

    fun selectDay(day: Int?) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.allEvents

        state.selectedStageId?.let { stageId ->
            filtered = filtered.filter { it.stageId == stageId }
        }
        state.selectedDay?.let { day ->
            filtered = filtered.filter { it.day == day }
        }

        _uiState.value = state.copy(filteredEvents = filtered)
    }

    fun retry() {
        loadSchedule()
    }

    class Factory(
        private val contentStageScheduleApi: ContentStageScheduleApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StageScheduleViewModel(contentStageScheduleApi, festivalSlug) as T
        }
    }
}
