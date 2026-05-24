package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.local.SosHistoryRecord
import com.faster.festival.data.repository.local.SosHistoryRepository
import com.faster.festival.data.sos.remote.PinchAlertHistoryItem
import com.faster.festival.domain.sos.FetchSosHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

/**
 * Drives the SOS History screen. The list itself is served from the local Room
 * cache ([SosHistoryRepository.history]) so it survives offline; on open we pull
 * the authoritative list from `pinch-alert-history` ([fetchHistory]) and upsert
 * each alert into the cache (keyed by alert id) so terminal `ui_status` and
 * latest location replace the local stub instead of duplicating.
 */
class SosHistoryViewModel(
    private val repository: SosHistoryRepository,
    private val fetchHistory: FetchSosHistoryUseCase? = null
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val hasMore: Boolean = false
    )

    val history: StateFlow<List<SosHistoryRecord>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null

    private val _selectedRecord = MutableStateFlow<SosHistoryRecord?>(null)
    val selectedRecord: StateFlow<SosHistoryRecord?> = _selectedRecord.asStateFlow()

    init {
        refresh()
    }

    /** Re-fetch the first page from the backend and upsert into the cache. */
    fun refresh() {
        val useCase = fetchHistory ?: return
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            useCase(limit = PAGE_SIZE, cursor = null).fold(
                onSuccess = { resp ->
                    resp.alerts.forEach { upsert(it) }
                    nextCursor = resp.nextCursor
                    _uiState.update {
                        it.copy(isLoading = false, hasMore = resp.nextCursor != null)
                    }
                },
                onFailure = { err ->
                    Timber.tag(TAG).w(err, "pinch-alert-history refresh failed")
                    // Keep showing the cached list; surface a soft error only.
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    /** Fetch the next page (cursor pagination) and append into the cache. */
    fun loadMore() {
        val useCase = fetchHistory ?: return
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        viewModelScope.launch {
            useCase(limit = PAGE_SIZE, cursor = cursor).fold(
                onSuccess = { resp ->
                    resp.alerts.forEach { upsert(it) }
                    nextCursor = resp.nextCursor
                    _uiState.update {
                        it.copy(isLoadingMore = false, hasMore = resp.nextCursor != null)
                    }
                },
                onFailure = { err ->
                    Timber.tag(TAG).w(err, "pinch-alert-history loadMore failed")
                    _uiState.update { it.copy(isLoadingMore = false, error = err.message) }
                }
            )
        }
    }

    fun loadRecord(id: Long) {
        viewModelScope.launch {
            _selectedRecord.value = repository.getById(id)
        }
    }

    fun clearSelection() {
        _selectedRecord.value = null
    }

    private suspend fun upsert(item: PinchAlertHistoryItem) {
        val alertId = item.id?.takeIf { it.isNotBlank() } ?: return
        val coords = item.latestLocation?.let { loc ->
            val lat = loc.latitude
            val lng = loc.longitude
            if (lat != null && lng != null) "$lat,$lng" else null
        }
        repository.upsertRemote(
            requestId = alertId,
            createdAt = parseIso(item.receivedAt) ?: System.currentTimeMillis(),
            emergencyTypes = emptyList(), // not returned by the history endpoint
            status = humanize(item.uiStatus ?: item.userStatus ?: item.status ?: "UNKNOWN"),
            locationText = null,
            coordinates = coords,
            contactPhone = null,
            additionalInfo = null,
            triggerType = item.triggerSource ?: "mobile_ui"
        )
    }

    private fun parseIso(value: String?): Long? =
        value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    /** "HELP_ON_THE_WAY" → "Help On The Way". */
    private fun humanize(raw: String): String =
        raw.split('_').joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }

    class Factory(
        private val repository: SosHistoryRepository,
        private val fetchHistory: FetchSosHistoryUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SosHistoryViewModel(repository, fetchHistory) as T
        }
    }

    private companion object {
        const val TAG = "SosHistoryVM"
        const val PAGE_SIZE = 20
    }
}
