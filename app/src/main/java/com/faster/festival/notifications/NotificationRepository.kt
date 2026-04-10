package com.faster.festival.notifications

import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val prefsManager: NotificationPreferencesManager
) {

    val preferences: Flow<NotificationPreferences> = prefsManager.preferences

    suspend fun setPushEnabled(enabled: Boolean) {
        prefsManager.setPushEnabled(enabled)
        if (enabled) {
            // Re-read current prefs to sync topics
            // Individual topic states are already persisted; syncing happens in ViewModel
        } else {
            TopicManager.unsubscribeAll()
        }
    }

    suspend fun setEmergencyAlerts(enabled: Boolean) {
        prefsManager.setEmergencyAlerts(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_EMERGENCY)
        else TopicManager.unsubscribe(TopicManager.TOPIC_EMERGENCY)
    }

    suspend fun setFestivalUpdates(enabled: Boolean) {
        prefsManager.setFestivalUpdates(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_FESTIVAL)
        else TopicManager.unsubscribe(TopicManager.TOPIC_FESTIVAL)
    }

    suspend fun setExclusivePromotions(enabled: Boolean) {
        prefsManager.setExclusivePromotions(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_PROMOTIONS)
        else TopicManager.unsubscribe(TopicManager.TOPIC_PROMOTIONS)
    }

    suspend fun setSmsNotifications(enabled: Boolean) {
        prefsManager.setSmsNotifications(enabled)
        // Future: sync with backend API
    }

    suspend fun setEmailNotifications(enabled: Boolean) {
        prefsManager.setEmailNotifications(enabled)
        // Future: sync with backend API
    }

    fun syncAllTopics(prefs: NotificationPreferences) {
        if (!prefs.pushEnabled) {
            TopicManager.unsubscribeAll()
            return
        }
        TopicManager.syncSubscriptions(
            emergencyAlerts = prefs.emergencyAlerts,
            festivalUpdates = prefs.festivalUpdates,
            exclusivePromotions = prefs.exclusivePromotions
        )
    }
}
