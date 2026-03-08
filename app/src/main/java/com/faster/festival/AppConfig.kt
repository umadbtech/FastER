package com.faster.festival

/**
 * Centralized app configuration and constants
 * Avoids hard-coded values scattered throughout the codebase
 */
object AppConfig {
    // ============= Festival Defaults =============
    const val DEFAULT_FESTIVAL_SLUG = "floydfest-26"
    const val DEFAULT_FESTIVAL_ID = "297d5837-a7b6-49a4-873b-4e3b17b60657"

    // ============= Network Timeouts =============
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // ============= Token Management =============
    // Refresh token 1 minute before it expires to ensure smooth UX
    const val TOKEN_REFRESH_THRESHOLD_SECONDS = 60L

    // ============= Retry Logic =============
    const val MAX_RETRIES = 1
    const val RETRY_DELAY_MS = 100L
}
