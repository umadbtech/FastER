package com.faster.festival

import android.app.Application
import android.util.Log
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.di.DatabaseModule
import com.faster.festival.di.NetworkModule
import com.faster.festival.di.NotificationModule
import com.faster.festival.di.PinchModule
import com.faster.festival.di.SosModule
import com.faster.festival.di.TelemetryModule
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
        runCatching {
            com.faster.festival.di.ConnectivityModule.initialize(applicationContext)
            // Touch the lazy property so the NetworkCallback registers eagerly,
            // before the first screen paints.
            com.faster.festival.di.ConnectivityModule.networkMonitor.current
        }.onFailure { Log.e(TAG, "ConnectivityModule init failed", it) }

        // Wristband / BLE Mesh module.
        // • Debug builds default to FakeMeshManager so previews / instrumented
        //   tests work without the wristband.
        // • Release builds use the real Nordic-backed NordicMeshManager.
        // Override at runtime if you need to force real BLE in debug — call
        //   WristbandModule.useFakeMesh = false BEFORE the first access.
        runCatching {
            com.faster.festival.wristband.di.WristbandModule.initialize(applicationContext)
            com.faster.festival.wristband.di.WristbandModule.useFakeMesh =
                com.faster.festival.BuildConfig.DEBUG
        }.onFailure { Log.e(TAG, "WristbandModule init failed", it) }

        // Telemetry pipeline — BLE 0x10 → Room queue → WorkManager → Project 1.
        // MUST be initialized AFTER WristbandModule (BLE source) and
        // DatabaseModule (queue store) but is independent of SOS. Safe to
        // start unconditionally; the collector no-ops until a wristband
        // becomes active.
        runCatching {
            TelemetryModule.initialize(applicationContext)
            TelemetryModule.telemetryCollector.start()
        }.onFailure { Log.e(TAG, "TelemetryModule init failed", it) }

        // SOS trusted-device flow — silent bootstrap. Setup runs only when
        // the user is logged in AND has an active attendee membership; safe
        // to call here unconditionally because SOSSetupManager guards both.
        runCatching {
            sessionManager?.let {
                SosModule.initialize(applicationContext, it)
                // Plant a Timber tree so the security/canonical/polling logs
                // surface in logcat without piping through Log.* call sites.
                if (BuildConfig.DEBUG && timber.log.Timber.treeCount == 0) {
                    timber.log.Timber.plant(timber.log.Timber.DebugTree())
                }
                SosModule.setupManager.ensureSetup()
                // Wire wristband 0x11/0x12 → EmergencySOSManager, and resume
                // any in-flight SOS that was active when the process died.
                SosModule.startEmergencyOrchestration()
            }
        }.onFailure { Log.e(TAG, "SosModule init failed", it) }

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
