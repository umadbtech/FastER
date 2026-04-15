package com.faster.festival.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Shared utility for surfacing a "coming soon" message whenever the user taps
 * an action whose destination/feature is not yet implemented.
 *
 * This prevents silent no-op taps and broken navigation across the app.
 *
 * Usage from a Composable:
 * ```
 * val showComingSoon = rememberComingSoonToast()
 * IconButton(onClick = { showComingSoon() }) { ... }
 * ```
 *
 * Or directly with a context:
 * ```
 * ComingSoonToast.show(context)
 * ```
 */
object ComingSoonToast {

    const val DEFAULT_MESSAGE = "Coming soon"

    fun show(context: Context, message: String = DEFAULT_MESSAGE) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Compose-friendly helper. Captures [LocalContext] once and returns a lambda
 * that shows the "Coming soon" toast. Safe to use inside any composable.
 */
@Composable
fun rememberComingSoonToast(message: String = ComingSoonToast.DEFAULT_MESSAGE): () -> Unit {
    val context = LocalContext.current
    return { ComingSoonToast.show(context, message) }
}
