package com.faster.festival.ui.util

import com.faster.festival.data.network.ConnectivityState
import com.faster.festival.data.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Reusable VM helper that turns any `suspend fun fetch(): T?` into a
 * connectivity-aware [LoadState] flow.
 *
 *  • If the device is offline at call time, emits [LoadState.Offline] without
 *    flashing a spinner.
 *  • On exception, classifies between [LoadState.Offline] (IOException OR
 *    monitor reports offline) and [LoadState.Error] (server / parse problem).
 *  • While offline, arms a one-shot auto-retry that fires the moment the
 *    monitor transitions back to [ConnectivityState.Available].
 *  • A `null` payload from `fetch` is rendered as [LoadState.Empty].
 */
class NetworkAwareLoader<T>(
    private val scope: CoroutineScope,
    private val monitor: NetworkMonitor,
    private val fetch: suspend () -> T?
) {
    private val _state = MutableStateFlow<LoadState<T>>(LoadState.Loading)
    val state: StateFlow<LoadState<T>> = _state.asStateFlow()

    private var inFlight: Job? = null
    private var autoRetryJob: Job? = null

    fun load(force: Boolean = false) {
        if (inFlight?.isActive == true && !force) return
        inFlight?.cancel()

        if (monitor.current.isOffline) {
            _state.value = LoadState.Offline
            armAutoRetry()
            return
        }

        _state.value = LoadState.Loading
        inFlight = scope.launch {
            runCatching { fetch() }
                .onSuccess { data ->
                    _state.value = if (data == null) LoadState.Empty else LoadState.Success(data)
                }
                .onFailure { t ->
                    val offlineNow = monitor.current.isOffline || t is IOException
                    _state.value = if (offlineNow) {
                        LoadState.Offline
                    } else {
                        LoadState.Error(t.localizedMessage ?: "Something went wrong", t)
                    }
                    if (offlineNow) armAutoRetry()
                }
        }
    }

    /** When state == Offline, wait for the next Available transition and reload once. */
    private fun armAutoRetry() {
        autoRetryJob?.cancel()
        autoRetryJob = scope.launch {
            monitor.state.first { it is ConnectivityState.Available }
            if (_state.value is LoadState.Offline) load(force = true)
        }
    }

    fun retry() = load(force = true)
}
