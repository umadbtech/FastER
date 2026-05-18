package com.faster.festival.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable buffer for inbound `0x10` telemetry packets that haven't been
 * uploaded to Project 1 yet.
 *
 *  • Source of truth between the BLE collector and [TelemetryUploadWorker].
 *  • Survives process death — process crashes between the BLE notification
 *    and the upload don't lose readings.
 *  • Dedup key is `(wristband_id, seq_num)` — firmware monotonic seq + the
 *    wristband identity. ON CONFLICT IGNORE on insert means repeated
 *    publication of the same packet (e.g. on reconnect re-deliver) doesn't
 *    double-queue.
 *
 * Telemetry must ONLY land on Project 1 (`functions/v1/wristband-telemetry-batch`).
 * Project 2 hosts the signed dispatch surface; telemetry has no signing
 * envelope. The repository enforces this — the worker never picks the
 * Project 2 client.
 */
@Entity(
    tableName = "pending_telemetry",
    indices = [
        Index(value = ["wristband_id", "seq_num"], unique = true),
        Index(value = ["captured_at"])
    ]
)
data class PendingTelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Wristband identity at the time of capture. Same shape as `WristbandRepository.wristbandId`. */
    val wristband_id: String,

    /** Firmware-monotonic sequence number; part of the dedup key. */
    val seq_num: Int,

    /** Phone-side capture timestamp (epoch ms). Drives FIFO drain order. */
    val captured_at: Long,

    val accel_x_g: Float,
    val accel_y_g: Float,
    val accel_z_g: Float,
    val peak_mag_g: Float,
    val motion: Boolean,
    val battery_pct: Int,
    /** Stored as the enum name (`"Operational"`, `"SosActive"`, …). */
    val device_state: String
)
