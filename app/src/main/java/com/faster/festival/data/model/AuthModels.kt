package com.faster.festival.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@Serializable
data class SignupResponse(
    val id: String,
    val aud: String,
    val role: String,
    val email: String,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String>? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: User? = null // Sometimes nested in 'user' field depending on endpoint/version
)

@Serializable
data class ErrorResponse(
    val code: Int? = null,
    val msg: String? = null,
    val error: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)

// Reusing User for general usage if needed, or mapping SignupResponse to it
@Serializable
data class User(
    val id: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String>? = null
) {
    val isEmailVerified: Boolean
        get() = !emailConfirmedAt.isNullOrEmpty()

    val is2faEnabled: Boolean
        get() = userMetadata?.get("is_2fa_enabled") == "true"
}

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: User? = null
)

@Serializable
data class MagicLinkCallbackData(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val email: String? = null
)

@Serializable
data class TotpData(
    val uri: String,
    val secret: String? = null
)

@Serializable
data class EnrollFactorResponse(
    val id: String,
    val totp: TotpData
)

// Login models for password grant
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: User? = null
)

// Token refresh models
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("token_type")
    val tokenType: String = "Bearer",
    @SerialName("expires_in")
    val expiresIn: Int? = null
)

// Password recovery models
@Serializable
data class PasswordResetRequest(
    val email: String
)

@Serializable
data class RecoveryVerifyRequest(
    val email: String,
    val token: String,
    val type: String = "recovery"
)

@Serializable
data class UpdatePasswordRequest(
    val password: String
)

// Password OTP models for phone
@Serializable
data class SendOtpRequest(
    val phone: String,
    @SerialName("create_user") val createUser: Boolean = true
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val token: String,
    val type: String = "sms"
)

// The verify endpoint returns AuthResponse (tokens + user), reuse AuthResponse as AuthSession

typealias AuthSession = AuthResponse

// The verify endpoint returns AuthResponse (tokens + user), reuse AuthResponse for session mapping
