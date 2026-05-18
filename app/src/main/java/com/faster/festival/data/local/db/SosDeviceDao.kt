package com.faster.festival.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SosDeviceDao {

    /** Live stream of the currently active trusted device (null if none). */
    @Query("SELECT * FROM sos_registered_device WHERE active = 1 LIMIT 1")
    fun observeActive(): Flow<SosRegisteredDeviceEntity?>

    /** One-shot fetch of the currently active trusted device. */
    @Query("SELECT * FROM sos_registered_device WHERE active = 1 LIMIT 1")
    suspend fun getActive(): SosRegisteredDeviceEntity?

    @Query("SELECT * FROM sos_registered_device WHERE device_id = :deviceId LIMIT 1")
    suspend fun findByDeviceId(deviceId: String): SosRegisteredDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SosRegisteredDeviceEntity)

    /** Mark every other row inactive — single active device for Phase A. */
    @Query("UPDATE sos_registered_device SET active = 0 WHERE device_id != :keepDeviceId")
    suspend fun deactivateOthers(keepDeviceId: String)

    @Query(
        """
        UPDATE sos_registered_device
           SET registration_status = :status,
               raw_backend_payload = :payload,
               updated_at         = :timestamp,
               last_sync_at       = :timestamp
         WHERE device_id = :deviceId
        """
    )
    suspend fun updateRegistrationStatus(
        deviceId: String,
        status: String,
        payload: String?,
        timestamp: Long
    )

    @Query(
        """
        UPDATE sos_registered_device
           SET attestation_status = :status,
               verified           = :verified,
               verified_at        = :verifiedAt,
               raw_backend_payload = COALESCE(:payload, raw_backend_payload),
               updated_at         = :timestamp,
               last_sync_at       = :timestamp
         WHERE device_id = :deviceId
        """
    )
    suspend fun updateAttestationStatus(
        deviceId: String,
        status: String,
        verified: Boolean,
        verifiedAt: Long?,
        payload: String?,
        timestamp: Long
    )

    @Query("UPDATE sos_registered_device SET last_sync_at = :timestamp WHERE active = 1")
    suspend fun touchLastSync(timestamp: Long)

    @Query("DELETE FROM sos_registered_device")
    suspend fun clear()
}
