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

    /**
     * Upserts an authoritative alert pulled from `pinch-alert-history`, keyed by
     * [requestId] (the backend alert id). Updates the row in place when it
     * already exists (e.g. the local stub written at trigger time) so the list
     * reflects the latest backend `ui_status`/location instead of duplicating.
     *
     * Merge rule: backend-owned fields ([status]/[coordinates]/[createdAt]) are
     * authoritative; fields the history endpoint doesn't return ([emergencyTypes],
     * [contactPhone], [additionalInfo]) fall back to whatever was cached locally.
     */
    suspend fun upsertRemote(
        requestId: String,
        createdAt: Long,
        emergencyTypes: List<String>,
        status: String,
        locationText: String?,
        coordinates: String?,
        contactPhone: String?,
        additionalInfo: String?,
        triggerType: String
    ) {
        val existing = dao.getByRequestId(requestId)
        val emergencyCsv = emergencyTypes.joinToString(",")
        if (existing == null) {
            dao.insert(
                SosHistoryEntity(
                    requestId = requestId,
                    createdAt = createdAt,
                    emergencyTypes = emergencyCsv,
                    status = status,
                    locationText = locationText,
                    coordinates = coordinates,
                    contactPhone = contactPhone,
                    additionalInfo = additionalInfo,
                    triggerType = triggerType
                )
            )
        } else {
            dao.update(
                existing.copy(
                    createdAt = createdAt,
                    emergencyTypes = emergencyCsv.ifBlank { existing.emergencyTypes },
                    status = status,
                    locationText = locationText ?: existing.locationText,
                    coordinates = coordinates ?: existing.coordinates,
                    contactPhone = contactPhone ?: existing.contactPhone,
                    additionalInfo = additionalInfo ?: existing.additionalInfo,
                    triggerType = triggerType
                )
            )
        }
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
