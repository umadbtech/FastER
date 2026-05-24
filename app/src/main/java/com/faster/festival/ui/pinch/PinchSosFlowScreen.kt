package com.faster.festival.ui.pinch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.festival.R
import com.faster.festival.domain.sos.PinchUiStatus
import com.faster.festival.ui.pinch.components.BottomCard
import com.faster.festival.ui.pinch.components.PinchGray
import com.faster.festival.ui.pinch.components.PinchGreen
import com.faster.festival.ui.pinch.components.PinchPrimaryButton
import com.faster.festival.ui.pinch.components.PinchRed
import com.faster.festival.ui.pinch.components.PinchSecondaryButton
import com.faster.festival.ui.pinch.components.PinchTextDark
import com.faster.festival.ui.pinch.components.PinchTextLight
import com.faster.festival.ui.pinch.components.PinchTextMedium
import com.faster.festival.ui.pinch.components.PinchWhite
import com.faster.festival.ui.pinch.components.RadioOption
import com.faster.festival.ui.pinch.map.LiveMapBackground
import kotlin.math.roundToInt

// Predefined festival zones for the "Where is the emergency?" modal.
private val LOCATION_ZONES = listOf(
    Triple("main_stage_crowd", "Main Stage — Crowd", "Festival ground, west"),
    Triple("east_pavilion", "East Pavilion", "Food court area"),
    Triple("north_gate", "North Gate", "Entrance & rideshare"),
    Triple("lakeside_stage", "Lakeside Stage", "Adjacent to water")
)

private val MEDICAL_OPTIONS = listOf(
    "severe_allergy" to "Severe allergy",
    "asthma" to "Asthma",
    "diabetic" to "Diabetic",
    "on_blood_thinners" to "On blood thinners",
    "none" to "None"
)

private val INCIDENT_OPTIONS = listOf(
    "unconscious" to "Unconscious",
    "trouble_breathing" to "Trouble breathing",
    "crowd_crush" to "Crowd crush",
    "allergy" to "Allergy",
    "bleeding" to "Bleeding",
    "panic_attack" to "Panic attack"
)

