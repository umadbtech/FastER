package com.faster.festival.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to add Authorization Bearer token when available
 */
class AuthorizationInterceptor(
    private val getAccessToken: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val token = getAccessToken()
        if (!token.isNullOrBlank()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
