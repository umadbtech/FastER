package com.faster.festival.core.sos

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Bridges [EmergencySOSManager.state] and [ActiveSosForegroundService].
 *
 *  • Sending / Active → start (or keep alive) the foreground service.
 *  • Resolved / Cancelled / Failed / Idle → stop the service.
 *
 * Sibling to [SosNotificationCoordinator] — both run inside
 * [com.faster.festival.di.SosModule.emergencyScope] so they survive UI death.
 */
class ActiveSosServiceCoordinator(
    private val context: Context,
    private val manager: EmergencySOSManager,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = manager.state.onEach { state ->
            when (state) {
                is EmergencySOSState.Sending,
                is EmergencySOSState.Active -> {
                    Timber.tag(TAG).d("Active SOS — ensuring FG service running")
                    ActiveSosForegroundService.ensureRunning(context)
                }
                EmergencySOSState.Idle,
                EmergencySOSState.Preparing,
                is EmergencySOSState.Resolved,
                is EmergencySOSState.Cancelled,
                is EmergencySOSState.Failed -> {
                    Timber.tag(TAG).d("Inactive state %s — stopping FG service",
                        state::class.simpleName)
                    ActiveSosForegroundService.ensureStopped(context)
                }
            }
        }.launchIn(scope)
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val TAG = "ActiveSosSvcCoord"
    }
}
