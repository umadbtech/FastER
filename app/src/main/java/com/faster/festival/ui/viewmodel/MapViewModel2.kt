package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.remote.ContentMapApi
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
    val iconUrl: String?,
    val imageUrl: String?
)

data class MapInfo(
    val mapImageUrl: String,
    val thumbnailUrl: String?,
    val venues: List<MapVenue>,
    val facilities: List<MapFacility>
)

data class MapFacility(
    val id: String,
    val name: String,
    val facilityType: String,
    val description: String?,
    val location: String?,
    val openingHours: String?
)

sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(
        val mapInfo: MapInfo,
        val filteredVenues: List<MapVenue>,
        val selectedFilter: String
    ) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class NewMapViewModel(
    private val contentMapApi: ContentMapApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var mapInfo: MapInfo? = null
    private var currentFilter: String = "All"

    init {
        loadMap()
    }

    private fun loadMap() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                val response = contentMapApi.getContentMap(festivalSlug)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val venues = body.points_of_interest.map { poi ->
                            MapVenue(
                                id = poi.id,
                                name = poi.name,
                                type = poi.type,
                                description = poi.description,
                                latitude = poi.latitude,
                                longitude = poi.longitude,
                                iconUrl = poi.icon_url,
                                imageUrl = poi.image_url
                            )
                        }
                        val facilities = body.facilities.map { f ->
                            MapFacility(
                                id = f.id,
                                name = f.name,
                                facilityType = f.facility_type,
                                description = f.description,
                                location = f.location,
                                openingHours = f.opening_hours
                            )
                        }
                        mapInfo = MapInfo(
                            mapImageUrl = body.map.image_url,
                            thumbnailUrl = body.map.thumbnail_url,
                            venues = venues,
                            facilities = facilities
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
                _uiState.value = MapUiState.Error(
                    e.localizedMessage ?: "Network error occurred"
                )
            }
        }
    }

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
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewMapViewModel(contentMapApi, festivalSlug) as T
        }
    }
}
