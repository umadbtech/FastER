package com.faster.festival.notifications

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.faster.festival.MainActivity
import com.faster.festival.R
import com.faster.festival.di.NotificationModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FasterMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FasterFCM"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        serviceScope.launch {
            NotificationModule.fcmTokenRegistrar.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "FASTER"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val channelId = resolveChannel(message.data["topic"] ?: message.from)

        showNotification(title, body, channelId)
    }

    private fun resolveChannel(topicOrSender: String?): String {
        return when {
            topicOrSender?.contains("emergency") == true -> NotificationChannelHelper.CHANNEL_EMERGENCY
            topicOrSender?.contains("festival") == true -> NotificationChannelHelper.CHANNEL_FESTIVAL
            topicOrSender?.contains("promotion") == true -> NotificationChannelHelper.CHANNEL_PROMOTIONS
            else -> NotificationChannelHelper.CHANNEL_GENERAL
        }
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channelId == NotificationChannelHelper.CHANNEL_EMERGENCY)
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }
}
