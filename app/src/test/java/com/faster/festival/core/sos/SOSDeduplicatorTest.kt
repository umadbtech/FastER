package com.faster.festival.core.sos

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The wristband firmware retries SOS at 1s/2s/4s/8s/16s/30s with the SAME
 * `event_id` (per `Mobile-Vendor-Model-Dev.md` §5.2). The deduplicator MUST
 * drop every retry after the first so we don't fan out into N backend alerts
 * for one emergency.
 */
class SOSDeduplicatorTest {

    @Test
    fun `first event is allowed`() {
        val d = SOSDeduplicator()
        assertTrue(d.shouldHandle(eventId = 42L))
    }

    @Test
    fun `firmware retry of same event_id is dropped`() {
        val d = SOSDeduplicator()
        assertTrue(d.shouldHandle(42L))
        // Wristband retries at 1s/2s/4s/8s/16s/30s — every one with event_id=42.
        repeat(6) { assertFalse(d.shouldHandle(42L), "Retry $it should be dropped") }
    }

    @Test
    fun `different event_id is allowed independently`() {
        val d = SOSDeduplicator()
        assertTrue(d.shouldHandle(1L))
        assertTrue(d.shouldHandle(2L))
        assertFalse(d.shouldHandle(1L))
        assertFalse(d.shouldHandle(2L))
    }

    @Test
    fun `expired entries are purged after TTL`() {
        val d = SOSDeduplicator(ttlMs = 1000L)
        val t0 = 0L
        val t1 = 500L                 // within TTL
        val t2 = 1500L                // past TTL

        assertTrue(d.shouldHandle(7L, nowMs = t0))
        assertFalse(d.shouldHandle(7L, nowMs = t1))      // still within window
        assertTrue(d.shouldHandle(7L, nowMs = t2))       // window expired — accept again
    }

    @Test
    fun `forget removes a single event`() {
        val d = SOSDeduplicator()
        assertTrue(d.shouldHandle(99L))
        d.forget(99L)
        assertTrue(d.shouldHandle(99L))   // accepted again after forget
    }

    @Test
    fun `reset clears every retained event`() {
        val d = SOSDeduplicator()
        assertTrue(d.shouldHandle(1L))
        assertTrue(d.shouldHandle(2L))
        d.reset()
        assertTrue(d.shouldHandle(1L))
        assertTrue(d.shouldHandle(2L))
    }
}
