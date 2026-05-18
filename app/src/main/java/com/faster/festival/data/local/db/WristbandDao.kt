package com.faster.festival.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WristbandDao {

    /** Live stream of the currently active wristband (null if none). */
    @Query("SELECT * FROM paired_wristband WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<PairedWristbandEntity?>

    /** One-shot fetch of the currently active wristband. */
    @Query("SELECT * FROM paired_wristband WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): PairedWristbandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PairedWristbandEntity)

    /** Mark all existing rows inactive — used before inserting a fresh pair. */
    @Query("UPDATE paired_wristband SET isActive = 0")
    suspend fun deactivateAll()

    /** Touched on every inbound telemetry / connection event. */
    @Query("UPDATE paired_wristband SET lastSeenAt = :ts WHERE isActive = 1")
    suspend fun updateLastSeen(ts: Long)

    @Query("DELETE FROM paired_wristband")
    suspend fun clear()
}
