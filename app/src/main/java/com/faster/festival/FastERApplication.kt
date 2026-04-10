package com.faster.festival

import android.app.Application
import android.util.Log
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.di.NotificationModule
import com.faster.festival.notifications.NotificationChannelHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FastERApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Create notification channels (safe no-op on < Android 8)
        NotificationChannelHelper.createAllChannels(this)

        // Register FCM token with backend if user is logged in
        val sessionManager = EncryptedSessionManager(this)
        val hasToken = sessionManager.getAccessToken() != null

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FastERApp", "FCM Token: ${task.result}")
                if (hasToken) {
                    appScope.launch {
                        NotificationModule.fcmTokenRegistrar.registerCurrentToken()
                    }
                }
            } else {
                Log.w("FastERApp", "FCM token fetch failed", task.exception)
            }
        }
    }
}
