package com.faster.festival.data.pinch.repository

import com.faster.festival.data.pinch.local.FakeEmergencyApi
import com.faster.festival.data.pinch.model.EmergencyCategory
import com.faster.festival.data.pinch.model.EmergencyRequest
import com.faster.festival.data.pinch.model.TimelineConfig
import com.faster.festival.data.pinch.model.UserContext
import com.faster.festival.data.pinch.model.toDomain

class AssetsEmergencyRepository(
    private val fakeApi: FakeEmergencyApi
) : PinchEmergencyRepository {

    override suspend fun getEmergencyCategories(): Result<List<EmergencyCategory>> {
        return try {
            val response = fakeApi.getEmergencyCategories()
            Result.success(response.categories.map { it.toDomain() }.sortedBy { it.priority })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTimelineConfig(): Result<TimelineConfig> {
        return try {
            val response = fakeApi.getStatusTimeline()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserContext(): Result<UserContext> {
        return try {
            val response = fakeApi.getUserContext()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitEmergencyRequest(request: EmergencyRequest): Result<String> {
        return try {
            val result = fakeApi.submitEmergencyRequest(
                locationLabel = request.locationLabel,
                coordinates = request.coordinates,
                contactPhone = request.contactPhone,
                selectedCategoryIds = request.selectedCategoryIds,
                additionalInfo = request.additionalInfo,
                useCurrentLocation = request.useCurrentLocation
            )
            if (result.success) {
                Result.success(result.requestId)
            } else {
                Result.failure(Exception(result.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
