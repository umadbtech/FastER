package com.faster.festival.ui.pinch.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/** Neutral background shown before a real GPS fix exists. Never a simulated map. */
private val MapPlaceholderColor = Color(0xFFE8EDF2)

/**
 * Emergency map background. Renders a REAL Google Map centred on the user's
 * live GPS fix, with [content] overlaid on top.
 *
 * No data is ever fabricated: the responder marker, medical-station markers and
 * the route line are drawn ONLY when a real backend supplies those coordinates
 * (i.e. when the corresponding parameters are non-null/non-empty). Until then
 * the map shows just the user's real location.
 *
 * When no GPS fix is available yet ([userLatLng] is null) a neutral surface is
 * shown instead — never a simulated map.
 */
@Composable
fun LiveMapBackground(
    userLatLng: LatLng? = null,
    medicalStations: List<LatLng> = emptyList(),
    responderLatLng: LatLng? = null,
    showRoute: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        if (userLatLng != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
            }
            LaunchedEffect(userLatLng, responderLatLng) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                )
            }
            val uiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false
                )
            }
            val mapProperties = remember { MapProperties(isMyLocationEnabled = false) }

            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = uiSettings,
                properties = mapProperties
            ) {
                Marker(
                    state = MarkerState(position = userLatLng),
                    title = "Your location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                )
                responderLatLng?.let { responder ->
                    Marker(
                        state = MarkerState(position = responder),
                        title = "Responder",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                    if (showRoute) {
                        Polyline(
                            points = listOf(responder, userLatLng),
                            color = Color(0xFF1A237E),
                            width = 8f
                        )
                    }
                }
                medicalStations.forEach { station ->
                    Marker(
                        state = MarkerState(position = station),
                        title = "Medical station",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
        } else {
            Box(Modifier.matchParentSize().background(MapPlaceholderColor))
        }
        content()
    }
}
