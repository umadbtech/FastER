package com.faster.festival.core.sos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Bridges [EmergencySOSManager.state] and [SosNotifier].
 *
 * Sits in the same app-scoped coroutine context as [WristbandSosWatcher] so
 * the notification updates regardless of UI lifecycle — a screen-off device
 * with a paired wristband will still wake to a full-screen SOS alert.
 *
 * Started from `SosModule.startEmergencyOrchestration()`.
 */
class SosNotificationCoordinator(
    private val manager: EmergencySOSManager,
    private val notifier: SosNotifier,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = manager.state.onEach { state ->
            when (state) {
                EmergencySOSState.Idle, EmergencySOSState.Preparing -> {
                    notifier.clear()
                }
                is EmergencySOSState.Sending -> {
                    Timber.tag(TAG).i("Posting SOS notification — Sending")
                    notifier.showSending(state.session)
                }
                is EmergencySOSState.Active -> {
                    notifier.showActive(
                        session = state.session,
                        userStatus = state.userStatus,
                        responderName = state.responderName,
                        etaMinutes = state.etaMinutes
                    )
                }
                is EmergencySOSState.Resolved -> {
                    notifier.showResolved(state.session, state.terminalStatus)
                }
                is EmergencySOSState.Cancelled -> {
                    notifier.showCancelled()
                }
                is EmergencySOSState.Failed -> {
                    notifier.showFailed(state.message)
                }
            }
        }.launchIn(scope)
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val TAG = "SosNotifCoordinator"
    }
}
