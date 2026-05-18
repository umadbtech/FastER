package com.faster.festival.di

import android.content.Context
import com.faster.festival.data.network.AndroidNetworkMonitor
import com.faster.festival.data.network.NetworkMonitor

/**
 * Manual DI holder for the app-wide [NetworkMonitor]. Initialized once
 * from `FASTERApplication.onCreate` via [initialize]. Kept out of
 * [NetworkModule] so the Retrofit/OkHttp wiring there stays untouched.
 */
object ConnectivityModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "ConnectivityModule.initialize(context) must be called first"
        }

    val networkMonitor: NetworkMonitor by lazy { AndroidNetworkMonitor(ctx) }
}
