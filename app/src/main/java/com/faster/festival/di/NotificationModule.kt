package com.faster.festival.di

import android.content.Context
import com.faster.festival.notifications.FcmTokenRegistrar
import com.faster.festival.notifications.NotificationPreferencesManager
import com.faster.festival.notifications.NotificationRepository

object NotificationModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val preferencesManager: NotificationPreferencesManager by lazy {
        NotificationPreferencesManager(
            requireNotNull(appContext) { "NotificationModule not initialized. Call initialize(context) first." }
        )
    }

    val repository: NotificationRepository by lazy {
        NotificationRepository(preferencesManager)
    }

    val fcmTokenRegistrar: FcmTokenRegistrar by lazy {
        FcmTokenRegistrar(NetworkModule.notificationDeviceApi)
    }
}
