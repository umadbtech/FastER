package com.faster.festival.core.telemetry

import android.content.Context
import com.faster.festival.data.repository.local.TelemetryQueueRepository
import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.wristband.domain.usecase.ObserveTelemetryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App-scoped bridge between the BLE Mesh inbound `0x10` flow and the durable
 * [TelemetryQueueRepository]. Started exactly once from
 * `FASTERApplication.onCreate` via [start]; lives for the process lifetime.
 *
 * Flow:
 *   `WristbandMeshRepository.telemetry  →  Telemetry`
 *   `↓ tag with active wristband_id`
 *   `→ TelemetryQueueRepository.enqueue (Room INSERT OR IGNORE)`
 *   `→ if pending depth ≥ EARLY_FLUSH_THRESHOLD: trigger one-shot worker`
 *
 * The collector intentionally does NOT call the HTTP upload itself — that's
 * the worker's job. Putting Room between the BLE stack and the network call
 * is what gives us process-death recovery + offline persistence + the
 * dedup index (`wristband_id`, `seq_num`).
 *
 * Project routing: Project 1 is hardcoded by virtue of using
 * `WristbandRepository.uploadTelemetryBatch`. Telemetry NEVER reaches Project 2.
 */
class TelemetryCollector(
    private val context: Context,
    private val observeTelemetry: ObserveTelemetryUseCase,
    private val wristbandRepo: WristbandRepository,
    private val queue: TelemetryQueueRepository,
    private val scope: CoroutineScope
) {

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return

        // Always make sure the safety-net periodic worker is scheduled — it
        // is the only flush path when the collector isn't actively running
        // (e.g. wristband disconnected for hours but app open).
        TelemetryUploadWorker.schedulePeriodic(context)

        // If queue already had rows from a prior session, kick the worker
        // right away rather than waiting for the first new packet.
        scope.launch {
            val pending = runCatching { queue.pendingCount() }.getOrDefault(0)
            if (pending > 0) {
                Timber.tag(TAG).i(
                    "Cold start — %d pending row(s) from prior session, triggering drain",
                    pending
                )
                TelemetryUploadWorker.triggerOneShot(context)
            }
        }

        job = scope.launch {
            observeTelemetry().collect { telemetry ->
                val wristbandId = wristbandRepo.activeWristband.filterNotNull().first().wristbandId
                val inserted = runCatching { queue.enqueue(wristbandId, telemetry) }
                    .onFailure { Timber.tag(TAG).w(it, "enqueue threw") }
                    .getOrDefault(false)

                if (!inserted) return@collect

                val depth = runCatching { queue.pendingCount() }.getOrDefault(0)
                if (depth >= TelemetryQueueRepository.EARLY_FLUSH_THRESHOLD) {
                    Timber.tag(TAG).d(
                        "Depth %d ≥ threshold %d — triggering one-shot worker",
                        depth, TelemetryQueueRepository.EARLY_FLUSH_THRESHOLD
                    )
                    TelemetryUploadWorker.triggerOneShot(context)
                }
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
    }

    private companion object {
        const val TAG = "TelemetryCollector"
    }
}
