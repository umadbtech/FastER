package com.faster.festival.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelHelper {

    const val CHANNEL_GENERAL = "faster_general"
    const val CHANNEL_EMERGENCY = "faster_emergency"
    const val CHANNEL_FESTIVAL = "faster_festival"
    const val CHANNEL_PROMOTIONS = "faster_promotions"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            },
            NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts from medical staff"
                enableVibration(true)
                setBypassDnd(true)
            },
            NotificationChannel(
                CHANNEL_FESTIVAL,
                "Festival Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Festival schedule changes, announcements, and updates"
            },
            NotificationChannel(
                CHANNEL_PROMOTIONS,
                "Exclusive Promotions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Deals, offers, and exclusive promotions"
            }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }
}
