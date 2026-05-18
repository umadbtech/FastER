package com.faster.festival.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SosHistoryDao {

    @Query("SELECT * FROM sos_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SosHistoryEntity>>

    @Query("SELECT * FROM sos_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SosHistoryEntity?

    @Insert
    suspend fun insert(entity: SosHistoryEntity): Long

    @Query("DELETE FROM sos_history")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM sos_history")
    suspend fun count(): Int
}
