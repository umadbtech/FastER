package com.faster.festival.ui.util

import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ErrorMapper
 * Tests error code → message mapping and retry logic
 */
class ErrorMapperTest {

    @Test
    fun `test 401 error maps to session expired message`() {
        // Arrange
        val exception = createHttpException(401, "Unauthorized")

        // Act
        val message = ErrorMapper.mapThrowableToMessage(exception)

        // Assert
        assertEquals("Session expired. Refreshing automatically...", message)
    }

    @Test
    fun `test 403 error maps to access denied message`() {
        // Arrange
        val exception = createHttpException(403, "Forbidden")

        // Act
        val message = ErrorMapper.mapThrowableToMessage(exception)

        // Assert
        assertEquals("Access denied.", message)
    }

    @Test
    fun `test 404 error maps to not found message`() {
        // Arrange
        val exception = createHttpException(404, "Not Found")

        // Act
        val message = ErrorMapper.mapThrowableToMessage(exception)

        // Assert
        assertEquals("Resource not found.", message)
    }

    @Test
    fun `test 500 error maps to server error message`() {
        // Arrange
        val exception = createHttpException(500, "Internal Server Error")

        // Act
        val message = ErrorMapper.mapThrowableToMessage(exception)

        // Assert
        assertEquals("Server is experiencing issues. Please try again.", message)
    }

    @Test
    fun `test IOException maps to no internet message`() {
        // Arrange
        val exception = IOException("Network error")

        // Act
        val message = ErrorMapper.mapThrowableToMessage(exception)

        // Assert
        assertEquals("No internet connection. Check your network.", message)
    }

    @Test
    fun `test 401 is not retryable by UI`() {
        // Act & Assert - 401 is retried automatically by TokenRefreshInterceptor
        assertFalse(ErrorMapper.isRetryableError(401))
    }

    @Test
    fun `test 500 is retryable by UI`() {
        // Act & Assert
        assertTrue(ErrorMapper.isRetryableError(500))
    }

    @Test
    fun `test 503 is retryable by UI`() {
        // Act & Assert
        assertTrue(ErrorMapper.isRetryableError(503))
    }

    @Test
    fun `test 400 is not retryable by UI`() {
        // Act & Assert - client errors not retryable
        assertFalse(ErrorMapper.isRetryableError(400))
    }

    @Test
    fun `test 401 is auth error`() {
        // Arrange
        val exception = createHttpException(401, "Unauthorized")

        // Act & Assert
        assertTrue(ErrorMapper.isAuthError(exception))
    }

    @Test
    fun `test 403 is auth error`() {
        // Arrange
        val exception = createHttpException(403, "Forbidden")

        // Act & Assert
        assertTrue(ErrorMapper.isAuthError(exception))
    }

    @Test
    fun `test 500 is not auth error`() {
        // Arrange
        val exception = createHttpException(500, "Server Error")

        // Act & Assert
        assertFalse(ErrorMapper.isAuthError(exception))
    }

    @Test
    fun `test IOException is not auth error`() {
        // Arrange
        val exception = IOException("Network error")

        // Act & Assert
        assertFalse(ErrorMapper.isAuthError(exception))
    }

    // Helper function to create mock HttpException
    private fun createHttpException(code: Int, message: String): HttpException {
        val response = Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType()))
        return HttpException(response)
    }

    private fun String.toResponseBody(mediaType: String) =
        okhttp3.ResponseBody.Companion.create(mediaType.toMediaType(), this)
}
