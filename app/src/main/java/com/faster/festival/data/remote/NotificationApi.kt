package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// ═══════════════════════════════════════════════════════════════════════════════
// Notification API — Supabase Edge Functions
// Source: notification_api.txt
// Base: {{baseUrl}}/functions/v1
// ═══════════════════════════════════════════════════════════════════════════════

interface NotificationApi {

    /**
     * 1. Register Device
     * POST /functions/v1/register-notification-device
     * Called after login or app install — registers FCM token with backend.
     */
    @POST("functions/v1/register-notification-device")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>

    /**
     * 2. Get Notification Settings
     * GET /functions/v1/notification-settings?festival_id=...&push_token=...&provider=fcm
     */
    @GET("functions/v1/notification-settings")
    suspend fun getSettings(
        @Query("festival_id") festivalId: String,
        @Query("push_token") pushToken: String?,
        @Query("provider") provider: String = "fcm"
    ): Response<NotificationSettingsResponse>

    /**
     * 3. Save Notification Settings
     * POST /functions/v1/notification-settings
     * Send the FULL state of all toggles, not just the changed ones.
     */
    @POST("functions/v1/notification-settings")
    suspend fun saveSettings(
        @Body request: SaveNotificationSettingsRequest
    ): Response<OkResponse>

    /**
     * 5. List Notifications (Inbox)
     * GET /functions/v1/list-notifications?festival_id=...&limit=20
     */
    @GET("functions/v1/list-notifications")
    suspend fun listNotifications(
        @Query("festival_id") festivalId: String,
        @Query("limit") limit: Int = 20,
        @Query("before") before: String? = null
    ): Response<ListNotificationsResponse>

    /**
     * 6. Mark Notification Read / Dismiss / All
     * POST /functions/v1/mark-notification-read
     */
    @POST("functions/v1/mark-notification-read")
    suspend fun markNotificationRead(
        @Body request: MarkNotificationReadRequest
    ): Response<OkResponse>
}

// ═══════════════════════════════════════════════════════════════════════════════
// Request DTOs
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class RegisterDeviceRequest(
    val platform: String = "android",
    val provider: String = "fcm",
    @SerialName("push_token") val pushToken: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("app_version") val appVersion: String,
    val locale: String,
    val timezone: String,
    val enabled: Boolean = true
)

@Serializable
data class SaveNotificationSettingsRequest(
    @SerialName("festival_id") val festivalId: String,
    @SerialName("push_token") val pushToken: String? = null,
    val provider: String = "fcm",
    @SerialName("push_enabled") val pushEnabled: Boolean,
    @SerialName("emergency_alerts") val emergencyAlerts: Boolean,
    @SerialName("festival_updates") val festivalUpdates: Boolean,
    @SerialName("exclusive_promotions") val exclusivePromotions: Boolean,
    @SerialName("sms_enabled") val smsEnabled: Boolean,
    @SerialName("email_enabled") val emailEnabled: Boolean
)

@Serializable
data class MarkNotificationReadRequest(
    @SerialName("notification_id") val notificationId: String? = null,
    val dismiss: Boolean? = null,
    val all: Boolean? = null,
    @SerialName("festival_id") val festivalId: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// Response DTOs
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class OkResponse(
    val ok: Boolean = false,
    val error: String? = null
)

@Serializable
data class RegisterDeviceResponse(
    val ok: Boolean = false,
    val device: RegisteredDevice? = null,
    val error: String? = null
)

@Serializable
data class RegisteredDevice(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val platform: String? = null,
    val provider: String? = null,
    val enabled: Boolean? = null
)

@Serializable
data class NotificationSettingsResponse(
    val ok: Boolean = false,
    val settings: NotificationSettingsDto? = null,
    val error: String? = null
)

@Serializable
data class NotificationSettingsDto(
    @SerialName("festival_id") val festivalId: String? = null,
    @SerialName("push_enabled") val pushEnabled: Boolean = false,
    @SerialName("push_preference_enabled") val pushPreferenceEnabled: Boolean = false,
    @SerialName("push_configured") val pushConfigured: Boolean = false,
    @SerialName("push_device_count") val pushDeviceCount: Int = 0,
    @SerialName("emergency_alerts") val emergencyAlerts: Boolean = true,
    @SerialName("festival_updates") val festivalUpdates: Boolean = true,
    @SerialName("exclusive_promotions") val exclusivePromotions: Boolean = true,
    @SerialName("sms_enabled") val smsEnabled: Boolean = false,
    @SerialName("email_enabled") val emailEnabled: Boolean = false
)

@Serializable
data class ListNotificationsResponse(
    val ok: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("next_before") val nextBefore: String? = null,
    val error: String? = null
)

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("notification_type") val notificationType: String? = null,
    val title: String? = null,
    val body: String? = null,
    val channels: List<String> = emptyList(),
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("dismissed_at") val dismissedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
