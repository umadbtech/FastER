package com.faster.festival.ui.util

/**
 * Per-screen content state with a first-class `Offline` variant so the UI can
 * render a different surface (and auto-retry on reconnect) instead of folding
 * "no internet" into a generic error.
 */
sealed class LoadState<out T> {
    object Loading : LoadState<Nothing>()
    data class Success<T>(val data: T) : LoadState<T>()
    object Empty : LoadState<Nothing>()
    object Offline : LoadState<Nothing>()
    data class Error(val message: String, val cause: Throwable? = null) : LoadState<Nothing>()
}
