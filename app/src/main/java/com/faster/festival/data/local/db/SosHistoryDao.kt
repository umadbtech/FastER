package com.faster.festival.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SosHistoryDao {

    @Query("SELECT * FROM sos_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SosHistoryEntity>>

    @Query("SELECT * FROM sos_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SosHistoryEntity?

    /** Lookup by the backend alert id so remote-history sync can upsert. */
    @Query("SELECT * FROM sos_history WHERE requestId = :requestId LIMIT 1")
    suspend fun getByRequestId(requestId: String): SosHistoryEntity?

    @Insert
    suspend fun insert(entity: SosHistoryEntity): Long

    @Update
    suspend fun update(entity: SosHistoryEntity)

    @Query("DELETE FROM sos_history")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM sos_history")
    suspend fun count(): Int
}
