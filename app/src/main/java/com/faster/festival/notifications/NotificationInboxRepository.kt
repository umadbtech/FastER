package com.faster.festival.notifications

import android.util.Log
import com.faster.festival.data.remote.MarkNotificationReadRequest
import com.faster.festival.data.remote.NotificationApi
import com.faster.festival.data.remote.NotificationDto

/**
 * Domain model for an inbox notification (UI-friendly).
 */
data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val channels: List<String>,
    val isRead: Boolean,
    val isDismissed: Boolean,
    val createdAt: String?
)

data class NotificationInbox(
    val items: List<NotificationItem>,
    val unreadCount: Int,
    val nextBefore: String?
)

class NotificationInboxRepository(
    private val notificationApi: NotificationApi,
    private val festivalId: String
) {

    companion object {
        private const val TAG = "NotificationInboxRepo"
    }

    /**
     * Fetch the inbox notifications for display in the notifications screen.
     */
    suspend fun listNotifications(limit: Int = 20, before: String? = null): Result<NotificationInbox> {
        return try {
            val response = notificationApi.listNotifications(
                festivalId = festivalId,
                limit = limit,
                before = before
            )
            if (response.isSuccessful && response.body()?.ok == true) {
                val body = response.body()!!
                val inbox = NotificationInbox(
                    items = body.notifications.map { it.toDomain() },
                    unreadCount = body.unreadCount,
                    nextBefore = body.nextBefore
                )
                Result.success(inbox)
            } else {
                val msg = response.body()?.error ?: "HTTP ${response.code()}"
                Log.e(TAG, "listNotifications failed: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "listNotifications failed", e)
            Result.failure(e)
        }
    }

    /** Mark a single notification as read. */
    suspend fun markRead(notificationId: String): Result<Unit> =
        sendMarkRequest(MarkNotificationReadRequest(notificationId = notificationId))

    /** Dismiss a notification. */
    suspend fun dismiss(notificationId: String): Result<Unit> =
        sendMarkRequest(
            MarkNotificationReadRequest(
                notificationId = notificationId,
                dismiss = true
            )
        )

    /** Mark all notifications as read for the current festival. */
    suspend fun markAllRead(): Result<Unit> =
        sendMarkRequest(
            MarkNotificationReadRequest(
                all = true,
                festivalId = festivalId
            )
        )

    private suspend fun sendMarkRequest(request: MarkNotificationReadRequest): Result<Unit> {
        return try {
            val response = notificationApi.markNotificationRead(request)
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(Unit)
            } else {
                val msg = response.body()?.error ?: "HTTP ${response.code()}"
                Log.e(TAG, "markNotificationRead failed: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "markNotificationRead failed", e)
            Result.failure(e)
        }
    }
}

private fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = id,
    type = notificationType ?: "unknown",
    title = title ?: "",
    body = body ?: "",
    channels = channels,
    isRead = readAt != null,
    isDismissed = dismissedAt != null,
    createdAt = createdAt
)
