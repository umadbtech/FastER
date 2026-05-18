package com.faster.festival.core.sos

import com.faster.festival.wristband.domain.usecase.ObserveSosEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App-scoped collector that pumps wristband BLE Mesh SOS events into the
 * [EmergencySOSManager]. Lives outside any UI lifecycle so a SOS event lands
 * even when the screen with the overlay isn't currently composed.
 *
 * Started exactly once from `FASTERApplication.onCreate` after both
 * [com.faster.festival.wristband.di.WristbandModule] and `SosModule` are
 * initialized.
 */
class WristbandSosWatcher(
    private val observeSos: ObserveSosEventsUseCase,
    private val manager: EmergencySOSManager,
    private val scope: CoroutineScope
) {

    private var eventsJob: Job? = null
    private var cancelsJob: Job? = null

    fun start() {
        if (eventsJob?.isActive == true) return
        eventsJob = observeSos.events.onEach { event ->
            Timber.tag(TAG).d(
                "Wristband SOS 0x11 — event_id=%d retry=%d battery=%d",
                event.eventId, event.retryCount, event.batteryPct
            )
            scope.launch {
                manager.handleWristbandSOS(
                    eventId = event.eventId,
                    retryCount = event.retryCount,
                    batteryPct = event.batteryPct,
                    deviceUptimeMs = event.deviceUptimeMs
                )
            }
        }.launchIn(scope)

        cancelsJob = observeSos.cancels.onEach { cancel ->
            Timber.tag(TAG).d("Wristband SOS 0x12 cancel — event_id=%d", cancel.eventId)
            scope.launch { manager.handleWristbandCancel(cancel.eventId) }
        }.launchIn(scope)
    }

    fun stop() {
        eventsJob?.cancel(); eventsJob = null
        cancelsJob?.cancel(); cancelsJob = null
    }

    private companion object {
        const val TAG = "WristbandSosWatcher"
    }
}
