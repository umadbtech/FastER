package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.remote.ContentFaqApi
import com.faster.festival.data.remote.FaqApiItem
import com.faster.festival.data.remote.FaqCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FaqUiState(
    val isLoading: Boolean = true,
    val categories: List<FaqCategory> = emptyList(),
    val allItems: List<FaqApiItem> = emptyList(),
    val filteredItems: List<FaqApiItem> = emptyList(),
    val selectedCategory: String? = null, // null = "All"
    val error: String? = null
)

class FaqViewModel(
    private val contentFaqApi: ContentFaqApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaqUiState())
    val uiState: StateFlow<FaqUiState> = _uiState.asStateFlow()

    init {
        loadFaq()
    }

    private fun loadFaq() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = contentFaqApi.getFaq(festivalSlug = festivalSlug)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val categories = body.categories.sortedBy { it.sortOrder }
                        val items = body.items.sortedBy { it.sortOrder }
                        _uiState.value = FaqUiState(
                            isLoading = false,
                            categories = categories,
                            allItems = items,
                            filteredItems = items,
                            selectedCategory = null
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

    fun selectCategory(categoryKey: String?) {
        val items = _uiState.value.allItems
        val filtered = if (categoryKey == null) {
            items
        } else {
            items.filter { it.categoryKeys.contains(categoryKey) }
        }
        _uiState.value = _uiState.value.copy(
            selectedCategory = categoryKey,
            filteredItems = filtered
        )
    }

    fun retry() {
        loadFaq()
    }

    class Factory(
        private val contentFaqApi: ContentFaqApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FaqViewModel(contentFaqApi, festivalSlug) as T
        }
    }
}
