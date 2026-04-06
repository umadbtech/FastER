package com.faster.festival.data.util

import android.util.Log
import retrofit2.HttpException
import kotlin.math.pow

/**
 * Retry helper for API calls
 * Implements exponential backoff retry logic for transient errors
 */
object RetryHelper {
    private const val TAG = "RetryHelper"
    private const val MAX_RETRIES = 1
    private const val INITIAL_DELAY_MS = 100L
    private const val MAX_DELAY_MS = 1000L

    /**
     * Retry a suspend function with exponential backoff
     *
     * @param maxRetries Maximum number of retries (default: 1)
     * @param block The suspend function to retry
     * @return Result of the function call
     *
     * ✅ 401 errors are NOT retried here - TokenRefreshInterceptor handles them
     * ✅ 500+ errors are retried with exponential backoff
     * ✅ Network errors are retried
     */
    internal suspend inline fun <T> retryOnError(
        maxRetries: Int = MAX_RETRIES,
        noinline block: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { retryAttempt ->
            try {
                return block()
            } catch (e: HttpException) {
                // 401 should NOT be retried here - TokenRefreshInterceptor handles it
                if (e.code() == 401) {
                    Log.d(TAG, "401 error caught - TokenRefreshInterceptor will retry")
                    throw e
                }

                // 400-level errors (except 401) are not retryable
                if (e.code() in 400..499) {
                    Log.w(TAG, "Non-retryable HTTP error ${e.code()}")
                    throw e
                }

                // 500+ errors are retryable
                if (e.code() >= 500) {
                    Log.w(TAG, "Retryable HTTP error ${e.code()} (attempt $retryAttempt/$maxRetries)")
                    lastException = e

                    if (retryAttempt < maxRetries) {
                        val delayMs = calculateBackoffDelay(retryAttempt)
                        Log.d(TAG, "Waiting ${delayMs}ms before retry...")
                        kotlinx.coroutines.delay(delayMs)
                    }
                    return@repeat
                }
            } catch (e: Exception) {
                // Network errors and other exceptions are retryable
                Log.w(TAG, "Error: ${e.message} (attempt $retryAttempt/$maxRetries)")
                lastException = e

                if (retryAttempt < maxRetries) {
                    val delayMs = calculateBackoffDelay(retryAttempt)
                    Log.d(TAG, "Waiting ${delayMs}ms before retry...")
                    kotlinx.coroutines.delay(delayMs)
                }
                return@repeat
            }
        }

        // All retries exhausted
        lastException?.let { throw it }
        throw IllegalStateException("Retry exhausted but no exception captured")
    }

    /**
     * Calculate exponential backoff delay
     * Formula: min(2^attempt * 100ms, 1000ms)
     */
    private fun calculateBackoffDelay(attemptNumber: Int): Long {
        val exponentialDelay = (2.0.pow(attemptNumber) * INITIAL_DELAY_MS).toLong()
        return exponentialDelay.coerceAtMost(MAX_DELAY_MS)
    }
}
