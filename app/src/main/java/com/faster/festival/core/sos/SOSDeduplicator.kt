package com.faster.festival.core.sos

import java.util.concurrent.ConcurrentHashMap

/**
 * Wristband-side dedup. The firmware retries SOS at 1s/2s/4s/8s/16s/30s
 * (see `Mobile-Vendor-Model-Dev.md` §5.2) — every retry has the SAME
 * `event_id`. We must not create more than one backend alert per emergency.
 *
 * Window: TTL on each remembered event is generous (1 hour) — way past the
 * firmware's max retry budget but short enough that a long-lived session
 * doesn't leak memory if the user opens-and-paid wristbands repeatedly.
 *
 * Thread-safe: backed by [ConcurrentHashMap] so the wristband watcher
 * coroutine and any UI-driven cancel can hit it concurrently.
 */
class SOSDeduplicator(private val ttlMs: Long = DEFAULT_TTL_MS) {

    private val seen = ConcurrentHashMap<Long, Long>()    // eventId → firstSeenAtMs

    /**
     * Returns `true` the first time we see [eventId] within the TTL window,
     * `false` for every subsequent retry. Caller should drop the second-and-
     * later returns silently.
     */
    fun shouldHandle(eventId: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        purgeExpired(nowMs)
        val firstSeen = seen.putIfAbsent(eventId, nowMs)
        return firstSeen == null
    }

    /**
     * Forget every retained event id. Call when an active session resolves /
     * cancels — we don't want stale entries to suppress a NEW emergency from
     * the same wristband if its monotonic counter happens to wrap.
     */
    fun reset() = seen.clear()

    /** Remove only the given event id (used on explicit 0x12 cancel). */
    fun forget(eventId: Long) { seen.remove(eventId) }

    private fun purgeExpired(nowMs: Long) {
        val it = seen.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (nowMs - e.value > ttlMs) it.remove()
        }
    }

    companion object {
        const val DEFAULT_TTL_MS: Long = 60 * 60 * 1000L  // 1 hour
    }
}
