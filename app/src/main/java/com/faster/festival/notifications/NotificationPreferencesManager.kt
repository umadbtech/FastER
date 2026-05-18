package com.faster.festival.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

data class NotificationPreferences(
    val pushEnabled: Boolean = false,
    val emergencyAlerts: Boolean = true,
    val festivalUpdates: Boolean = true,
    val exclusivePromotions: Boolean = true,
    val smsNotifications: Boolean = false,
    val emailNotifications: Boolean = false
)

class NotificationPreferencesManager(private val context: Context) {

    private object Keys {
        val PUSH_ENABLED = booleanPreferencesKey("push_enabled")
        val EMERGENCY_ALERTS = booleanPreferencesKey("emergency_alerts")
        val FESTIVAL_UPDATES = booleanPreferencesKey("festival_updates")
        val EXCLUSIVE_PROMOTIONS = booleanPreferencesKey("exclusive_promotions")
        val SMS_NOTIFICATIONS = booleanPreferencesKey("sms_notifications")
        val EMAIL_NOTIFICATIONS = booleanPreferencesKey("email_notifications")
    }

    val preferences: Flow<NotificationPreferences> = context.notificationDataStore.data.map { prefs ->
        NotificationPreferences(
            pushEnabled = prefs[Keys.PUSH_ENABLED] ?: false,
            emergencyAlerts = prefs[Keys.EMERGENCY_ALERTS] ?: true,
            festivalUpdates = prefs[Keys.FESTIVAL_UPDATES] ?: true,
            exclusivePromotions = prefs[Keys.EXCLUSIVE_PROMOTIONS] ?: true,
            smsNotifications = prefs[Keys.SMS_NOTIFICATIONS] ?: false,
            emailNotifications = prefs[Keys.EMAIL_NOTIFICATIONS] ?: false
        )
    }

    suspend fun setPushEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.PUSH_ENABLED] = enabled }
    }

    suspend fun setEmergencyAlerts(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.EMERGENCY_ALERTS] = enabled }
    }

    suspend fun setFestivalUpdates(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.FESTIVAL_UPDATES] = enabled }
    }

    suspend fun setExclusivePromotions(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.EXCLUSIVE_PROMOTIONS] = enabled }
    }

    suspend fun setSmsNotifications(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.SMS_NOTIFICATIONS] = enabled }
    }

    suspend fun setEmailNotifications(enabled: Boolean) {
        context.notificationDataStore.edit { it[Keys.EMAIL_NOTIFICATIONS] = enabled }
    }

    /** Persist a complete preferences snapshot in a single transaction. */
    suspend fun saveAll(prefs: NotificationPreferences) {
        context.notificationDataStore.edit {
            it[Keys.PUSH_ENABLED] = prefs.pushEnabled
            it[Keys.EMERGENCY_ALERTS] = prefs.emergencyAlerts
            it[Keys.FESTIVAL_UPDATES] = prefs.festivalUpdates
            it[Keys.EXCLUSIVE_PROMOTIONS] = prefs.exclusivePromotions
            it[Keys.SMS_NOTIFICATIONS] = prefs.smsNotifications
            it[Keys.EMAIL_NOTIFICATIONS] = prefs.emailNotifications
        }
    }
}
