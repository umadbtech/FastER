package com.faster.festival.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Application-wide internet connectivity monitor. Survives configuration
 * changes — there's exactly one instance per process, owned by
 * [com.faster.festival.di.ConnectivityModule].
 */
interface NetworkMonitor {
    /** Continuous stream of the current connectivity state. */
    val state: StateFlow<ConnectivityState>

    /** Snapshot — useful when a request is about to fire. */
    val current: ConnectivityState

    /**
     * Hot stream of "transitioned to online from offline". Suppressed on
     * cold start. Use this to drive the "You're back online" snackbar.
     */
    val onReconnected: Flow<Unit>
}
