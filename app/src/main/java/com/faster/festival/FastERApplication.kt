package com.faster.festival

import android.app.Application
import android.util.Log
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.di.DatabaseModule
import com.faster.festival.di.NetworkModule
import com.faster.festival.di.NotificationModule
import com.faster.festival.di.PinchModule
import com.faster.festival.notifications.NotificationChannelHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FASTERApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize all DI modules eagerly in Application.onCreate BEFORE any
        // async callback (like FCM) may fire. Wrapped in try/catch so that a bad
        // persisted state or failing sub-initializer can never crash-loop the app.
        val sessionManager = try {
            EncryptedSessionManager(applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to init EncryptedSessionManager; clearing prefs and retrying", t)
            runCatching {
                applicationContext
                    .getSharedPreferences("festival_auth_prefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }
            try {
                EncryptedSessionManager(applicationContext)
            } catch (t2: Throwable) {
                Log.e(TAG, "EncryptedSessionManager still failing after reset", t2)
                null
            }
        }

        runCatching { NetworkModule.initializeWithSessionManager(sessionManager ?: EncryptedSessionManager(applicationContext)) }
            .onFailure { Log.e(TAG, "NetworkModule init failed", it) }
        runCatching { PinchModule.initialize(applicationContext) }
            .onFailure { Log.e(TAG, "PinchModule init failed", it) }
        runCatching { NotificationModule.initialize(applicationContext) }
            .onFailure { Log.e(TAG, "NotificationModule init failed", it) }
        runCatching { DatabaseModule.initialize(applicationContext) }
            .onFailure { Log.e(TAG, "DatabaseModule init failed", it) }

        // Create notification channels (safe no-op on < Android 8)
        runCatching { NotificationChannelHelper.createAllChannels(this) }
            .onFailure { Log.e(TAG, "Notification channel create failed", it) }

        // Register FCM token with backend if user is logged in.
        // Entire callback is guarded so that FCM/Play Services failures can never crash the process.
        val hasToken = sessionManager?.getAccessToken() != null
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        Log.d(TAG, "FCM Token: ${task.result}")
                        if (hasToken) {
                            appScope.launch {
                                runCatching { NotificationModule.fcmTokenRegistrar.registerCurrentToken() }
                                    .onFailure { Log.e(TAG, "FCM token register failed", it) }
                            }
                        }
                    } else {
                        Log.w(TAG, "FCM token fetch failed", task.exception)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "FCM completion handler threw", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "FirebaseMessaging.getInstance() threw", t)
        }
    }

    companion object {
        private const val TAG = "FASTERApp"
    }
}
