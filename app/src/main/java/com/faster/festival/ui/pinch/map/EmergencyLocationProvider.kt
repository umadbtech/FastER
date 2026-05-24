package com.faster.festival.ui.pinch.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.faster.festival.core.location.LocationValidation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "EmergencyLocation"

/**
 * Get the user's current location using FusedLocationProviderClient.
 *
 * Returns `null` when permission is missing or no real fix is available — the
 * caller (map / UI) shows a neutral state in that case. Every fix is run through
 * [LocationValidation], so we never substitute (and never render) a hardcoded,
 * zero, NaN or stale coordinate for a missing fix.
 */
suspend fun getCurrentLocation(context: Context): LatLng? {
    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        Log.w(TAG, "No location permission; no fix available")
        return null
    }

    return suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        try {
            client.getCurrentLocation(
                if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                if (LocationValidation.isUsable(location)) {
                    Log.d(TAG, "Got valid location: ${location.latitude}, ${location.longitude}")
                    cont.resume(LatLng(location.latitude, location.longitude))
                } else {
                    Log.w(TAG, "Location missing/invalid, trying recent last known")
                    client.lastLocation.addOnSuccessListener { last ->
                        cont.resume(
                            if (LocationValidation.isUsable(last, LocationValidation.MAX_LAST_KNOWN_AGE_MS))
                                LatLng(last.latitude, last.longitude)
                            else null
                        )
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "getCurrentLocation failed", e)
                cont.resume(null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException", e)
            cont.resume(null)
        }

        cont.invokeOnCancellation { cts.cancel() }
    }
}
