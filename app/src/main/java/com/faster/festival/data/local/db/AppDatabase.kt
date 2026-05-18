package com.faster.festival.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PairedWristbandEntity::class,
        SosHistoryEntity::class,
        SosRegisteredDeviceEntity::class,
        PendingTelemetryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wristbandDao(): WristbandDao
    abstract fun sosHistoryDao(): SosHistoryDao
    abstract fun sosDeviceDao(): SosDeviceDao
    abstract fun pendingTelemetryDao(): PendingTelemetryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * Non-destructive v1 → v2 migration. Pure ALTER TABLE ADD COLUMN —
         * existing wristbandId / deviceName / firmwareVersion / batteryLevel /
         * connectionStatus / pairedAt / isActive are preserved.
         * 49152 == 0xC000 (default mesh group address).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE paired_wristband ADD COLUMN unicastAddress INTEGER")
                db.execSQL(
                    "ALTER TABLE paired_wristband ADD COLUMN groupAddress INTEGER NOT NULL DEFAULT 49152"
                )
                db.execSQL("ALTER TABLE paired_wristband ADD COLUMN lastSeenAt INTEGER")
            }
        }

        /**
         * v2 → v3: adds `sos_registered_device` table — the canonical audit
         * record for the SOS trusted-device registration with the backend.
         * Hot-path `device_id` + `attestation_verified` boolean continue to
         * live in DataStore (`DeviceIdentityStore`) so the signed
         * `pinch-ingest` flow doesn't hit Room. This table is the long-term
         * record with extended status fields, timestamps, and the raw
         * backend payload for support / debugging.
         *
         * Pure CREATE TABLE — no existing data touched. On fresh installs
         * Room generates this from the @Entity annotation directly; the
         * migration covers upgrade-from-v2 only.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sos_registered_device (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        device_id           TEXT    NOT NULL,
                        platform            TEXT    NOT NULL,
                        app_id              TEXT    NOT NULL,
                        app_version         TEXT    NOT NULL,
                        public_key          TEXT    NOT NULL,
                        key_algorithm       TEXT    NOT NULL,
                        registration_status TEXT    NOT NULL,
                        attestation_status  TEXT    NOT NULL,
                        verified            INTEGER NOT NULL,
                        verified_at         INTEGER,
                        created_at          INTEGER NOT NULL,
                        updated_at          INTEGER NOT NULL,
                        last_sync_at        INTEGER,
                        raw_backend_payload TEXT,
                        active              INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sos_registered_device_device_id " +
                            "ON sos_registered_device(device_id)"
                )
            }
        }

        /**
         * v3 → v4: adds `pending_telemetry` — the durable buffer for inbound
         * `0x10` packets between the BLE collector and [TelemetryUploadWorker].
         * Pure CREATE TABLE, no existing data touched.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_telemetry (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        wristband_id TEXT    NOT NULL,
                        seq_num      INTEGER NOT NULL,
                        captured_at  INTEGER NOT NULL,
                        accel_x_g    REAL    NOT NULL,
                        accel_y_g    REAL    NOT NULL,
                        accel_z_g    REAL    NOT NULL,
                        peak_mag_g   REAL    NOT NULL,
                        motion       INTEGER NOT NULL,
                        battery_pct  INTEGER NOT NULL,
                        device_state TEXT    NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_telemetry_wb_seq " +
                        "ON pending_telemetry(wristband_id, seq_num)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_telemetry_captured_at " +
                        "ON pending_telemetry(captured_at)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "faster.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()             // safety net only
                    .build()
                    .also { instance = it }
            }
        }
    }
}
