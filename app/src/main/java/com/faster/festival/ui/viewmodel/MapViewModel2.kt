package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.ContentMapApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapVenue(
    val id: String,
    val name: String,
    val type: String,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val nextEventTitle: String?,
    val nextEventTime: String?
)

data class MapInfo(
    val festivalName: String,
    val venues: List<MapVenue>
)

sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(
        val mapInfo: MapInfo,
        val filteredVenues: List<MapVenue>,
        val selectedFilter: String
    ) : MapUiState()
    object Offline : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class NewMapViewModel(
    private val contentMapApi: ContentMapApi,
    private val festivalSlug: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var mapInfo: MapInfo? = null
    private var currentFilter: String = "All"
    private var autoRetryJob: Job? = null

    init {
        loadMap()
    }

    fun loadMap() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _uiState.value = MapUiState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadMap() }
                return@launch
            }
            _uiState.value = MapUiState.Loading
            try {
                val response = contentMapApi.getContentMap(festivalSlug)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val venues = body.mapPoints.map { point ->
                            MapVenue(
                                id = point.id,
                                name = point.name,
                                type = point.kind,
                                description = point.description,
                                latitude = point.lat,
                                longitude = point.lng,
                                nextEventTitle = point.nextEvent?.title,
                                nextEventTime = point.nextEvent?.startsAt
                            )
                        }
                        mapInfo = MapInfo(
                            festivalName = body.festival.name,
                            venues = venues
                        )
                        applyFilter()
                    } else {
                        _uiState.value = MapUiState.Error("Empty response from server")
                    }
                } else {
                    _uiState.value = MapUiState.Error(
                        "Failed to load map: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _uiState.value = MapUiState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadMap() }
                } else {
                    _uiState.value = MapUiState.Error(
                        e.localizedMessage ?: "Network error occurred"
                    )
                }
            }
        }
    }

    fun retry() = loadMap()

    fun onFilterChanged(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    fun refresh() {
        currentFilter = "All"
        loadMap()
    }

    private fun applyFilter() {
        val info = mapInfo ?: return
        val filtered = if (currentFilter == "All") {
            info.venues
        } else {
            info.venues.filter { venue ->
                venue.type.equals(currentFilter, ignoreCase = true)
            }
        }
        _uiState.value = MapUiState.Success(
            mapInfo = info,
            filteredVenues = filtered,
            selectedFilter = currentFilter
        )
    }

    class Factory(
        private val contentMapApi: ContentMapApi,
        private val festivalSlug: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewMapViewModel(contentMapApi, festivalSlug, networkMonitor) as T
        }
    }
}
