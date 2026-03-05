package com.faster.festival.data.remote

import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.model.EnrollFactorResponse
import com.faster.festival.data.model.SignupRequest
import com.faster.festival.data.model.SignupResponse
import com.faster.festival.data.model.User
import com.faster.festival.data.model.LoginRequest
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.model.SendOtpRequest
import com.faster.festival.data.model.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

        // OTP endpoints for email verification (client calls backend service)
        @POST("otp/send")
        suspend fun sendOtp(@Body body: Map<String, String>): Response<Unit>

        // Use Supabase verify endpoint: expects { email, token, type }
        @POST("auth/v1/verify")
        suspend fun verifyOtp(@Body body: Map<String, String>): Response<AuthResponse>

        // Supabase phone OTP: send code via SMS
        @POST("auth/v1/otp")
        suspend fun sendPhoneOtp(@Body request: SendOtpRequest): Response<Unit>

        // Typed verify for phone OTP (type = "sms")
        @POST("auth/v1/verify")
        suspend fun verifyPhoneOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

        // Supabase password grant login: POST /auth/v1/token?grant_type=password
        @POST("auth/v1/token?grant_type=password")
        suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

        // Password recovery: send recovery email
        @POST("auth/v1/recover")
        suspend fun recover(@Body body: Map<String, String>): Response<Unit>

        // Update user (requires Authorization: Bearer <token>) to change password
        @PUT("auth/v1/user")
        suspend fun updateUser(@Header("Authorization") authorization: String, @Body body: Map<String, String>): Response<AuthResponse>

        // Logout endpoint: POST /auth/v1/logout with Authorization header
        @POST("auth/v1/logout")
        suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>
}
