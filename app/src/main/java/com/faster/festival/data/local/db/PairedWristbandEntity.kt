package com.faster.festival.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Locally persisted paired wristband. Only one row is expected (the currently
 * active wristband) — the app replaces any existing row on successful re-pair.
 *
 * v2 schema (see [AppDatabase.MIGRATION_1_2]) adds three columns so the
 * dashboard / reconnect logic can recover the BLE Mesh unicast address:
 *   • [unicastAddress]  null on legacy v1 rows from the simulated walkthrough
 *   • [groupAddress]    NOT NULL DEFAULT 49152 (== 0xC000)
 *   • [lastSeenAt]      last telemetry / connection timestamp
 */
@Entity(tableName = "paired_wristband")
data class PairedWristbandEntity(
    @PrimaryKey val wristbandId: String,
    val deviceName: String?,
    val firmwareVersion: String?,
    val batteryLevel: Int?,
    val connectionStatus: String,
    val pairedAt: Long,
    val isActive: Boolean = true,
    val unicastAddress: Int? = null,
    val groupAddress: Int = 0xC000,
    val lastSeenAt: Long? = null
)
