package com.faster.festival.data.network

/**
 * App-facing connectivity state. Maps the Android ConnectivityManager
 * callbacks down to four states the UI can branch on without knowing
 * about NetworkCapabilities internals.
 */
sealed class ConnectivityState {
    /** A network with internet capability is available and validated. */
    object Available : ConnectivityState()

    /** Active network is being torn down (Wi-Fi about to drop, cell handoff). */
    object Losing : ConnectivityState()

    /** Active network was lost — there's no internet right now. */
    object Lost : ConnectivityState()

    /** No suitable network has been seen since process start. */
    object Unavailable : ConnectivityState()

    val isOnline: Boolean get() = this is Available
    val isOffline: Boolean get() = this is Lost || this is Unavailable
}
