package com.faster.festival.ui.components.network

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.faster.festival.data.network.NetworkMonitor

/**
 * Drives a transient "You're back online" snackbar off the monitor's
 * [NetworkMonitor.onReconnected] flow. Suppressed on cold start — only
 * fires on Lost/Unavailable -> Available transitions.
 *
 * Mount once at the app shell next to your shared [SnackbarHostState].
 */
@Composable
fun ConnectionRestoredSnackbarEffect(
    monitor: NetworkMonitor,
    snackbarHostState: SnackbarHostState,
    message: String = "You're back online"
) {
    LaunchedEffect(monitor) {
        monitor.onReconnected.collect {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
}
