package com.faster.festival.data.repository.local

import com.faster.festival.data.local.db.SosHistoryDao
import com.faster.festival.data.local.db.SosHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UI-friendly domain model for an SOS/emergency history record.
 */
data class SosHistoryRecord(
    val id: Long,
    val requestId: String?,
    val createdAt: Long,
    val emergencyTypes: List<String>,
    val status: String,
    val locationText: String?,
    val coordinates: String?,
    val contactPhone: String?,
    val additionalInfo: String?,
    val triggerType: String
)

class SosHistoryRepository(private val dao: SosHistoryDao) {

    val history: Flow<List<SosHistoryRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): SosHistoryRecord? = dao.getById(id)?.toDomain()

    suspend fun recordSos(
        requestId: String?,
        emergencyTypes: List<String>,
        status: String,
        locationText: String?,
        coordinates: String?,
        contactPhone: String?,
        additionalInfo: String?,
        triggerType: String = "pinch_flow"
    ): Long {
        return dao.insert(
            SosHistoryEntity(
                requestId = requestId,
                createdAt = System.currentTimeMillis(),
                emergencyTypes = emergencyTypes.joinToString(","),
                status = status,
                locationText = locationText,
                coordinates = coordinates,
                contactPhone = contactPhone,
                additionalInfo = additionalInfo,
                triggerType = triggerType
            )
        )
    }

    suspend fun clear() = dao.clear()
    suspend fun count(): Int = dao.count()
}

private fun SosHistoryEntity.toDomain() = SosHistoryRecord(
    id = id,
    requestId = requestId,
    createdAt = createdAt,
    emergencyTypes = if (emergencyTypes.isBlank()) emptyList()
    else emergencyTypes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    status = status,
    locationText = locationText,
    coordinates = coordinates,
    contactPhone = contactPhone,
    additionalInfo = additionalInfo,
    triggerType = triggerType
)
