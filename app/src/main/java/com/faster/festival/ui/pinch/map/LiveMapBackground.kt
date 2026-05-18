package com.faster.festival.ui.pinch.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng

/**
 * Emergency map background wrapper.
 *
 * Currently uses the polished fake map (FakeEmergencyMapBackground).
 * When ready to switch to real Google Maps, replace the body of this
 * function with the RealMapLayer implementation — all screen call sites
 * remain unchanged since they only call LiveMapBackground.
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
    // Derive the visual state from the parameters
    val mapState = when {
        showRoute && responderLatLng != null -> FakeMapState.HELP_ON_THE_WAY
        responderLatLng != null -> FakeMapState.HELP_ARRIVED
        else -> FakeMapState.DEFAULT
    }

    FakeEmergencyMapBackground(
        mapState = mapState,
        modifier = modifier,
        content = content
    )
}
