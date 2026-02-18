package com.faster.festival.utils

import android.net.Uri

object DeepLinkHandler {
    const val SCHEME = "yourapp"
    const val AUTH_CALLBACK_HOST = "auth-callback"

    data class CallbackData(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val email: String? = null,
        val error: String? = null
    )

    fun parseAuthCallback(uri: Uri): CallbackData {
        return try {
            val accessToken = uri.getQueryParameter("access_token")
            val refreshToken = uri.getQueryParameter("refresh_token")
            val email = uri.getQueryParameter("email")
            val error = uri.getQueryParameter("error")

            // Also check fragment-based params (in case Supabase uses hash routing)
            val fragment = uri.fragment
            val fragmentParams = parseFragmentParams(fragment)

            val finalAccessToken = accessToken ?: fragmentParams["access_token"]
            val finalRefreshToken = refreshToken ?: fragmentParams["refresh_token"]
            val finalEmail = email ?: fragmentParams["email"]
            val finalError = error ?: fragmentParams["error"]

            if (!finalError.isNullOrEmpty()) {
                CallbackData(error = finalError)
            } else if (finalAccessToken != null && finalRefreshToken != null) {
                CallbackData(
                    accessToken = finalAccessToken,
                    refreshToken = finalRefreshToken,
                    email = finalEmail
                )
            } else {
                CallbackData(error = "Missing tokens in callback")
            }
        } catch (e: Exception) {
            CallbackData(error = "Failed to parse callback: ${e.message}")
        }
    }

    private fun parseFragmentParams(fragment: String?): Map<String, String> {
        if (fragment.isNullOrEmpty()) return emptyMap()

        return fragment.split("&").associate { param ->
            val (key, value) = if ("=" in param) {
                param.split("=", limit = 2).let { it[0] to it.getOrNull(1).orEmpty() }
            } else {
                param to ""
            }
            key to value
        }
    }

    fun isAuthCallback(uri: Uri): Boolean {
        return uri.scheme == SCHEME && uri.host == AUTH_CALLBACK_HOST
    }
}
