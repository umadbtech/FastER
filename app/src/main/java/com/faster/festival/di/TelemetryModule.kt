package com.faster.festival.di

import android.content.Context
import com.faster.festival.core.telemetry.TelemetryCollector
import com.faster.festival.wristband.di.WristbandModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual-DI wiring for the telemetry pipeline. Mirrors the pattern used by
 * [DatabaseModule] / [SosModule] / [WristbandModule] — initialized exactly
 * once from `FASTERApplication.onCreate` via [initialize].
 *
 * The queue repository itself lives on [DatabaseModule] because it owns the
 * Room handle; this module wires the BLE-side collector and exposes the
 * process-wide coroutine scope the collector runs in.
 */
object TelemetryModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "TelemetryModule.initialize(context) must be called first"
        }

    private val collectorScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    val telemetryCollector: TelemetryCollector by lazy {
        TelemetryCollector(
            context = ctx,
            observeTelemetry = WristbandModule.observeTelemetry,
            wristbandRepo = DatabaseModule.wristbandRepository,
            queue = DatabaseModule.telemetryQueueRepository,
            scope = collectorScope
        )
    }
}
