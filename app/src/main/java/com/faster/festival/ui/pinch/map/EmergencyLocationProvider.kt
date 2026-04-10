package com.faster.festival.ui.pinch.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "EmergencyLocation"

// Fallback: downtown Nashville (festival area)
val DEFAULT_LOCATION = LatLng(36.1627, -86.7816)

/**
 * Get the user's current location using FusedLocationProviderClient.
 * Returns DEFAULT_LOCATION if permission is missing or location unavailable.
 */
suspend fun getCurrentLocation(context: Context): LatLng {
    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        Log.w(TAG, "No location permission, using default")
        return DEFAULT_LOCATION
    }

    return suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        try {
            client.getCurrentLocation(
                if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
                    cont.resume(LatLng(location.latitude, location.longitude))
                } else {
                    Log.w(TAG, "Location null, trying last known")
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            cont.resume(LatLng(last.latitude, last.longitude))
                        } else {
                            cont.resume(DEFAULT_LOCATION)
                        }
                    }.addOnFailureListener {
                        cont.resume(DEFAULT_LOCATION)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "getCurrentLocation failed", e)
                cont.resume(DEFAULT_LOCATION)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException", e)
            cont.resume(DEFAULT_LOCATION)
        }

        cont.invokeOnCancellation { cts.cancel() }
    }
}
