package com.faster.festival.core.sos

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.faster.festival.data.sos.isSosRetryable
import com.faster.festival.di.SosModule
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Durable retry path for `pinch-ingest`. Picks up a [ActiveSOSSession] whose
 * `alertId` is still null (dispatch never completed) and re-issues the
 * signed POST via the existing [SosRepositoryImpl.triggerSos][com.faster.festival.data.sos.SosRepositoryImpl.triggerSos]
 * — same `client_trigger_id`, fresh nonce + timestamp + signature.
 *
 * Triggered by:
 *  • [EmergencySOSManager.sendBackend] failure path (inline 3-attempt budget exhausted)
 *  • [EmergencySOSManager.resumeIfPersisted] when a persisted session has no `alertId`
 *    (process death between persist and the 2xx response)
 *
 * Constraints:
 *  • `NetworkType.CONNECTED` — there is no point retrying offline
 *  • Backoff: exponential, start 30 s, cap ~5 h (WorkManager ceiling)
 *  • Single-flight via `UNIQUE_WORK_NAME` + `ExistingWorkPolicy.KEEP` so a
 *    cascade of failures doesn't fan out into N parallel workers
 *
 * Idempotency: `client_trigger_id` is reused across every retry. The server
 * dedups so duplicate dispatch attempts produce the same `alert_id`.
 *
 * **Project routing:** the worker calls `SosRepository.triggerSos` which is
 * hard-wired to Project 2 `pinch-ingest`. Telemetry never reaches this path
 * and this path never reaches telemetry — separate worker, separate queue.
 */
class SosDispatchRetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val sessionStore = SosModule.activeSessionStore
    private val triggerSos = SosModule.triggerSos
    private val locationProvider = SosModule.locationProvider
    private val manager = SosModule.emergencyManager
    private val pairedRepo = com.faster.festival.di.DatabaseModule.wristbandRepository

    override suspend fun doWork(): Result {
        Timber.tag(TAG).i("Run start — attempt=%d", runAttemptCount)

        val session = runCatching { sessionStore.load() }.getOrNull()
        if (session == null) {
            Timber.tag(TAG).d("No persisted session — nothing to do")
            return Result.success()
        }
        if (!session.alertId.isNullOrBlank()) {
            Timber.tag(TAG).d(
                "Session already dispatched (alert=%s) — nothing to do",
                session.alertId.takeLast(8)
            )
            return Result.success()
        }

        // Recompute location + wristband info FRESH per attempt — a 15-min
        // delay can make the originally captured fix stale.
        val location = locationProvider.currentFix()?.let {
            com.faster.festival.data.sos.remote.SosLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = it.accuracy.toInt()
            )
        }
        val wristband = buildWristbandInfo(session)

        val result = triggerSos(
            clientTriggerId = session.clientTriggerId,
            festivalId = session.festivalId,
            location = location,
            wristband = wristband,
            // Inline use case retries 3 times itself; we keep that for the
            // burst-recovery case, then WorkManager handles longer waits.
            maxAttempts = 3
        )

        return result.fold(
            onSuccess = { handle ->
                Timber.tag(TAG).i(
                    "Worker re-dispatch OK — trigger=%s alert=%s",
                    session.clientTriggerId.takeLast(8),
                    handle.alertId?.takeLast(8) ?: "-"
                )
                // Notify the manager so it transitions Failed/Sending → Active
                // and starts polling. Manager owns persistence + state.
                manager.onDispatchAcknowledged(
                    clientTriggerId = handle.clientTriggerId,
                    alertId = handle.alertId,
                    initialStatus = handle.initialStatus
                )
                Result.success()
            },
            onFailure = { err ->
                if (err.isSosRetryable() && runAttemptCount < MAX_ATTEMPTS) {
                    Timber.tag(TAG).i(
                        err, "Retryable failure — yielding to WorkManager backoff"
                    )
                    Result.retry()
                } else {
                    Timber.tag(TAG).e(
                        err, "Non-retryable / budget exhausted (attempt=%d)",
                        runAttemptCount
                    )
                    manager.onDispatchExhausted(
                        clientTriggerId = session.clientTriggerId,
                        message = err.localizedMessage ?: "SOS could not be sent."
                    )
                    Result.failure()
                }
            }
        )
    }

    private suspend fun buildWristbandInfo(
        session: ActiveSOSSession
    ): com.faster.festival.data.sos.remote.WristbandInfo {
        val paired = pairedRepo.getActiveOnce()
        return when (session.source) {
            SosSource.Wristband -> com.faster.festival.data.sos.remote.WristbandInfo(
                wristbandId = paired?.wristbandId ?: session.wristbandEvent?.let { wb ->
                    "FSTR-%04X".format(0) // placeholder; we never expect paired to be null mid-emergency
                },
                connectionState = if (paired != null) "connected" else "unknown",
                batteryPercent = session.wristbandEvent?.batteryPct ?: paired?.batteryLevel
            )
            SosSource.Manual -> {
                if (paired == null) {
                    com.faster.festival.data.sos.remote.WristbandInfo(
                        wristbandId = null,
                        connectionState = "mobile_only",
                        batteryPercent = null
                    )
                } else {
                    com.faster.festival.data.sos.remote.WristbandInfo(
                        wristbandId = paired.wristbandId,
                        connectionState = "connected",
                        batteryPercent = paired.batteryLevel
                    )
                }
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sos-dispatch-retry"
        private const val TAG = "SosDispatchRetryWorker"
        private const val MAX_ATTEMPTS = 20

        private val constraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        /**
         * Enqueue exactly one retry attempt. KEEP policy coalesces multiple
         * triggers (e.g. inline failure AND resumeIfPersisted both firing)
         * into a single worker run.
         */
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<SosDispatchRetryWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                req
            )
            Timber.tag(TAG).d("Worker enqueued (KEEP policy)")
        }

        /** Cancel any pending retry — used when the session terminates. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