// ═══════════════════════════════════════════════════════════════════════════
// ENTRY — state-driven router
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PinchSosFlowScreen(
    viewModel: PinchSosViewModel,
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.initLocationIfNeeded(context) }

    // Auto-clear the transient snackbar.
    LaunchedEffect(state.transientMessage) {
        if (state.transientMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearTransient()
        }
    }

    when (state.screen) {
        PinchScreen.Landing -> LandingScreen(
            onGetHelp = { viewModel.navigateToSwipe() },
            onClose = onBackClick
        )
        PinchScreen.Swipe -> SwipeForHelpScreen(
            state = state,
            onSwiped = { viewModel.swipeForHelp() },
            onCancel = { viewModel.backToLanding() }
        )
        PinchScreen.Live -> LiveTrackingScreen(
            state = state,
            viewModel = viewModel,
            onExit = onBackClick
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 1. LANDING (kept) — two ways to get help
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LandingScreen(onGetHelp: () -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(PinchWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            Image(
                painter = painterResource(R.drawable.faster_logo),
                contentDescription = "FASTER",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Two Ways to Get Medical Help",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PinchTextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PinchRed.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Hold the Button on Your Wristband",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "For 4 seconds to get help",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("or", style = MaterialTheme.typography.bodyLarge, color = PinchTextLight)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGetHelp,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
            ) {
                Text("Get Help Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "By sending the alert, I agree to share necessary\npersonal information with Medical Staff.",
                style = MaterialTheme.typography.bodySmall,
                color = PinchTextLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(32.dp))
        }
        CloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding())
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 2. SWIPE FOR HELP
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SwipeForHelpScreen(
    state: PinchSosUiState,
    onSwiped: () -> Unit,
    onCancel: () -> Unit
) {
    LiveMapBackground(userLatLng = state.userLatLng) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            BottomCard {
                Text(
                    "Swipe for Help",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "By swiping the button you alert medical staff.\nHelp arrives without further questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                SwipeToConfirm(onConfirmed = onSwiped)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SwipeToConfirm(onConfirmed: () -> Unit) {
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 220.dp.toPx() }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var confirmed by remember { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(swipeOffset, tween(120), label = "swipe")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(PinchRed.copy(alpha = 0.1f))
            .border(2.dp, PinchRed.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
    ) {
        Text(
            "Get Help Now",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = PinchRed.copy(alpha = 0.6f)
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(56.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .background(PinchRed)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!confirmed && swipeOffset > maxSwipePx * 0.7f) {
                                confirmed = true
                                onConfirmed()
                            }
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { _, drag ->
                            if (!confirmed) {
                                swipeOffset = (swipeOffset + drag).coerceIn(0f, maxSwipePx)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Swipe", tint = PinchWhite, modifier = Modifier.size(28.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. LIVE TRACKING — map + draggable bottom sheet + overlays
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveTrackingScreen(
    state: PinchSosUiState,
    viewModel: PinchSosViewModel,
    onExit: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // REAL map: only the user marker is drawn — the contract supplies no
        // responder coordinates, so we never fabricate a responder pin/route.
        LiveMapBackground(userLatLng = state.userLatLng) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                if (state.isTerminal) {
                    TerminalCard(
                        status = state.terminalStatus ?: PinchUiStatus.Completed,
                        onDone = { viewModel.dismissTerminal(); onExit() }
                    )
                } else {
                    LiveBottomSheet(state = state, viewModel = viewModel)
                }
            }
        }

        // Top "Alert received" toast chip.
        AlertReceivedChip(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)
        )

        // Transient snackbar.
        state.transientMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PinchTextDark.copy(alpha = 0.92f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(msg, color = PinchWhite, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Overlays (More Info / sub-sheets / Location).
        when (state.activeSheet) {
            PinchSheet.AddMoreInfo -> AddMoreInfoSheet(
                onPhone = { viewModel.openSheet(PinchSheet.Phone) },
                onMedical = { viewModel.openSheet(PinchSheet.Medical) },
                onWhatHappened = { viewModel.openSheet(PinchSheet.WhatHappened) },
                onDone = { viewModel.closeSheet() }
            )
            PinchSheet.Phone -> PhoneSheet(
                submitting = state.sheetSubmitting,
                onSend = { viewModel.submitPhone(it) },
                onSkip = { viewModel.closeSheet() }
            )
            PinchSheet.Medical -> MedicalSheet(
                submitting = state.sheetSubmitting,
                onSend = { viewModel.submitMedical(it) },
                onSkip = { viewModel.closeSheet() }
            )
            PinchSheet.WhatHappened -> WhatHappenedSheet(
                submitting = state.sheetSubmitting,
                onSend = { cats, notes -> viewModel.submitIncident(cats, notes) },
                onSkip = { viewModel.submitIncidentDeclined() }
            )
            PinchSheet.Location -> LocationSheet(
                state = state,
                submitting = state.sheetSubmitting,
                onUseCurrent = { viewModel.useCurrentLocation() },
                onSelectZone = { choice, desc -> viewModel.selectLocationZone(choice, desc) },
                onCancel = { viewModel.closeSheet() }
            )
            PinchSheet.None -> Unit
        }
    }
}

@Composable
private fun AlertReceivedChip(state: PinchSosUiState, modifier: Modifier = Modifier) {
    val text = when {
        state.isOffline -> "Offline — reconnecting…"
        state.isSending -> "Sending alert…"
        else -> "Alert received · Medical staff notified"
    }
    val icon = if (state.isOffline) Icons.Default.WifiOff else Icons.Default.CheckCircle
    val tint = if (state.isOffline) PinchTextMedium else PinchGreen
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PinchWhite)
            .border(1.dp, PinchGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isSending) {
            CircularProgressIndicator(color = PinchRed, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = PinchTextDark, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LiveBottomSheet(state: PinchSosUiState, viewModel: PinchSosViewModel) {
    BottomCard {
        StageIndicator(stage = state.uiStatus.stage)
        Spacer(Modifier.height(14.dp))

        Text(
            text = state.uiStatus.headline,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark
        )
        state.etaLabel?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = PinchTextMedium)
        }
        // Responder card (mock: person · message · ● LIVE). The contract carries
        // NO responder name/role/distance — only `responder.message` — so we show
        // the live message honestly rather than fabricating a name.
        if (!state.isTerminal && !state.isSending && !state.responderMessage.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            ResponderCard(
                message = state.responderMessage!!,
                live = state.alertId != null
            )
        }

        if (state.isFailed) {
            Spacer(Modifier.height(12.dp))
            RetryBanner(message = state.failureMessage, onRetry = { viewModel.retrySend() })
        }

        if (state.isCancelPending) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = PinchRed, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cancel requested — waiting for staff to confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextMedium
                )
            }
        } else if (state.uiStatus == PinchUiStatus.CancelDenied) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Cancel was not approved — help is still coming.",
                style = MaterialTheme.typography.bodySmall,
                color = PinchRed,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        // Controls row: Cancel · More Info · Location (outlined pills, per the mock)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlAction(
                icon = Icons.Default.Close,
                label = "Cancel",
                tint = PinchRed,
                enabled = state.canCancel,
                onClick = { viewModel.requestCancel() },
                modifier = Modifier.weight(1f)
            )
            ControlAction(
                icon = Icons.Default.Add,
                label = "More Info",
                tint = PinchTextDark,
                enabled = state.alertId != null,
                onClick = { viewModel.openSheet(PinchSheet.AddMoreInfo) },
                modifier = Modifier.weight(1f)
            )
            ControlAction(
                icon = Icons.Default.LocationOn,
                label = "Location",
                tint = PinchTextDark,
                enabled = state.alertId != null,
                onClick = { viewModel.openSheet(PinchSheet.Location) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Outlined pill control (Cancel / More Info / Location) — matches the live mock. */
@Composable
private fun ControlAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveTint = if (enabled) tint else PinchTextLight
    val borderColor = if (enabled) tint.copy(alpha = 0.5f) else PinchGray
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = effectiveTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = effectiveTint, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Responder card — person avatar, the backend `responder.message`, and a green
 * ● LIVE badge while the alert is being tracked. The Pinch contract returns no
 * responder name/role/distance, so we surface the live message rather than
 * fabricate identity details.
 */
@Composable
private fun ResponderCard(message: String, live: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PinchGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PinchRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = PinchRed, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Medical staff",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = PinchTextDark
            )
            Text(message, style = MaterialTheme.typography.bodySmall, color = PinchTextLight)
        }
        if (live) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(PinchGreen))
                Spacer(Modifier.width(4.dp))
                Text(
                    "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PinchGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** SWIPED → ARRIVED → RESOLVED three-step progress (icon dots, per the live mock). */
@Composable
private fun StageIndicator(stage: Int) {
    val steps = listOf(
        Icons.AutoMirrored.Filled.ArrowForward to "SWIPED",
        Icons.Default.Navigation to "ARRIVED",
        Icons.Default.VerifiedUser to "RESOLVED"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (icon, label) ->
            val active = index <= stage
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (active) PinchRed else Color.Transparent)
                        .border(2.dp, if (active) PinchRed else PinchGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (active) PinchWhite else PinchTextLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) PinchRed else PinchTextLight,
                    fontSize = 10.sp
                )
            }
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (index < stage) PinchRed else PinchGray)
                )
            }
        }
    }
}

