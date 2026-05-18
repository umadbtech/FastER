package com.faster.festival.data.repository.local

import com.faster.festival.data.local.db.PendingTelemetryDao
import com.faster.festival.data.local.db.PendingTelemetryEntity
import com.faster.festival.data.remote.TelemetrySnapshot
import com.faster.festival.wristband.domain.model.Telemetry
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for the BLE-to-backend telemetry queue.
 *
 *  • [enqueue] is called by the BLE collector for every inbound `0x10`
 *    packet. Dedup is enforced by the `(wristband_id, seq_num)` unique
 *    index on [PendingTelemetryEntity] — duplicates are silently dropped.
 *  • [takeBatch] grabs up to [MAX_BATCH_SIZE] = **200** oldest rows for
 *    upload — the server contract caps the request at 200.
 *  • [acknowledge] deletes successfully uploaded rows by primary key.
 *  • [enforceCap] sheds the oldest rows when the queue grows beyond
 *    [SOFT_CAP_ROWS] — telemetry is non-critical and we'd rather lose old
 *    samples than starve newer captures or OOM SQLite.
 *
 * Project routing: this repository hands batches up to [com.faster.festival.data.repository.local.WristbandRepository.uploadTelemetryBatch],
 * which POSTs to **Project 1 only** (`functions/v1/wristband-telemetry-batch`).
 * Telemetry NEVER goes to Project 2 — Project 2 is the signed dispatch
 * surface for SOS, not raw telemetry.
 */
class TelemetryQueueRepository(
    private val dao: PendingTelemetryDao
) {

    /**
     * Persist one reading. Returns `true` when the row was newly inserted —
     * the BLE collector uses this to decide whether to trigger an early
     * worker enqueue.
     */
    suspend fun enqueue(wristbandId: String, telemetry: Telemetry): Boolean {
        val rowId = dao.insertOne(
            PendingTelemetryEntity(
                wristband_id = wristbandId,
                seq_num = telemetry.seqNum,
                captured_at = telemetry.receivedAtMs,
                accel_x_g = telemetry.accelX_g,
                accel_y_g = telemetry.accelY_g,
                accel_z_g = telemetry.accelZ_g,
                peak_mag_g = telemetry.peakMag_g,
                motion = telemetry.motionDetected,
                battery_pct = telemetry.batteryPct,
                device_state = telemetry.deviceState.name
            )
        )
        val inserted = rowId != -1L
        if (!inserted) {
            Timber.tag(TAG).v(
                "Dedup drop — wb=%s seq=%d already queued",
                wristbandId.takeLast(6), telemetry.seqNum
            )
        }
        return inserted
    }

    /** Queue depth — used by the collector to gate the early-flush trigger. */
    suspend fun pendingCount(): Int = dao.count()

    /**
     * Drain up to [MAX_BATCH_SIZE] oldest rows. Returns the entities AND
     * their pre-mapped [TelemetrySnapshot]s ready for the Project 1 POST —
     * the worker hands the ids back via [acknowledge] on success.
     */
    suspend fun takeBatch(maxRows: Int = MAX_BATCH_SIZE): TelemetryBatch {
        val clamped = maxRows.coerceIn(1, MAX_BATCH_SIZE)
        val rows = dao.takeOldest(clamped)
        return TelemetryBatch(
            ids = rows.map { it.id },
            snapshots = rows.map { it.toSnapshot() }
        )
    }

    suspend fun acknowledge(ids: List<Long>) {
        if (ids.isEmpty()) return
        val removed = dao.deleteByIds(ids)
        Timber.tag(TAG).d("Drained batch — %d rows ack'd", removed)
    }

    /** Shed oldest rows over [SOFT_CAP_ROWS]; logs the count when it trims. */
    suspend fun enforceCap(): Int {
        val depth = dao.count()
        if (depth <= SOFT_CAP_ROWS) return 0
        val excess = depth - SOFT_CAP_ROWS
        val dropped = dao.dropOldest(excess)
        if (dropped > 0) {
            Timber.tag(TAG).w(
                "Queue cap exceeded — depth=%d cap=%d dropped=%d oldest rows",
                depth, SOFT_CAP_ROWS, dropped
            )
        }
        return dropped
    }

    suspend fun clearAll() {
        val removed = dao.clear()
        if (removed > 0) Timber.tag(TAG).i("Cleared %d pending rows", removed)
    }

    /** Result tuple from [takeBatch]. */
    data class TelemetryBatch(
        val ids: List<Long>,
        val snapshots: List<TelemetrySnapshot>
    ) {
        val isEmpty: Boolean get() = ids.isEmpty()
        val size: Int get() = ids.size
    }

    private fun PendingTelemetryEntity.toSnapshot() = TelemetrySnapshot(
        seqNum = seq_num,
        accelX = accel_x_g,
        accelY = accel_y_g,
        accelZ = accel_z_g,
        peakMag = peak_mag_g,
        motion = motion,
        batteryPct = battery_pct,
        deviceState = device_state,
        receivedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(captured_at))
    )

    companion object {
        /** Server-contract maximum per `functions/v1/wristband-telemetry-batch`. */
        const val MAX_BATCH_SIZE = 200

        /**
         * Soft cap on durable queue depth — beyond this we shed oldest rows.
         * 10k samples ≈ 2 h 47 min at 1 Hz. Comfortable margin for any
         * realistic offline window without letting Room sprawl.
         */
        const val SOFT_CAP_ROWS = 10_000

        /**
         * Pending count that triggers an early one-time worker enqueue from
         * the collector. Half of [MAX_BATCH_SIZE] so we send the first batch
         * promptly during steady-state streaming, without thrashing the
         * worker scheduler on every single packet.
         */
        const val EARLY_FLUSH_THRESHOLD = 100

        private const val TAG = "TelemetryQueue"
    }
}
