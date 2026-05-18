package com.faster.festival.wristband.ui.sos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Mount once at the root of MainActivity (sibling to the NavHost / Scaffold)
 * so the SOS alert can take over the screen from any tab — including covering
 * the bottom navigation bar and system insets, per emergency-UX requirements.
 */
@Composable
fun SosOverlayHost(
    viewModel: SosAlertViewModel,
    modifier: Modifier = Modifier.fillMaxSize(),
    onClose: () -> Unit = { viewModel.dismiss() }
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val visible = s.event != null && !s.canceled
    if (visible) {
        SosAlertScreen(viewModel = viewModel, onClose = onClose)
    }
}
