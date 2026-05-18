package com.faster.festival.data.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.faster.festival.BuildConfig
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Sole owner of "what coordinate goes into the SOS payload."
 *
 * Two modes, picked at build time via `BuildConfig`:
 *
 *  • **Mock** — `BuildConfig.DEBUG || BuildConfig.USE_TEST_LOCATION`. Returns
 *    the fixed staging Medical Point coordinate from
 *    `Pinch_SOS_Frontend_Implementation_Guide.md` §"Test Locations". Used by
 *    QA / emulator / demo APKs where the device has no usable GPS.
 *
 *  • **Real GPS** — production. Uses Google Play Services
 *    `FusedLocationProviderClient` to get a high-accuracy fix, falls back to
 *    last-known if the current-location request times out. Returns `null`
 *    when there's no fix at all — the caller decides what to do
 *    (the SOS pipeline currently sends `0.0, 0.0` as a "GPS unavailable"
 *    sentinel that dispatch can detect).
 *
 * Test coordinates are intentionally NOT visible to `SosRepositoryImpl` or
 * any other production request builder — keeping the mock isolated behind a
 * single build-config gate satisfies the spec's "Never hardcode test
 * coordinates directly inside production SOS request builders" rule.
 */
class SosLocationProvider(
    private val context: Context,
    /**
     * Override seam for tests. Production code uses the build-config-driven
     * default which is `true` only when explicitly enabled via `.env`.
     */
    private val useMockLocation: Boolean = BuildConfig.DEBUG || BuildConfig.USE_TEST_LOCATION
) {

    @SuppressLint("MissingPermission")
    suspend fun currentFix(timeoutMs: Long = 5_000L): Location? {
        if (useMockLocation) {
            Timber.tag(TAG).d("USE_TEST_LOCATION on — returning staging Medical Point fix")
            return STAGING_LOCATION
        }

        if (!hasLocationPermission()) {
            Timber.tag(TAG).w("Location permission not granted — no fix available")
            return null
        }

        val client = LocationServices.getFusedLocationProviderClient(context)
        val req = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(timeoutMs)
            .build()
        val current: Location? = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont: CancellableContinuation<Location?> ->
                client.getCurrentLocation(req, null)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        if (current != null) return current

        // Fallback — last known. Pre-existing fix is still better than nothing.
        return suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private companion object {
        const val TAG = "SosLocationProvider"

        /**
         * Staging Medical Point — `Pinch_SOS_Frontend_Implementation_Guide.md`
         * §"Test Locations". Only returned when [useMockLocation] is on.
         */
        val STAGING_LOCATION: Location = Location("test_mock").apply {
            latitude = 33.198741
            longitude = -97.137414
            accuracy = 8f
        }
    }
}
