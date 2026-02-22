package com.faster.festival.data.repository

import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryMapErrorTest {

    @Test
    fun map_invalidCredentials_extractsMsg() {
        val body = "{\"msg\": \"Invalid credentials\"}"
        val mapped = AuthRepository.mapLoginError(401, body)
        assertTrue(mapped.contains("Invalid email or password") || mapped.contains("Invalid credentials"))
    }

    @Test
    fun map_rateLimit_returnsFriendly() {
        val mapped = AuthRepository.mapLoginError(429, "{\"msg\": \"Rate limit\"}")
        assertEquals("Too many requests. Please try again later.", mapped)
    }

    @Test
    fun map_unknown_returnsCode() {
        val mapped = AuthRepository.mapLoginError(500, null)
        assertTrue(mapped.contains("Login failed: 500"))
    }
}
