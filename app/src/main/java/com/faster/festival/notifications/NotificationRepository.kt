package com.faster.festival.notifications

import android.util.Log
import com.faster.festival.data.remote.NotificationApi
import com.faster.festival.data.remote.NotificationSettingsDto
import com.faster.festival.data.remote.SaveNotificationSettingsRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NotificationRepository(
    private val prefsManager: NotificationPreferencesManager,
    private val notificationApi: NotificationApi,
    private val tokenRegistrar: FcmTokenRegistrar,
    private val festivalId: String
) {

    companion object {
        private const val TAG = "NotificationRepo"
    }

    val preferences: Flow<NotificationPreferences> = prefsManager.preferences

    /**
     * Fetch latest settings from backend, persist locally, and return them.
     * Falls back to local DataStore on network failure.
     */
    suspend fun loadSettingsFromBackend(): Result<NotificationPreferences> {
        return try {
            val token = tokenRegistrar.getCurrentToken()
            val response = notificationApi.getSettings(
                festivalId = festivalId,
                pushToken = token,
                provider = "fcm"
            )
            if (response.isSuccessful) {
                val settings = response.body()?.settings
                if (settings != null) {
                    val prefs = settings.toLocalPreferences()
                    prefsManager.saveAll(prefs)
                    Log.d(TAG, "Settings loaded from backend")
                    Result.success(prefs)
                } else {
                    val errorMsg = response.body()?.error ?: "Empty settings response"
                    Log.w(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadSettingsFromBackend failed", e)
            Result.failure(e)
        }
    }

    /**
     * Save the FULL state of all toggles to the backend (per API contract:
     * "Send the full state of all toggles, not only the changed value").
     */
    suspend fun saveSettingsToBackend(prefs: NotificationPreferences): Result<Unit> {
        return try {
            val token = tokenRegistrar.getCurrentToken()
            val request = SaveNotificationSettingsRequest(
                festivalId = festivalId,
                pushToken = token,
                provider = "fcm",
                pushEnabled = prefs.pushEnabled,
                emergencyAlerts = prefs.emergencyAlerts,
                festivalUpdates = prefs.festivalUpdates,
                exclusivePromotions = prefs.exclusivePromotions,
                smsEnabled = prefs.smsNotifications,
                emailEnabled = prefs.emailNotifications
            )
            val response = notificationApi.saveSettings(request)
            if (response.isSuccessful && response.body()?.ok == true) {
                Log.d(TAG, "Settings saved to backend")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.error ?: "HTTP ${response.code()}"
                Log.e(TAG, "saveSettings failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveSettingsToBackend failed", e)
            Result.failure(e)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Toggle setters — persist locally, sync FCM topics, then push to backend
    // ────────────────────────────────────────────────────────────────────────

    suspend fun setPushEnabled(enabled: Boolean): Result<Unit> {
        prefsManager.setPushEnabled(enabled)
        if (!enabled) TopicManager.unsubscribeAll()
        return syncCurrentSettingsToBackend()
    }

    suspend fun setEmergencyAlerts(enabled: Boolean): Result<Unit> {
        prefsManager.setEmergencyAlerts(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_EMERGENCY)
        else TopicManager.unsubscribe(TopicManager.TOPIC_EMERGENCY)
        return syncCurrentSettingsToBackend()
    }

    suspend fun setFestivalUpdates(enabled: Boolean): Result<Unit> {
        prefsManager.setFestivalUpdates(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_FESTIVAL)
        else TopicManager.unsubscribe(TopicManager.TOPIC_FESTIVAL)
        return syncCurrentSettingsToBackend()
    }

    suspend fun setExclusivePromotions(enabled: Boolean): Result<Unit> {
        prefsManager.setExclusivePromotions(enabled)
        if (enabled) TopicManager.subscribe(TopicManager.TOPIC_PROMOTIONS)
        else TopicManager.unsubscribe(TopicManager.TOPIC_PROMOTIONS)
        return syncCurrentSettingsToBackend()
    }

    suspend fun setSmsNotifications(enabled: Boolean): Result<Unit> {
        prefsManager.setSmsNotifications(enabled)
        return syncCurrentSettingsToBackend()
    }

    suspend fun setEmailNotifications(enabled: Boolean): Result<Unit> {
        prefsManager.setEmailNotifications(enabled)
        return syncCurrentSettingsToBackend()
    }

    /**
     * Reads the current local prefs and POSTs the full state to the backend.
     */
    private suspend fun syncCurrentSettingsToBackend(): Result<Unit> {
        val current = preferences.first()
        return saveSettingsToBackend(current)
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

// ═══════════════════════════════════════════════════════════════════════════════
// DTO → local model mapping
// ═══════════════════════════════════════════════════════════════════════════════

private fun NotificationSettingsDto.toLocalPreferences() = NotificationPreferences(
    pushEnabled = pushEnabled,
    emergencyAlerts = emergencyAlerts,
    festivalUpdates = festivalUpdates,
    exclusivePromotions = exclusivePromotions,
    smsNotifications = smsEnabled,
    emailNotifications = emailEnabled
)
