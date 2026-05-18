package com.faster.festival.core.telemetry

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.faster.festival.data.remote.ApiError
import com.faster.festival.data.repository.local.TelemetryQueueRepository
import com.faster.festival.di.DatabaseModule
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Drains [TelemetryQueueRepository] into the Project 1
 * `functions/v1/wristband-telemetry-batch` endpoint.
 *
 *  • Pulls up to [TelemetryQueueRepository.MAX_BATCH_SIZE] (200) rows per
 *    `doWork()`.
 *  • Drains multiple batches per run while rows remain and uploads succeed.
 *  • Hands typed [ApiError] from [com.faster.festival.data.repository.local.WristbandRepository.uploadTelemetryBatch]
 *    into a three-way decision:
 *      - `retrySafe = true`  → return [Result.retry] (Network, Server 5xx)
 *      - `retrySafe = false` AND batch-shaped (PayloadTooLarge / Unprocessable) → drop the batch
 *      - 401 / 403          → retry — the underlying TokenRefreshInterceptor
 *        has already tried; one more pass via WorkManager backoff gives the
 *        user time to re-auth in foreground
 *  • Honors WorkManager's exponential backoff (10 s → ~5 h cap). `runAttemptCount`
 *    is checked to give up after [MAX_ATTEMPTS] so a permanently broken
 *    batch can't loop forever.
 *
 * **Project routing:** the underlying repository call hits Project 1 ONLY.
 * No path through this worker can reach Project 2.
 *
 * **Process-death safety:** rows live in Room. Worker re-runs after process
 * death because WorkManager keeps the request in its own database and
 * reissues it on next `WorkManager.getInstance(ctx)` boot.
 */
class TelemetryUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val queue: TelemetryQueueRepository get() = DatabaseModule.telemetryQueueRepository
    private val uploads = DatabaseModule.wristbandRepository

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d(
            "Run start — attempt=%d depth=%d",
            runAttemptCount, runCatching { queue.pendingCount() }.getOrDefault(-1)
        )

        // Enforce cap once per run so a long offline window doesn't grow
        // unbounded before we ever ship a batch.
        runCatching { queue.enforceCap() }.onFailure {
            Timber.tag(TAG).w(it, "enforceCap threw")
        }

        // Drain in a loop. Stop on empty queue, on a retry-safe failure
        // (let WorkManager back off), or on a non-retryable failure for a
        // batch we've already dropped.
        var batches = 0
        while (true) {
            val batch = queue.takeBatch()
            if (batch.isEmpty) {
                Timber.tag(TAG).d("Queue empty — finishing run after %d batch(es)", batches)
                return Result.success()
            }

            // Telemetry must be tagged with a wristband — if we don't have a
            // paired wristband locally, there is no recipient. We drop the
            // batch rather than retry: it would just keep failing.
            val active = uploads.getActiveOnce()
            if (active == null) {
                Timber.tag(TAG).w(
                    "No paired wristband — dropping %d-row batch", batch.size
                )
                queue.acknowledge(batch.ids)
                return Result.success()
            }

            val result = uploads.uploadTelemetryBatch(active.wristbandId, batch.snapshots)
            val outcome = decide(result, batch)
            when (outcome) {
                Decision.AckAndContinue -> {
                    queue.acknowledge(batch.ids)
                    batches++
                    if (batch.size < TelemetryQueueRepository.MAX_BATCH_SIZE) {
                        // Last partial slice — queue is drained.
                        Timber.tag(TAG).d("Partial drain — done")
                        return Result.success()
                    }
                }
                Decision.Drop -> {
                    queue.acknowledge(batch.ids)
                    batches++
                    Timber.tag(TAG).w(
                        "Dropping %d-row batch — non-retryable error", batch.size
                    )
                    // Continue trying subsequent batches: a single bad shape
                    // shouldn't stop the rest of the queue.
                }
                Decision.Retry -> {
                    if (runAttemptCount >= MAX_ATTEMPTS) {
                        Timber.tag(TAG).e(
                            "Retry budget exhausted (%d) — failing run", runAttemptCount
                        )
                        return Result.failure()
                    }
                    Timber.tag(TAG).i(
                        "Transient error — yielding to WorkManager backoff (attempt=%d)",
                        runAttemptCount
                    )
                    return Result.retry()
                }
            }
        }
    }

    private enum class Decision { AckAndContinue, Retry, Drop }

    private fun decide(
        result: kotlin.Result<*>,
        batch: TelemetryQueueRepository.TelemetryBatch
    ): Decision {
        if (result.isSuccess) return Decision.AckAndContinue
        val err = result.exceptionOrNull() ?: return Decision.Retry
        return when (err) {
            is ApiError.PayloadTooLarge,
            is ApiError.Unprocessable,
            is ApiError.BadRequest,
            is ApiError.Conflict -> Decision.Drop
            is ApiError.Network,
            is ApiError.Server -> Decision.Retry
            is ApiError.Unauthorized,
            is ApiError.Forbidden -> Decision.Retry
            is ApiError.NotFound -> Decision.Drop
            is ApiError -> if (err.retrySafe) Decision.Retry else Decision.Drop
            else -> Decision.Retry
        }.also {
            Timber.tag(TAG).w(
                err, "Upload error → %s (batch=%d)", it.name, batch.size
            )
        }
    }

    companion object {
        const val UNIQUE_PERIODIC = "telemetry-upload-periodic"
        const val UNIQUE_ONESHOT = "telemetry-upload-oneshot"

        private const val TAG = "TelemetryWorker"

        /** Beyond this attempt count the worker gives up and surfaces failure. */
        private const val MAX_ATTEMPTS = 10

        /** Connected network + battery-not-low is sufficient — telemetry is not urgent. */
        private val constraints: Constraints
            get() = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

        /**
         * Safety-net periodic worker — runs every 15 min (WorkManager's
         * minimum period) regardless of foreground activity. The collector's
         * early-flush enqueue handles steady-state streaming; this periodic
         * catches the case where the user leaves the app open with the
         * wristband disconnected for an extended period.
         */
        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<TelemetryUploadWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        /**
         * Trigger one immediate drain — called by the collector when the
         * queue depth crosses [TelemetryQueueRepository.EARLY_FLUSH_THRESHOLD].
         * KEEP policy means consecutive triggers coalesce into a single run.
         */
        fun triggerOneShot(context: Context) {
            val req = OneTimeWorkRequestBuilder<TelemetryUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                ExistingWorkPolicy.KEEP,
                req
            )
        }
    }
}
