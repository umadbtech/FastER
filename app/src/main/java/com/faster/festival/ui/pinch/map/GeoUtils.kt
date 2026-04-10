package com.faster.festival.ui.pinch.map

import com.google.android.gms.maps.model.LatLng
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Calculate a new LatLng offset from [origin] by [distanceMeters] at [bearingDegrees].
 *
 * Uses the Haversine "destination point given distance and bearing" formula.
 *
 * @param origin Starting coordinate
 * @param distanceMeters Distance in meters
 * @param bearingDegrees Compass bearing in degrees (0 = North, 90 = East, 180 = South, 270 = West)
 */
fun offsetLatLng(origin: LatLng, distanceMeters: Double, bearingDegrees: Double): LatLng {
    val lat1 = Math.toRadians(origin.latitude)
    val lng1 = Math.toRadians(origin.longitude)
    val bearing = Math.toRadians(bearingDegrees)
    val angularDistance = distanceMeters / EARTH_RADIUS_METERS

    val lat2 = asin(
        sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing)
    )
    val lng2 = lng1 + atan2(
        sin(bearing) * sin(angularDistance) * cos(lat1),
        cos(angularDistance) - sin(lat1) * sin(lat2)
    )

    return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
}

/**
 * Generate the responder marker position offset from the user.
 * Default bearing 35° (NE direction, matching the wireframe diagonal path).
 */
fun responderPosition(userLocation: LatLng, distanceMeters: Double = 250.0): LatLng {
    return offsetLatLng(userLocation, distanceMeters, bearingDegrees = 35.0)
}

/**
 * Generate 3 medical station markers around the user at varied bearings and distances.
 */
fun medicalStationPositions(userLocation: LatLng): List<LatLng> {
    return listOf(
        offsetLatLng(userLocation, 180.0, bearingDegrees = 320.0),  // NW
        offsetLatLng(userLocation, 220.0, bearingDegrees = 60.0),   // NE
        offsetLatLng(userLocation, 200.0, bearingDegrees = 160.0)   // SE
    )
}
