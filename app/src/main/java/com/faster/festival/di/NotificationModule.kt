package com.faster.festival.di

import android.content.Context
import com.faster.festival.AppConfig
import com.faster.festival.notifications.FcmTokenRegistrar
import com.faster.festival.notifications.NotificationInboxRepository
import com.faster.festival.notifications.NotificationPreferencesManager
import com.faster.festival.notifications.NotificationRepository

object NotificationModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "NotificationModule not initialized. Call initialize(context) first."
        }

    private val preferencesManager: NotificationPreferencesManager by lazy {
        NotificationPreferencesManager(ctx)
    }

    val fcmTokenRegistrar: FcmTokenRegistrar by lazy {
        FcmTokenRegistrar(NetworkModule.notificationApi, ctx)
    }

    val repository: NotificationRepository by lazy {
        NotificationRepository(
            prefsManager = preferencesManager,
            notificationApi = NetworkModule.notificationApi,
            tokenRegistrar = fcmTokenRegistrar,
            festivalId = AppConfig.DEFAULT_FESTIVAL_ID
        )
    }

    val inboxRepository: NotificationInboxRepository by lazy {
        NotificationInboxRepository(
            notificationApi = NetworkModule.notificationApi,
            festivalId = AppConfig.DEFAULT_FESTIVAL_ID
        )
    }
}
