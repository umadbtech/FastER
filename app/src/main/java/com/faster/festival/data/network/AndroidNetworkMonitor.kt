package com.faster.festival.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production [NetworkMonitor] backed by [ConnectivityManager.NetworkCallback].
 *
 * Behaviour:
 *  • Tracks the set of *validated* networks (NET_CAPABILITY_VALIDATED) so the
 *    state only flips to [ConnectivityState.Lost] when the LAST validated
 *    network goes away — handles dual-stack (Wi-Fi + cellular) gracefully.
 *  • Fires [onReconnected] only on Lost/Unavailable -> Available transitions,
 *    not on cold start.
 *
 * Requires `android.permission.ACCESS_NETWORK_STATE` in the manifest.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(initialState())
    override val state: StateFlow<ConnectivityState> = _state.asStateFlow()
    override val current: ConnectivityState get() = _state.value

    private val _reconnected = MutableSharedFlow<Unit>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val onReconnected: Flow<Unit> = _reconnected.asSharedFlow()

    private val validatedNetworks = mutableSetOf<Network>()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { /* wait for validated capabilities */ }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val validated =
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val wasOnline = _state.value is ConnectivityState.Available
            if (validated) {
                validatedNetworks += network
                _state.value = ConnectivityState.Available
                if (!wasOnline) scope.launch { _reconnected.emit(Unit) }
            } else {
                validatedNetworks -= network
                if (validatedNetworks.isEmpty()) {
                    _state.value = ConnectivityState.Lost
                }
            }
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            if (validatedNetworks.size <= 1) _state.value = ConnectivityState.Losing
        }

        override fun onLost(network: Network) {
            validatedNetworks -= network
            if (validatedNetworks.isEmpty()) _state.value = ConnectivityState.Lost
        }

        override fun onUnavailable() {
            if (validatedNetworks.isEmpty()) _state.value = ConnectivityState.Unavailable
        }
    }

    init {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
            }
        }.onFailure {
            // If registration fails (e.g. missing perm in misconfigured manifest)
            // we degrade to "Unavailable" rather than crashing.
            _state.value = ConnectivityState.Unavailable
        }
    }

    private fun initialState(): ConnectivityState {
        val active = cm.activeNetwork ?: return ConnectivityState.Unavailable
        val caps = cm.getNetworkCapabilities(active) ?: return ConnectivityState.Unavailable
        val online =
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (online) ConnectivityState.Available else ConnectivityState.Lost
    }
}
