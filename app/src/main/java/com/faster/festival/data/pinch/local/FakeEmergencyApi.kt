package com.faster.festival.data.pinch.local

import com.faster.festival.data.pinch.model.EmergencyCategoriesResponse
import com.faster.festival.data.pinch.model.MockUserContextResponse
import com.faster.festival.data.pinch.model.StatusTimelineResponse
import kotlinx.coroutines.delay

class FakeEmergencyApi(private val assetsReader: AssetsReader) {

    suspend fun getEmergencyCategories(): EmergencyCategoriesResponse {
        delay(300)
        return assetsReader.readJson("pinch/emergency_categories.json")
    }

    suspend fun getStatusTimeline(): StatusTimelineResponse {
        delay(200)
        return assetsReader.readJson("pinch/status_timeline.json")
    }

    suspend fun getUserContext(): MockUserContextResponse {
        delay(200)
        return assetsReader.readJson("pinch/mock_user_context.json")
    }

    suspend fun submitEmergencyRequest(
        locationLabel: String,
        coordinates: String,
        contactPhone: String,
        selectedCategoryIds: List<String>,
        additionalInfo: String?,
        useCurrentLocation: Boolean
    ): SubmitResult {
        delay(800)
        return SubmitResult(
            success = true,
            requestId = "REQ-${System.currentTimeMillis()}",
            message = "Emergency request submitted successfully"
        )
    }

    data class SubmitResult(
        val success: Boolean,
        val requestId: String,
        val message: String
    )
}
