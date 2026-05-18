package com.faster.festival.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTelemetryDao {

    /**
     * Insert one reading; duplicates on the unique `(wristband_id, seq_num)`
     * index are silently dropped. Returns the new rowid, or -1 if a duplicate
     * was suppressed — useful for the collector's "this was new" log line.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOne(entity: PendingTelemetryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PendingTelemetryEntity>): List<Long>

    /** Total queue depth — used by the collector to decide when to kick the worker early. */
    @Query("SELECT COUNT(*) FROM pending_telemetry")
    suspend fun count(): Int

    /**
     * Oldest-first batch slice. Limit is clamped to the spec maximum (200) at
     * the caller; this DAO doesn't enforce it so unit tests can drain smaller
     * batches.
     */
    @Query("SELECT * FROM pending_telemetry ORDER BY captured_at ASC, id ASC LIMIT :limit")
    suspend fun takeOldest(limit: Int): List<PendingTelemetryEntity>

    /** Delete exactly the rows we successfully uploaded. */
    @Query("DELETE FROM pending_telemetry WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    /**
     * Cap enforcement — when queue overflows the soft cap, the oldest rows
     * get dropped. Telemetry is non-critical; we'd rather shed old samples
     * than starve newer captures during a long offline window.
     */
    @Query(
        """
        DELETE FROM pending_telemetry
        WHERE id IN (
            SELECT id FROM pending_telemetry
            ORDER BY captured_at ASC, id ASC
            LIMIT :howMany
        )
        """
    )
    suspend fun dropOldest(howMany: Int): Int

    @Query("DELETE FROM pending_telemetry")
    suspend fun clear(): Int
}
