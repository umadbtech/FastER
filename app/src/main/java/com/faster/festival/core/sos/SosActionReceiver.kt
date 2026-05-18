package com.faster.festival.core.sos

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.faster.festival.di.SosModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Receives notification action taps from [SosNotifier]. Reachable from the
 * lockscreen — the user can cancel or resolve an SOS without unlocking or
 * opening the app.
 *
 *  • [ACTION_CANCEL]  → [EmergencySOSManager.cancelByUser]
 *  • [ACTION_RESOLVE] → [EmergencySOSManager.markResolved]
 *
 * Registered with `exported="false"` in the manifest so only this app can
 * fire it.
 *
 * Uses `goAsync()` to keep the receiver alive for the suspend call. Suspend
 * work runs on a dedicated [CoroutineScope] (not the main thread) to avoid
 * the 10 s broadcast deadline; once the work completes we invoke
 * `pending.finish()` so Android stops keeping the receiver alive.
 */
class SosActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Timber.tag(TAG).i("SOS action received: %s", action)

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        scope.launch {
            try {
                when (action) {
                    ACTION_CANCEL -> SosModule.emergencyManager.cancelByUser()
                    ACTION_RESOLVE -> SosModule.emergencyManager.markResolved(falseAlarm = false)
                    else -> Timber.tag(TAG).w("Unknown action: %s", action)
                }
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Action %s failed", action)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.faster.festival.sos.ACTION_CANCEL"
        const val ACTION_RESOLVE = "com.faster.festival.sos.ACTION_RESOLVE"

        private const val REQUEST_CANCEL = 0xF45EC1
        private const val REQUEST_RESOLVE = 0xF45EC2

        private const val TAG = "SosActionReceiver"

        fun cancelPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SosActionReceiver::class.java)
                .setAction(ACTION_CANCEL)
                .setPackage(context.packageName)
            return PendingIntent.getBroadcast(
                context, REQUEST_CANCEL, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun resolvePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SosActionReceiver::class.java)
                .setAction(ACTION_RESOLVE)
                .setPackage(context.packageName)
            return PendingIntent.getBroadcast(
                context, REQUEST_RESOLVE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
