package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.PromotionItem
import com.faster.festival.data.remote.AppHomeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PromotionDetailState {
    object Loading : PromotionDetailState()
    data class Success(val promotion: PromotionItem) : PromotionDetailState()
    data class Error(val message: String) : PromotionDetailState()
}

class PromotionDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val promotionId: String
) : ViewModel() {

    private val _promotionState = MutableStateFlow<PromotionDetailState>(PromotionDetailState.Loading)
    val promotionState: StateFlow<PromotionDetailState> = _promotionState.asStateFlow()

    init {
        loadPromotion()
    }

    private fun loadPromotion() {
        viewModelScope.launch {
            _promotionState.value = PromotionDetailState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val promotion = body.promotions.find { it.id == promotionId }
                        if (promotion != null) {
                            _promotionState.value = PromotionDetailState.Success(promotion)
                        } else {
                            _promotionState.value = PromotionDetailState.Error("Promotion not found")
                        }
                    } else {
                        _promotionState.value = PromotionDetailState.Error("Empty response")
                    }
                } else {
                    _promotionState.value = PromotionDetailState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                _promotionState.value = PromotionDetailState.Error(
                    e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val promotionId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PromotionDetailViewModel(appHomeApi, festivalSlug, promotionId) as T
        }
    }
}
