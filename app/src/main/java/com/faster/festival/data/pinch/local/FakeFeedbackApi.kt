package com.faster.festival.data.pinch.local

import com.faster.festival.data.pinch.model.FeedbackQuestionsResponse
import kotlinx.coroutines.delay

class FakeFeedbackApi(private val assetsReader: AssetsReader) {

    suspend fun getFeedbackQuestions(): FeedbackQuestionsResponse {
        delay(300)
        return assetsReader.readJson("pinch/feedback_questions.json")
    }

    suspend fun submitFeedback(
        requestId: String,
        ratings: Map<String, Int>,
        overallRating: Int,
        comment: String
    ): FeedbackResult {
        delay(600)
        return FeedbackResult(
            success = true,
            message = "Feedback submitted successfully"
        )
    }

    data class FeedbackResult(
        val success: Boolean,
        val message: String
    )
}
