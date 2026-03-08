package com.faster.festival.ui.util

import retrofit2.HttpException
import java.io.IOException

/**
 * Centralized error mapping for consistent error UI messages across the app
 * Maps exceptions to user-friendly messages
 */
object ErrorMapper {

    /**
     * Map any throwable to a user-friendly error message
     */
    fun mapThrowableToMessage(throwable: Throwable): String = when (throwable) {
        is HttpException -> mapHttpError(throwable.code(), throwable.message())
        is IOException -> "No internet connection. Check your network."
        else -> throwable.message ?: "An unknown error occurred."
    }

    /**
     * Map HTTP error codes to user-friendly messages
     */
    fun mapHttpError(code: Int, message: String? = null): String = when {
        code == 401 -> "Session expired. Refreshing automatically..."
        code == 403 -> "Access denied."
        code == 404 -> "Resource not found."
        code in 500..599 -> "Server is experiencing issues. Please try again."
        else -> message ?: "An error occurred (HTTP $code)"
    }

    /**
     * Determine if an error is retryable by the UI (manual retry button)
     * Note: 401 errors are automatically retried by TokenRefreshInterceptor
     */
    fun isRetryableError(code: Int): Boolean = when (code) {
        401 -> false // Will be retried automatically by TokenRefreshInterceptor
        in 500..599 -> true
        in 400..499 -> false
        else -> true // Network errors are retryable
    }

    /**
     * Check if error is a session/auth related error
     */
    fun isAuthError(throwable: Throwable): Boolean {
        return if (throwable is HttpException) {
            throwable.code() == 401 || throwable.code() == 403
        } else {
            false
        }
    }
}
