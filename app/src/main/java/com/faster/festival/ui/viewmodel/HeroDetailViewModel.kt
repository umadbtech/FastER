package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.HeroCarouselItem
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HeroDetailState {
    object Loading : HeroDetailState()
    data class Success(val heroItem: HeroCarouselItem) : HeroDetailState()
    data class Error(val message: String) : HeroDetailState()
}

class HeroDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val heroItemId: String
) : ViewModel() {

    private val _heroState = MutableStateFlow<HeroDetailState>(HeroDetailState.Loading)
    val heroState: StateFlow<HeroDetailState> = _heroState.asStateFlow()

    init {
        loadHeroItem()
    }

    private fun loadHeroItem() {
        viewModelScope.launch {
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
                _heroState.value = HeroDetailState.Error(
                    e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val heroItemId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HeroDetailViewModel(appHomeApi, festivalSlug, heroItemId) as T
        }
    }
}
