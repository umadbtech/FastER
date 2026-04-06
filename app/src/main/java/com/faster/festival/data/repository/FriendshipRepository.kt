package com.faster.festival.data.repository

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.FriendSearchResponse
import com.faster.festival.data.model.FriendshipListResponse
import com.faster.festival.data.model.FriendshipRequest
import com.faster.festival.data.model.FriendshipRespondRequest
import com.faster.festival.data.model.FriendshipResponse
import com.faster.festival.data.remote.FriendshipApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FriendshipRepository(
    private val api: FriendshipApiService,
    private val sessionManager: EncryptedSessionManager
) {
    private fun authHeader(): String {
        val token = sessionManager.getAccessToken() ?: throw Exception("No access token")
        return "Bearer $token"
    }

    private fun mapError(code: Int): String = when (code) {
        400 -> "Invalid request. Please try again."
        401 -> "Session expired. Please log in again."
        403 -> "You don't have permission for this action."
        404 -> "User not found."
        409 -> "Friend request already exists."
        413 -> "Request too large."
        415 -> "Unsupported format."
        422 -> "Invalid data. Please check and try again."
        500 -> "Server error. Please try again later."
        else -> "An error occurred (code $code)."
    }

    suspend fun sendFriendRequest(friendUserId: String): Result<FriendshipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendFriendRequest(authHeader(), FriendshipRequest(friendUserId))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapError(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun respondToRequest(friendshipId: String, accept: Boolean): Result<FriendshipResponse> = withContext(Dispatchers.IO) {
        try {
            val action = if (accept) "accept" else "reject"
            val response = api.respondToFriendRequest(authHeader(), FriendshipRespondRequest(friendshipId, action))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapError(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun getFriendships(): Result<FriendshipListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFriendships(authHeader())
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapError(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }

    suspend fun searchUsers(query: String): Result<FriendSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchUsers(authHeader(), query)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(mapError(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        }
    }
}
