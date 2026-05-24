package com.faster.festival.core.location

import android.location.Location
import android.os.SystemClock
import kotlin.math.abs

/**
 * Pure validation for a device GPS fix before it is trusted for SOS dispatch or
 * map display.
 *
 * No fake / placeholder / hardcoded coordinate can ever pass here — a fix is
 * only "usable" if it is a real, finite, in-range, reasonably-accurate and
 * recent reading from the OS. This is the single gate that enforces the
 * project rule: *the app only shares real live device location.*
 *
 * Rejected outright:
 *  • `null`
 *  • NaN / Infinite latitude or longitude
 *  • out-of-range coordinates (|lat| > 90, |lon| > 180)
 *  • the (0,0) "Null Island" sentinel — our explicit "GPS unavailable" marker,
 *    never a real fix
 *  • absurdly imprecise fixes (accuracy worse than [MAX_ACCURACY_METERS])
 *  • stale fixes older than the supplied freshness window
 */
object LocationValidation {

    /** A "current" fix older than this (monotonic age) is treated as stale. */
    const val MAX_FIX_AGE_MS = 120_000L          // 2 min

    /** Last-known fallback is only acceptable if at least this fresh. */
    const val MAX_LAST_KNOWN_AGE_MS = 300_000L   // 5 min

    /** Beyond this horizontal accuracy a fix is too imprecise to act on. */
    const val MAX_ACCURACY_METERS = 1_000f

    /** Coordinates within this of (0,0) are the Null-Island sentinel. */
    private const val NULL_ISLAND_EPS = 0.0001

    /**
     * @param location the candidate fix
     * @param maxAgeMs freshness window; pass [MAX_LAST_KNOWN_AGE_MS] when
     *   validating a last-known fallback, [MAX_FIX_AGE_MS] (default) for a fresh
     *   current-location request.
     */
    fun isUsable(
        location: Location?,
        maxAgeMs: Long = MAX_FIX_AGE_MS,
        nowElapsedRealtimeNanos: Long = SystemClock.elapsedRealtimeNanos()
    ): Boolean {
        if (location == null) return false

        val lat = location.latitude
        val lon = location.longitude

        // NaN / Infinite
        if (lat.isNaN() || lon.isNaN() || lat.isInfinite() || lon.isInfinite()) return false

        // Out of geographic range
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return false

        // (0,0) Null-Island sentinel — never a genuine fix
        if (abs(lat) < NULL_ISLAND_EPS && abs(lon) < NULL_ISLAND_EPS) return false

        // Accuracy: only reject when the OS reports an absurd value. A missing
        // accuracy is tolerated (don't drop a real fix in an emergency just
        // because the metadata is absent), but a present-yet-garbage one fails.
        if (location.hasAccuracy()) {
            if (location.accuracy <= 0f || location.accuracy > MAX_ACCURACY_METERS) return false
        }

        // Staleness — elapsedRealtimeNanos is monotonic, immune to wall-clock
        // changes / NTP jumps.
        val ageMs = (nowElapsedRealtimeNanos - location.elapsedRealtimeNanos) / 1_000_000L
        if (ageMs < 0L || ageMs > maxAgeMs) return false

        return true
    }
}
