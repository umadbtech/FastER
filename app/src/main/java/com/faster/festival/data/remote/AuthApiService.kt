package com.faster.festival.data.remote

import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.model.EnrollFactorResponse
import com.faster.festival.data.model.SignupRequest
import com.faster.festival.data.model.SignupResponse
import com.faster.festival.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {

        @POST("auth/v1/signup")
        suspend fun signUp(@Body request: SignupRequest): Response<SignupResponse>

        @GET("auth/v1/user")
        suspend fun getUser(@Header("Authorization") token: String): Response<User>

        @POST("auth/v1/factors")
        suspend fun enrollFactor(
                @Header("Authorization") token: String,
                @Body body: Map<String, String> = mapOf("factor_type" to "totp")
        ): Response<EnrollFactorResponse>

        @POST("auth/v1/factors/{factorId}/verify")
        suspend fun verifyFactor(
                @Header("Authorization") token: String,
                @Path("factorId") factorId: String,
                @Body body: Map<String, String>
        ): Response<AuthResponse>
}
