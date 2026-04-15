package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.local.SosHistoryRecord
import com.faster.festival.data.repository.local.SosHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SosHistoryViewModel(
    private val repository: SosHistoryRepository
) : ViewModel() {

    val history: StateFlow<List<SosHistoryRecord>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedRecord = MutableStateFlow<SosHistoryRecord?>(null)
    val selectedRecord: StateFlow<SosHistoryRecord?> = _selectedRecord.asStateFlow()

    fun loadRecord(id: Long) {
        viewModelScope.launch {
            _selectedRecord.value = repository.getById(id)
        }
    }

    fun clearSelection() {
        _selectedRecord.value = null
    }

    class Factory(
        private val repository: SosHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SosHistoryViewModel(repository) as T
        }
    }
}
