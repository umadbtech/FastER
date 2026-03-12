package com.faster.festival.data.remote

import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenRefreshInterceptorTest {

    @Test
    fun `test non-401 response passes through without refresh`() {
        val sessionManager = mockk<com.faster.festival.data.local.EncryptedSessionManager>(relaxed = true)
        val authApiService = mockk<AuthApiService>(relaxed = true)
        val interceptor = TokenRefreshInterceptor(sessionManager) { authApiService }

        val request = Request.Builder()
            .url("https://api.example.com/content")
            .build()

        val chain = mockk<Interceptor.Chain>()
        val successResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns successResponse

        val result = interceptor.intercept(chain)
        assertEquals(200, result.code)
    }
}
