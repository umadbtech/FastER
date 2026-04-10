package com.faster.festival.ui.pinch.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Real Google Map showing user location, responder position, medical stations,
 * and a polyline route from responder to user.
 */
@Composable
fun EmergencyMapView(
    userLocation: LatLng,
    responderLocation: LatLng,
    medicalStations: List<LatLng>,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()

    // Fit all markers in the camera bounds
    LaunchedEffect(userLocation, responderLocation) {
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(userLocation)
        boundsBuilder.include(responderLocation)
        medicalStations.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        )
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = mapProperties
    ) {
        // User location marker (dark pin)
        Marker(
            state = MarkerState(position = userLocation),
            title = "Your Location",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
        )

        // Responder marker (red — help on the way)
        Marker(
            state = MarkerState(position = responderLocation),
            title = "Help On The Way",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        // Medical station markers
        medicalStations.forEach { station ->
            Marker(
                state = MarkerState(position = station),
                title = "Medical Station",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        // Route polyline from responder to user
        Polyline(
            points = listOf(responderLocation, userLocation),
            color = Color(0xFF1A237E),
            width = 8f
        )
    }
}
