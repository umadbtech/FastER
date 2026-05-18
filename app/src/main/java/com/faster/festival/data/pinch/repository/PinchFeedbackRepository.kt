package com.faster.festival.data.pinch.repository

import com.faster.festival.data.pinch.model.FeedbackConfig
import com.faster.festival.data.pinch.model.FeedbackSubmission

interface PinchFeedbackRepository {

    suspend fun getFeedbackConfig(): Result<FeedbackConfig>

    suspend fun submitFeedback(requestId: String, submission: FeedbackSubmission): Result<Unit>
}
