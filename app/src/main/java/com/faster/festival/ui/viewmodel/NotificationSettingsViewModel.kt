package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.notifications.FcmTokenRegistrar
import com.faster.festival.notifications.NotificationPreferences
import com.faster.festival.notifications.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                repository.syncAllTopics(prefs)
            }
        }
    }

    fun setPushEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPushEnabled(enabled) }
    }

    fun setEmergencyAlerts(enabled: Boolean) {
        viewModelScope.launch { repository.setEmergencyAlerts(enabled) }
    }

    fun setFestivalUpdates(enabled: Boolean) {
        viewModelScope.launch { repository.setFestivalUpdates(enabled) }
    }

    fun setExclusivePromotions(enabled: Boolean) {
        viewModelScope.launch { repository.setExclusivePromotions(enabled) }
    }

    fun setSmsNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setSmsNotifications(enabled) }
    }

    fun setEmailNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setEmailNotifications(enabled) }
    }

    fun registerDeviceToken() {
        viewModelScope.launch {
            fcmTokenRegistrar.registerCurrentToken()
        }
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
