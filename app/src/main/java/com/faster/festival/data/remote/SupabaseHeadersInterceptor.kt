package com.faster.festival.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor to attach Supabase required headers
 * - Always adds: apikey header
 * - Conditionally adds: Authorization Bearer token (if available)
 */
class SupabaseHeadersInterceptor(
    private val apiKey: String,
    private val getAccessToken: () -> String? = { null }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val requestBuilder = request.newBuilder()
            // Always add API key header
            .header("apikey", apiKey)
            .header("Content-Type", "application/json")

        // Conditionally add Authorization header if token is available
        val token = getAccessToken()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        request = requestBuilder.build()
        return chain.proceed(request)
    }
}
