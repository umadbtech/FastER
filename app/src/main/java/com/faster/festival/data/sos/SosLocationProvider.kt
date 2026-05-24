package com.faster.festival.data.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.faster.festival.core.location.LocationValidation
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
 * **Real GPS only.** There is no mock / test / hardcoded coordinate path — in
 * any build, debug or release. The fix comes from Google Play Services
 * `FusedLocationProviderClient` and is run through [LocationValidation] before
 * it is trusted, so a fake, zero, NaN, out-of-range or stale reading can never
 * reach the SOS pipeline.
 *
 * Flow:
 *  1. Bail if location permission is missing or location services are off.
 *  2. Request a high-accuracy current fix (bounded by `timeoutMs`).
 *  3. If that fix is valid, use it.
 *  4. Otherwise fall back to last-known — but only if it is *recent and valid*
 *     (see [LocationValidation.MAX_LAST_KNOWN_AGE_MS]).
 *  5. If nothing valid is available, return `null`. The caller decides what to
 *     do — the SOS pipeline sends an explicit (0,0) "GPS unavailable" sentinel
 *     so an emergency still dispatches, never a fabricated coordinate.
 *
 * To exercise this on an emulator, set the device's emulated GPS via the
 * extended controls — that produces a *real* OS fix through FLP, which is the
 * correct way to test rather than injecting coordinates inside the app.
 */
class SosLocationProvider(
    private val context: Context
) {

    @SuppressLint("MissingPermission")
    suspend fun currentFix(timeoutMs: Long = 5_000L): Location? {
        if (!hasLocationPermission()) {
            Timber.tag(TAG).w("Location permission not granted — no fix available")
            return null
        }
        if (!isLocationEnabled()) {
            Timber.tag(TAG).w("Location services disabled — no fix available")
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
        if (LocationValidation.isUsable(current)) {
            Timber.tag(TAG).d(
                "Real high-accuracy fix acquired (acc=%sm)",
                current?.accuracy?.let { "%.0f".format(it) } ?: "?"
            )
            return current
        }
        Timber.tag(TAG).w("Current fix unusable (missing/invalid/stale) — trying recent last-known")

        // Fallback — last known, but only if recent and valid. A pre-existing
        // real fix beats nothing; a stale or junk one does not.
        val last: Location? = suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        return if (LocationValidation.isUsable(last, LocationValidation.MAX_LAST_KNOWN_AGE_MS)) {
            Timber.tag(TAG).i("Using recent validated last-known fix")
            last
        } else {
            Timber.tag(TAG).w("No usable GPS fix — caller will signal GPS-unavailable")
            null
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

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    private companion object {
        const val TAG = "SosLocationProvider"
    }
}
