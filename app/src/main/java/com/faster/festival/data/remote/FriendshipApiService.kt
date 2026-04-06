package com.faster.festival.data.remote

import com.faster.festival.data.model.FriendSearchResponse
import com.faster.festival.data.model.FriendshipListResponse
import com.faster.festival.data.model.FriendshipRequest
import com.faster.festival.data.model.FriendshipRespondRequest
import com.faster.festival.data.model.FriendshipResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface FriendshipApiService {
    @POST("functions/v1/friendship-request")
    suspend fun sendFriendRequest(
        @Header("Authorization") authorization: String,
        @Body request: FriendshipRequest
    ): Response<FriendshipResponse>

    @POST("functions/v1/friendship-respond")
    suspend fun respondToFriendRequest(
        @Header("Authorization") authorization: String,
        @Body request: FriendshipRespondRequest
    ): Response<FriendshipResponse>

    @GET("functions/v1/friendship-list")
    suspend fun getFriendships(
        @Header("Authorization") authorization: String
    ): Response<FriendshipListResponse>

    @GET("functions/v1/friendship-search")
    suspend fun searchUsers(
        @Header("Authorization") authorization: String,
        @Query("q") query: String
    ): Response<FriendSearchResponse>
}
