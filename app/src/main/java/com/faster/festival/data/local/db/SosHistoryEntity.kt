package com.faster.festival.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Locally persisted SOS / emergency event record.
 * Created from PinchHelpViewModel when the user submits an emergency request,
 * and displayed on the SOS History screen.
 */
@Entity(tableName = "sos_history")
data class SosHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String?,
    val createdAt: Long,
    val emergencyTypes: String,
    val status: String,
    val locationText: String?,
    val coordinates: String?,
    val contactPhone: String?,
    val additionalInfo: String?,
    val triggerType: String
)
