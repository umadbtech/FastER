package com.faster.festival.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload structure for email verification Realtime broadcast events.
 * Sent by the backend when a user's email is verified.
 */
@Serializable
data class VerificationPayload(
    @SerialName("user_id")
    val userId: String,
    @SerialName("email")
    val email: String,
    @SerialName("timestamp")
    val timestamp: String
)
