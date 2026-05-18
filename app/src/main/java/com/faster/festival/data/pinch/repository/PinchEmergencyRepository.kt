package com.faster.festival.data.pinch.repository

import com.faster.festival.data.pinch.model.EmergencyCategory
import com.faster.festival.data.pinch.model.EmergencyRequest
import com.faster.festival.data.pinch.model.TimelineConfig
import com.faster.festival.data.pinch.model.UserContext

interface PinchEmergencyRepository {

    suspend fun getEmergencyCategories(): Result<List<EmergencyCategory>>

    suspend fun getTimelineConfig(): Result<TimelineConfig>

    suspend fun getUserContext(): Result<UserContext>

    suspend fun submitEmergencyRequest(request: EmergencyRequest): Result<String>
}
