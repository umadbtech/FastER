package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.notifications.FcmTokenRegistrar
import com.faster.festival.notifications.NotificationPreferences
import com.faster.festival.notifications.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val initialLoadComplete: Boolean = false
)

class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    private val fcmTokenRegistrar: FcmTokenRegistrar
) : ViewModel() {

    val preferences: StateFlow<NotificationPreferences> = repository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationPreferences()
        )

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        // Sync FCM topics with persisted preferences whenever they change
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                repository.syncAllTopics(prefs)
            }
        }
        // Load fresh settings from backend on screen open
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.loadSettingsFromBackend()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        initialLoadComplete = true,
                        error = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        initialLoadComplete = true,
                        error = e.message ?: "Failed to load settings"
                    )
                }
        }
    }

    fun setPushEnabled(enabled: Boolean) = updateSetting {
        repository.setPushEnabled(enabled)
    }

    fun setEmergencyAlerts(enabled: Boolean) = updateSetting {
        repository.setEmergencyAlerts(enabled)
    }

    fun setFestivalUpdates(enabled: Boolean) = updateSetting {
        repository.setFestivalUpdates(enabled)
    }

    fun setExclusivePromotions(enabled: Boolean) = updateSetting {
        repository.setExclusivePromotions(enabled)
    }

    fun setSmsNotifications(enabled: Boolean) = updateSetting {
        repository.setSmsNotifications(enabled)
    }

    fun setEmailNotifications(enabled: Boolean) = updateSetting {
        repository.setEmailNotifications(enabled)
    }

    private fun updateSetting(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            action()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save setting"
                    )
                }
        }
    }

    fun registerDeviceToken() {
        viewModelScope.launch {
            fcmTokenRegistrar.registerCurrentToken()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(
        private val repository: NotificationRepository,
        private val fcmTokenRegistrar: FcmTokenRegistrar
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationSettingsViewModel(repository, fcmTokenRegistrar) as T
        }
    }
}
