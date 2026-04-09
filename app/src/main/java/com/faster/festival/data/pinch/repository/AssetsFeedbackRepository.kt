package com.faster.festival.data.pinch.repository

import com.faster.festival.data.pinch.local.FakeFeedbackApi
import com.faster.festival.data.pinch.model.FeedbackConfig
import com.faster.festival.data.pinch.model.FeedbackSubmission
import com.faster.festival.data.pinch.model.toDomain

class AssetsFeedbackRepository(
    private val fakeApi: FakeFeedbackApi
) : PinchFeedbackRepository {

    override suspend fun getFeedbackConfig(): Result<FeedbackConfig> {
        return try {
            val response = fakeApi.getFeedbackQuestions()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitFeedback(requestId: String, submission: FeedbackSubmission): Result<Unit> {
        return try {
            val result = fakeApi.submitFeedback(
                requestId = requestId,
                ratings = submission.ratings,
                overallRating = submission.overallRating,
                comment = submission.comment
            )
            if (result.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
