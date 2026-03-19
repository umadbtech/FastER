package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.remote.AppHomeApi
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
    data class Error(val message: String) : FestivalDetailsState()
}

class FestivalDetailsViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _state = MutableStateFlow<FestivalDetailsState>(FestivalDetailsState.Loading)
    val state: StateFlow<FestivalDetailsState> = _state.asStateFlow()

    init {
        loadFestival()
    }

    private fun loadFestival() {
        viewModelScope.launch {
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
                _state.value = FestivalDetailsState.Error(
                    e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FestivalDetailsViewModel(appHomeApi, festivalSlug) as T
        }
    }
}
