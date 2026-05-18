package com.faster.festival.core.sos

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.faster.festival.MainActivity
import com.faster.festival.R
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.notifications.NotificationChannelHelper
import timber.log.Timber

/**
 * Posts and updates the SOS foreground-style notification on the existing
 * `faster_emergency` channel (IMPORTANCE_HIGH + vibration + DND bypass — see
 * [NotificationChannelHelper]).
 *
 *  • Lockscreen visible (`VISIBILITY_PUBLIC`) — the user can see the SOS
 *    status without unlocking.
 *  • Full-screen intent on the initial post (`Sending`) so a screen-off /
 *    locked device wakes up to the alert. Requires `USE_FULL_SCREEN_INTENT`
 *    in the manifest.
 *  • Ongoing while in flight — can't be swiped away by accident. Auto-clears
 *    only on terminal states.
 *  • Updates in place via the same notification id so we don't get a stack
 *    of stale alerts as `pinch-alert-status` transitions arrive.
 */
class SosNotifier(private val context: Context) {

    private val nm: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun showSending(session: ActiveSOSSession) = post(
        title = "SOS sending — getting help…",
        text = "Connecting to dispatch.",
        ongoing = true,
        useFullScreen = true,
        showActions = true,
        session = session
    )

    fun showActive(
        session: ActiveSOSSession,
        userStatus: SosUserStatus,
        responderName: String?,
        etaMinutes: Int?
    ) {
        val title = when (userStatus) {
            SosUserStatus.SosReceived -> "SOS received"
            SosUserStatus.DispatchReceived,
            SosUserStatus.DispatchConfirmed -> "Dispatch confirmed"
            SosUserStatus.ResponderAssigned,
            SosUserStatus.ResponderAccepted -> "Responder assigned"
            SosUserStatus.ResponderEnRoute -> "Responder is on the way"
            SosUserStatus.ResponderOnScene -> "Responder is on scene"
            else -> "Help is on the way"
        }
        val parts = buildList {
            add(userStatus.displayText)
            responderName?.let { add(it) }
            etaMinutes?.let { add("ETA: $it min") }
        }
        post(
            title = title,
            text = parts.joinToString(" • "),
            ongoing = true,
            useFullScreen = false,
            showActions = true,
            session = session
        )
    }

    fun showResolved(session: ActiveSOSSession, terminalStatus: SosUserStatus) {
        val title = when (terminalStatus) {
            SosUserStatus.Resolved -> "SOS resolved"
            SosUserStatus.Closed -> "SOS closed"
            else -> "SOS closed"
        }
        post(
            title = title,
            text = "You're safe — emergency cleared.",
            ongoing = false,
            useFullScreen = false,
            showActions = false,
            session = session
        )
    }

    fun showCancelled() = post(
        title = "SOS cancelled",
        text = "The emergency was cancelled.",
        ongoing = false,
        useFullScreen = false,
        showActions = false,
        session = null
    )

    fun showFailed(message: String) = post(
        title = "SOS could not be sent",
        text = message,
        ongoing = false,
        useFullScreen = false,
        showActions = false,
        session = null
    )

    /** Tear down the sticky notification (e.g. user dismissed terminal state). */
    fun clear() {
        runCatching { nm.cancel(NOTIFICATION_ID) }
    }

    // ─── internals ─────────────────────────────────────────────────────────

    private fun post(
        title: String,
        text: String,
        ongoing: Boolean,
        useFullScreen: Boolean,
        showActions: Boolean,
        session: ActiveSOSSession?
    ) {
        if (!hasPostPermission()) {
            Timber.tag(TAG).d("POST_NOTIFICATIONS not granted — skipping SOS notification")
            return
        }

        val contentIntent = buildContentIntent()
        val builder = NotificationCompat.Builder(context, NotificationChannelHelper.CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(EMERGENCY_RED)
            .setColorized(true)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(false)
            .setVibrate(VIBRATION_PATTERN)
            .setContentIntent(contentIntent)

        if (useFullScreen) {
            builder.setFullScreenIntent(contentIntent, /* highPriority */ true)
        }

        if (showActions) {
            // Resolve first — surfaces as the primary green-tinted action on
            // most launchers because it's the positive outcome.
            builder.addAction(
                NotificationCompat.Action.Builder(
                    /* icon */ 0,
                    "Mark Resolved",
                    SosActionReceiver.resolvePendingIntent(context)
                ).build()
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    /* icon */ 0,
                    "Cancel SOS",
                    SosActionReceiver.cancelPendingIntent(context)
                ).build()
            )
        }

        runCatching { nm.notify(NOTIFICATION_ID, builder.build()) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to post SOS notification") }
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasPostPermission(): Boolean {
        // POST_NOTIFICATIONS runtime permission only matters on API 33+.
        if (android.os.Build.VERSION.SDK_INT < 33) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG = "SosNotifier"
        const val NOTIFICATION_ID = 0xF45E12      // stable id — updates in place
        const val REQUEST_CODE = 0xF45E

        // Foreground / accent red — matches the SOS overlay.
        val EMERGENCY_RED = android.graphics.Color.parseColor("#B3261E")

        // Stronger vibration than channel default — short attention buzz.
        val VIBRATION_PATTERN = longArrayOf(0, 600, 250, 600, 250, 600)
    }

    // Compose-callable wiring — kept here so the SosModule doesn't import
    // android.app.NotificationManager.
    @Suppress("unused")
    fun isEmergencyChannelMuted(): Boolean {
        val sys = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return sys.getNotificationChannel(NotificationChannelHelper.CHANNEL_EMERGENCY)
            ?.importance == NotificationManager.IMPORTANCE_NONE
    }
}
