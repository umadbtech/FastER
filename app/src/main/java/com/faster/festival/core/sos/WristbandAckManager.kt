package com.faster.festival.core.sos

import com.faster.festival.wristband.domain.repository.WristbandMeshRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * BLE-side response coordinator. Wraps the existing
 * [WristbandMeshRepository] vendor-message senders (sub-cmds 0x20 / 0x21 / 0x22)
 * with idempotency so the wristband doesn't get the same ACK twice.
 *
 * Why centralize: previously [com.faster.festival.wristband.ui.sos.SosAlertViewModel]
 * sent the BLE ACK itself. With the unified [EmergencySOSManager], the BLE
 * ACK is part of the manager's reaction to an inbound 0x11 — keeping the VM
 * out of it means the ACK fires the same way whether the overlay UI happens
 * to be alive or not.
 */
class WristbandAckManager(
    private val wristbandRepo: WristbandMeshRepository
) {

    private val ackedEventIds = mutableSetOf<Long>()
    private val responderEventIds = mutableSetOf<Long>()
    private val resolvedEventIds = mutableSetOf<Long>()
    private val mutex = Mutex()

    /**
     * Send the firmware-required `0x20` SOS_ACK once per event id. Subsequent
     * retries from the wristband for the same `event_id` are no-ops here.
     */
    suspend fun ackOnce(eventId: Long, helpDispatched: Boolean = true) {
        mutex.withLock {
            if (eventId in ackedEventIds) {
                Timber.tag(TAG).d("SOS ACK already sent for event_id=%d", eventId)
                return
            }
            wristbandRepo.sendSosAck(eventId, helpDispatched).onSuccess {
                ackedEventIds += eventId
                Timber.tag(TAG).i("SOS ACK 0x20 sent — event_id=%d helpDispatched=%s",
                    eventId, helpDispatched)
            }.onFailure { Timber.tag(TAG).w(it, "SOS ACK 0x20 failed — event_id=%d", eventId) }
        }
    }

    /** `0x21` SOS_RESPONDER — once per event id. */
    suspend fun responderDispatchedOnce(eventId: Long, etaMinutes: Int) {
        mutex.withLock {
            if (eventId in responderEventIds) return
            wristbandRepo.sendResponderDispatched(eventId, etaMinutes).onSuccess {
                responderEventIds += eventId
                Timber.tag(TAG).i("SOS RESPONDER 0x21 sent — event_id=%d eta=%d",
                    eventId, etaMinutes)
            }.onFailure { Timber.tag(TAG).w(it, "SOS RESPONDER 0x21 failed") }
        }
    }

    /** `0x22` SOS_RESOLVED — once per event id. */
    suspend fun resolvedOnce(eventId: Long, falseAlarm: Boolean = false) {
        mutex.withLock {
            if (eventId in resolvedEventIds) return
            wristbandRepo.sendResolved(eventId, falseAlarm).onSuccess {
                resolvedEventIds += eventId
                Timber.tag(TAG).i("SOS RESOLVED 0x22 sent — event_id=%d falseAlarm=%s",
                    eventId, falseAlarm)
            }.onFailure { Timber.tag(TAG).w(it, "SOS RESOLVED 0x22 failed") }
        }
    }

    /** Wipe all dedup state — call when an SOS terminal state is reached. */
    suspend fun resetForEvent(eventId: Long) {
        mutex.withLock {
            ackedEventIds -= eventId
            responderEventIds -= eventId
            resolvedEventIds -= eventId
        }
    }

    private companion object {
        const val TAG = "WristbandAck"
    }
}
