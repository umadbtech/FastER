package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.remote.ContentLineupApi
import com.faster.festival.data.remote.ContentStageScheduleApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LineupArtist(
    val id: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val genres: List<String>,
    val setTime: String?,
    val stageName: String?,
    val day: Int?,
    val order: Int
)

sealed class LineupUiState {
    object Loading : LineupUiState()
    data class Success(
        val artists: List<LineupArtist>,
        val filteredArtists: List<LineupArtist>,
        val days: List<Int>,
        val selectedDay: Int?,
        val searchQuery: String
    ) : LineupUiState()
    data class Error(val message: String) : LineupUiState()
}

class LineupViewModel(
    private val contentLineupApi: ContentLineupApi,
    private val contentStageScheduleApi: ContentStageScheduleApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<LineupUiState>(LineupUiState.Loading)
    val uiState: StateFlow<LineupUiState> = _uiState.asStateFlow()

    private var allArtists: List<LineupArtist> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentDayFilter: Int? = null

    init {
        loadLineup()
    }

    private fun loadLineup() {
        viewModelScope.launch {
            _uiState.value = LineupUiState.Loading
            try {
                val lineupResponse = contentLineupApi.getContentLineup(festivalSlug)
                val scheduleResponse = contentStageScheduleApi.getStageSchedule(festivalSlug)

                val lineupBody = lineupResponse.body()
                val scheduleBody = scheduleResponse.body()

                if (lineupResponse.isSuccessful && lineupBody != null) {
                    val scheduleMap = mutableMapOf<String, ContentStageScheduleApi.ScheduleSlot>()
                    if (scheduleResponse.isSuccessful && scheduleBody != null) {
                        scheduleBody.stages.forEach { stage ->
                            stage.schedule.forEach { slot ->
                                scheduleMap[slot.artist_id] = slot
                            }
                        }
                    }

                    allArtists = lineupBody.featured_artists.map { artist ->
                        val schedule = scheduleMap[artist.id]
                        LineupArtist(
                            id = artist.id,
                            name = artist.name,
                            bio = artist.bio,
                            imageUrl = artist.image_url,
                            genres = artist.genres ?: emptyList(),
                            setTime = schedule?.let { "${it.start_time} - ${it.end_time}" },
                            stageName = schedule?.let {
                                scheduleBody?.stages?.find { s -> s.id == it.id }?.name
                            } ?: schedule?.artist_name?.let { null },
                            day = schedule?.day,
                            order = artist.order
                        )
                    }.sortedBy { it.order }

                    val days = allArtists.mapNotNull { it.day }.distinct().sorted()
                    applyFilters(days)
                } else {
                    _uiState.value = LineupUiState.Error(
                        "Failed to load lineup: ${lineupResponse.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LineupUiState.Error(
                    e.localizedMessage ?: "Network error occurred"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        val days = allArtists.mapNotNull { it.day }.distinct().sorted()
        applyFilters(days)
    }

    fun onDayFilterChanged(day: Int?) {
        currentDayFilter = day
        val days = allArtists.mapNotNull { it.day }.distinct().sorted()
        applyFilters(days)
    }

    fun refresh() {
        currentSearchQuery = ""
        currentDayFilter = null
        loadLineup()
    }

    private fun applyFilters(days: List<Int>) {
        var filtered = allArtists

        if (currentDayFilter != null) {
            filtered = filtered.filter { it.day == currentDayFilter }
        }

        if (currentSearchQuery.isNotBlank()) {
            val query = currentSearchQuery.lowercase()
            filtered = filtered.filter { artist ->
                artist.name.lowercase().contains(query) ||
                    artist.genres.any { it.lowercase().contains(query) }
            }
        }

        _uiState.value = LineupUiState.Success(
            artists = allArtists,
            filteredArtists = filtered,
            days = days,
            selectedDay = currentDayFilter,
            searchQuery = currentSearchQuery
        )
    }

    class Factory(
        private val contentLineupApi: ContentLineupApi,
        private val contentStageScheduleApi: ContentStageScheduleApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LineupViewModel(contentLineupApi, contentStageScheduleApi, festivalSlug) as T
        }
    }
}
