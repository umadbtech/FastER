package com.faster.festival.ui.util

import com.faster.festival.data.network.ConnectivityState
import com.faster.festival.data.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Reusable helpers for view models that fold a connectivity-aware "Offline"
 * branch into their sealed UI state. Designed so each view model only has
 * to add ~3 lines of code on top of its existing load logic.
 *
 * Usage:
 *   if (networkMonitor.isOfflineNow()) { _state.value = Offline; armAutoRetry { load() }; return }
 *   ...
 *   } catch (e: Exception) {
 *       if (networkMonitor.isOfflineNow(e)) { _state.value = Offline; armAutoRetry { load() } }
 *       else _state.value = Error(...)
 *   }
 */
fun NetworkMonitor?.isOfflineNow(throwable: Throwable? = null): Boolean {
    if (this != null && current.isOffline) return true
    if (throwable is IOException) return true
    return false
}

/**
 * One-shot auto-retry. Cancels any prior auto-retry attached to [holder] and
 * launches a coroutine that suspends until the next [ConnectivityState.Available]
 * transition, then runs [action]. Returns the new Job for the caller to track
 * (typically stored back into [holder]).
 */
fun NetworkMonitor.armAutoRetry(
    scope: CoroutineScope,
    holder: Job?,
    action: () -> Unit
): Job {
    holder?.cancel()
    return scope.launch {
        state.first { it is ConnectivityState.Available }
        action()
    }
}
