package com.faster.festival.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendshipRequest(
    @SerialName("friend_user_id") val friendUserId: String
)

@Serializable
data class FriendshipResponse(
    val id: String,
    val status: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("responder_id") val responderId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class FriendshipRespondRequest(
    @SerialName("friendship_id") val friendshipId: String,
    val action: String // "accept" or "reject"
)

@Serializable
data class FriendSearchResult(
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("legal_first_name") val legalFirstName: String? = null,
    @SerialName("legal_last_name") val legalLastName: String? = null
)

@Serializable
data class FriendSearchResponse(
    val ok: Boolean,
    val results: List<FriendSearchResult> = emptyList()
)

@Serializable
data class FriendshipListResponse(
    val ok: Boolean,
    val friends: List<FriendshipResponse> = emptyList(),
    val pending: List<FriendshipResponse> = emptyList(),
    val requests: List<FriendshipResponse> = emptyList()
)