@Composable
private fun RetryBanner(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PinchRed.copy(alpha = 0.08f))
            .border(1.dp, PinchRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message ?: "Couldn't reach dispatch.",
            style = MaterialTheme.typography.bodySmall,
            color = PinchRed,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed),
            shape = RoundedCornerShape(20.dp)
        ) { Text("Retry") }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. TERMINAL CARDS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TerminalCard(status: PinchUiStatus, onDone: () -> Unit) {
    val (title, body, color) = when (status) {
        PinchUiStatus.Cancelled -> Triple(
            "Help Alert Cancelled",
            "Your help alert has been cancelled. Reach out again any time you need help.",
            PinchTextMedium
        )
        PinchUiStatus.Rejected -> Triple(
            "Alert Closed",
            "This alert was closed (false alarm / not actionable). You can start a new alert if you still need help.",
            PinchTextMedium
        )
        else -> Triple(
            "Emergency Resolved",
            "Medical staff have responded to your help alert. Follow any next steps they suggested.",
            PinchGreen
        )
    }
    BottomCard {
        Icon(
            imageVector = if (status == PinchUiStatus.Completed) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (status == PinchUiStatus.Completed) PinchGreen else PinchRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = PinchTextMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        PinchPrimaryButton(text = "Done", onClick = onDone)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. OVERLAY SHEETS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SheetScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Column(Modifier.fillMaxSize()) {
            // Only the area ABOVE the card dismisses — taps on the card itself
            // (fields, chips, buttons) are never swallowed.
            Box(Modifier.weight(1f).fillMaxWidth().clickable(onClick = onDismiss))
            content()
        }
    }
}

@Composable
private fun SheetCard(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PinchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PinchTextDark, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Close, "Close",
                    tint = PinchTextLight,
                    modifier = Modifier.size(24.dp).clickable(onClick = onClose)
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun AddMoreInfoSheet(
    onPhone: () -> Unit,
    onMedical: () -> Unit,
    onWhatHappened: () -> Unit,
    onDone: () -> Unit
) {
    SheetScrim(onDismiss = onDone) {
        SheetCard(title = "Add more info", onClose = onDone) {
            Text("All optional. Each step has Skip.", style = MaterialTheme.typography.bodyMedium, color = PinchTextLight)
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Default.Phone, "Phone number", "For staff to call if needed", onPhone)
            Spacer(Modifier.height(10.dp))
            InfoRow(Icons.Default.FavoriteBorder, "Medical info", "Allergies, conditions", onMedical)
            Spacer(Modifier.height(10.dp))
            InfoRow(Icons.Default.WarningAmber, "What happened", "Help staff prepare", onWhatHappened)
            Spacer(Modifier.height(20.dp))
            PinchPrimaryButton(text = "Done", onClick = onDone)
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PinchGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PinchRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = PinchTextDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PinchTextLight)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PinchTextLight, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PhoneSheet(submitting: Boolean, onSend: (String) -> Unit, onSkip: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    SheetScrim(onDismiss = onSkip) {
        SheetCard(title = "Phone number", onClose = onSkip) {
            Text("Used only if staff cannot find you.", style = MaterialTheme.typography.bodyMedium, color = PinchTextLight)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Your phone") },
                placeholder = { Text("(312) 555-0148") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinchRed, cursorColor = PinchRed)
            )
            Spacer(Modifier.height(16.dp))
            SheetActions(
                submitting = submitting,
                sendEnabled = phone.filter { it.isDigit() }.length >= 7,
                onSkip = onSkip,
                onSend = { onSend(phone) }
            )
        }
    }
}

@Composable
private fun MedicalSheet(submitting: Boolean, onSend: (Set<String>) -> Unit, onSkip: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    SheetScrim(onDismiss = onSkip) {
        SheetCard(title = "Medical info", onClose = onSkip) {
            Text("Tap anything that applies.", style = MaterialTheme.typography.bodyMedium, color = PinchTextLight)
            Spacer(Modifier.height(12.dp))
            MEDICAL_OPTIONS.forEach { (key, label) ->
                CheckOption(label = label, checked = selected.contains(key)) {
                    // "None" is mutually exclusive with the specific conditions —
                    // don't hand medics a contradictory "None + Asthma" set.
                    selected = when {
                        key == "none" -> if (selected.contains("none")) emptySet() else setOf("none")
                        else -> selected.toggle(key) - "none"
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            SheetActions(
                submitting = submitting,
                sendEnabled = selected.isNotEmpty(),
                onSkip = onSkip,
                onSend = { onSend(selected) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhatHappenedSheet(
    submitting: Boolean,
    onSend: (Set<String>, String) -> Unit,
    onSkip: () -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    SheetScrim(onDismiss = onSkip) {
        SheetCard(title = "What happened", onClose = onSkip) {
            Text("A few words help staff prepare.", style = MaterialTheme.typography.bodyMedium, color = PinchTextLight)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                INCIDENT_OPTIONS.forEach { (key, label) ->
                    SelectChip(label = label, selected = selected.contains(key)) {
                        selected = selected.toggle(key)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Describe… (optional)") },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinchRed, cursorColor = PinchRed)
            )
            Spacer(Modifier.height(16.dp))
            SheetActions(
                submitting = submitting,
                sendEnabled = selected.isNotEmpty() || notes.isNotBlank(),
                onSkip = onSkip,
                onSend = { onSend(selected, notes) }
            )
        }
    }
}

@Composable
private fun LocationSheet(
    state: PinchSosUiState,
    submitting: Boolean,
    onUseCurrent: () -> Unit,
    onSelectZone: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedZone by remember { mutableStateOf<Pair<String, String>?>(null) }
    val gpsText = state.userLatLng?.let { "GPS · %.6f, %.6f".format(it.latitude, it.longitude) }
        ?: "GPS unavailable"
    SheetScrim(onDismiss = onCancel) {
        SheetCard(title = "Where is the emergency?", onClose = onCancel) {
            RadioOption(
                label = "My current location",
                selected = selectedZone == null,
                onClick = { selectedZone = null },
                trailingContent = {
                    Icon(Icons.Default.MyLocation, null, tint = PinchRed, modifier = Modifier.size(20.dp))
                }
            )
            Text(gpsText, style = MaterialTheme.typography.bodySmall, color = PinchTextLight, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Spacer(Modifier.height(10.dp))
            LOCATION_ZONES.forEach { (choice, title, subtitle) ->
                RadioOption(
                    label = "$title — $subtitle",
                    selected = selectedZone?.first == choice,
                    onClick = { selectedZone = choice to title }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            if (submitting) {
                CircularProgressIndicator(color = PinchRed, modifier = Modifier.size(28.dp))
            } else {
                Row {
                    PinchSecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    PinchPrimaryButton(
                        text = "Update",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val zone = selectedZone
                            if (zone == null) onUseCurrent() else onSelectZone(zone.first, zone.second)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActions(submitting: Boolean, sendEnabled: Boolean, onSkip: () -> Unit, onSend: () -> Unit) {
    if (submitting) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinchRed, modifier = Modifier.size(28.dp))
        }
    } else {
        Row {
            PinchSecondaryButton(text = "Skip", onClick = onSkip, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            PinchPrimaryButton(text = "Send to staff", onClick = onSend, modifier = Modifier.weight(1f), enabled = sendEnabled)
        }
    }
}

@Composable
private fun CheckOption(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (checked) 2.dp else 1.dp,
                color = if (checked) PinchRed else PinchGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (checked) PinchRed.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PinchRed)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) PinchRed else PinchGray, RoundedCornerShape(20.dp))
            .background(if (selected) PinchRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) PinchRed else PinchTextDark,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(end = 12.dp, top = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(PinchWhite.copy(alpha = 0.95f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, "Close", tint = PinchRed, modifier = Modifier.size(22.dp))
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (contains(value)) this - value else this + value
