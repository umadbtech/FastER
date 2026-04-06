package com.faster.festival.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Festival Header API response from Supabase Edge Function
 */
@Serializable
data class FestivalHeaderResponse(
    @SerialName("schema_version")
    val schemaVersion: String,
    val festival: FestivalHeader
)

/**
 * Festival header data containing branding, dates, and context
 */
@Serializable
data class FestivalHeader(
    val id: String,
    val slug: String,
    val name: String,
    val timezone: String,
    @SerialName("starts_at")
    val startsAt: String,
    @SerialName("ends_at")
    val endsAt: String,
    @SerialName("logo_url")
    val logoUrl: String,
    @SerialName("banner_url")
    val bannerUrl: String,
    @SerialName("accent_color_hex")
    val accentColorHex: String? = null,  // Nullable - uses default Navy Blue if null
    @SerialName("context_state")
    val contextState: String
)

/**
 * Result wrapper for repository operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
