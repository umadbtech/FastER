package com.faster.festival.core.sos

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.faster.festival.MainActivity
import com.faster.festival.R
import com.faster.festival.data.sos.SosLocationProvider
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.di.SosModule
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.notifications.NotificationChannelHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Keeps the SOS polling loop and `pinch-update-location` emitter alive while
 * an emergency is in flight — even with the screen off or the app swiped away.
 *
 * Lifecycle:
 *  • Started by [SosModule.startEmergencyOrchestration] (or directly from the
 *    coordinator when [EmergencySOSManager.state] first transitions to
 *    [EmergencySOSState.Sending]/[EmergencySOSState.Active]).
 *  • Calls [Service.startForeground] with type
 *    `LOCATION | CONNECTED_DEVICE` — the foreground notification piggybacks
 *    on the same id [SosNotifier] uses so the user sees one notification.
 *  • Observes [EmergencySOSManager.state] and stops itself on terminal
 *    states (`Idle / Resolved / Cancelled / Failed`).
 *  • A second coroutine inside the same scope drives the periodic location
 *    emitter (default 30 s) once dispatch has assigned a `tracking_session_id`.
 *
 * If the foreground-service start is rejected by the OS (Android 12+ BG start
 * restrictions, missing permissions, etc.) the SOS pipeline still works —
 * polling lives in [SosModule.emergencyManager]'s app-scope. The service is a
 * production-safety net, not a hard dependency.
 */
class ActiveSosForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationJob: Job? = null
    private var stopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).i("ActiveSosForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Initial placeholder notification — SosNotifier updates the same id
        // with the rich title/status as state transitions arrive. Calling
        // startForeground here within ~5s of the start intent is required
        // by Android 12+; we can't wait for the first SosNotifier post.
        val initial = buildPlaceholderNotification()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initial,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, initial)
            }
        }.onFailure {
            Timber.tag(TAG).w(it, "startForeground rejected — service will run without FG promotion")
        }

        startStateObserver()
        startLocationEmitter()
        return START_STICKY
    }

    private fun startStateObserver() {
        if (stopJob?.isActive == true) return
        stopJob = SosModule.emergencyManager.state
            .onEach { state ->
                when (state) {
                    EmergencySOSState.Idle,
                    EmergencySOSState.Preparing,
                    is EmergencySOSState.Resolved,
                    is EmergencySOSState.Cancelled,
                    is EmergencySOSState.Failed -> {
                        Timber.tag(TAG).i("Terminal state %s — stopping FG service",
                            state::class.simpleName)
                        stopSelf()
                    }
                    is EmergencySOSState.Sending,
                    is EmergencySOSState.Active -> Unit // keep running
                }
            }
            .launchIn(scope)
    }

    private fun startLocationEmitter() {
        if (locationJob?.isActive == true) return
        val locationProvider: SosLocationProvider = SosModule.locationProvider
        val repository: SosRepository = SosModule.repository

        locationJob = scope.launch {
            // Modest start delay so we don't fire the first update before
            // pinch-ingest itself has even hit the wire.
            delay(LOCATION_FIRST_DELAY_MS)
            while (true) {
                val state = SosModule.emergencyManager.state.value
                val session = when (state) {
                    is EmergencySOSState.Active -> state.session
                    is EmergencySOSState.Sending -> state.session
                    else -> null
                }
                val trackingId = session?.trackingSessionId
                if (session != null && !trackingId.isNullOrBlank()) {
                    runCatching {
                        val fix = locationProvider.currentFix()
                        if (fix != null) {
                            val loc = SosLocation(
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyMeters = fix.accuracy.toInt()
                            )
                            repository.sendLocationUpdate(
                                clientTriggerId = session.clientTriggerId,
                                trackingSessionId = trackingId,
                                location = loc
                            ).onFailure {
                                Timber.tag(TAG).w(
                                    it, "pinch-update-location failed for trigger=%s",
                                    session.clientTriggerId.takeLast(8)
                                )
                            }
                        } else {
                            Timber.tag(TAG).d("No location fix this tick — skipping push")
                        }
                    }.onFailure {
                        Timber.tag(TAG).w(it, "Location emitter tick threw")
                    }
                }
                delay(LOCATION_INTERVAL_MS)
            }
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).i("ActiveSosForegroundService destroyed")
        locationJob?.cancel()
        stopJob?.cancel()
        scope.cancel()
        // STOP_FOREGROUND_DETACH keeps any terminal-state notification visible
        // (e.g. "SOS resolved") after the service exits.
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

    private fun buildPlaceholderNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationChannelHelper.CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SOS active")
            .setContentText("Connecting to dispatch.")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val TAG = "ActiveSosFgSvc"

        /** Same id as [SosNotifier.NOTIFICATION_ID] — keeps a single notification. */
        const val NOTIFICATION_ID = 0xF45E12
        private const val REQUEST_CODE = 0xF45F

        /** First location push fires this long after the FG service starts. */
        private const val LOCATION_FIRST_DELAY_MS = 10_000L

        /** Spec default — periodic GPS push every 30 s. */
        private const val LOCATION_INTERVAL_MS = 30_000L

        fun ensureRunning(context: Context) {
            val intent = Intent(context, ActiveSosForegroundService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure {
                    Timber.tag(TAG).w(it, "startForegroundService rejected — likely BG start restriction")
                }
        }

        fun ensureStopped(context: Context) {
            val intent = Intent(context, ActiveSosForegroundService::class.java)
            runCatching { context.stopService(intent) }
        }
    }
}
