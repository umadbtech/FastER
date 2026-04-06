package com.faster.festival.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.festival.R
import com.faster.festival.ui.viewmodel.PinchHelpState
import com.faster.festival.ui.viewmodel.PinchHelpViewModel
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════════
// PINCH HELP FLOW — Color Palette
// ═══════════════════════════════════════════════════════════════════════════════

private val PinchRed = Color(0xFFD32F2F)
private val PinchDarkRed = Color(0xFFB71C1C)
private val PinchBg = Color(0xFFF5F5F5)
private val PinchWhite = Color.White
private val PinchTextDark = Color(0xFF1A1A1A)
private val PinchTextMedium = Color(0xFF444444)
private val PinchTextLight = Color(0xFF888888)
private val PinchGreen = Color(0xFF2E7D32)
private val PinchMapGreen = Color(0xFFC8E6C9)

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN ENTRY — State-driven router
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PinchHelpScreen(
    viewModel: PinchHelpViewModel,
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    when (state.currentState) {
        PinchHelpState.Landing -> LandingScreen(
            onSwipeHelp = { viewModel.navigateToSwipe() },
            onBackClick = onBackClick
        )
        PinchHelpState.SwipeForHelp -> SwipeForHelpScreen(
            onAlertSent = { viewModel.swipeToAlert() },
            onCancel = onBackClick
        )
        PinchHelpState.AlertSent -> AlertSentScreen(
            onAnswerCall = { viewModel.proceedToAnswerCall() },
            onCancel = { viewModel.requestCancel() }
        )
        PinchHelpState.AnswerCall -> AnswerCallScreen(
            dispatcherName = state.dispatcherName,
            dispatcherPhone = state.dispatcherPhone,
            onHelpDispatched = { viewModel.helpOnTheWay() }
        )
        PinchHelpState.OnTheWay -> HelpOnTheWayScreen(
            etaMinutes = state.etaMinutes,
            responderName = state.responderName,
            onArrived = { viewModel.helpArrived() },
            onCancel = { viewModel.requestCancel() }
        )
        PinchHelpState.HelpArrived -> HelpArrivedScreen(
            responderName = state.responderName,
            onInProgress = { viewModel.helpInProgress() }
        )
        PinchHelpState.InProgress -> HelpInProgressScreen(
            onResolved = { viewModel.emergencyResolved() }
        )
        PinchHelpState.Resolved -> EmergencyResolvedScreen(
            onProvideFeedback = { viewModel.showFeedbackPrompt() },
            onDone = onBackClick
        )
        PinchHelpState.FeedbackPrompt -> FeedbackPromptScreen(
            onStartSurvey = { viewModel.startFeedbackSurvey() },
            onSkip = onBackClick
        )
        PinchHelpState.FeedbackSurvey -> FeedbackSurveyScreen(
            rating = state.feedbackRating,
            comment = state.feedbackComment,
            onRatingChange = { viewModel.setFeedbackRating(it) },
            onCommentChange = { viewModel.setFeedbackComment(it) },
            onSubmit = { viewModel.submitFeedback() },
            onSkip = onBackClick
        )
        PinchHelpState.FeedbackThankYou -> FeedbackThankYouScreen(
            onDone = {
                viewModel.reset()
                onBackClick()
            }
        )
        PinchHelpState.CancelConfirm -> CancelConfirmScreen(
            onConfirmCancel = { viewModel.confirmCancel() },
            onDismiss = { viewModel.dismissCancel() }
        )
        PinchHelpState.Cancelled -> HelpCancelledScreen(
            onDone = {
                viewModel.reset()
                onBackClick()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAP BACKGROUND — Shared map-like background for help screens
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MapBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Simulated map background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PinchMapGreen)
        ) {
            // Road lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFFAED581))
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .background(Color(0xFFAED581))
            )

            // Location pins
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
                    .offset(y = (-20).dp),
                tint = PinchRed
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 60.dp),
                tint = PinchRed.copy(alpha = 0.6f)
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 200.dp, start = 40.dp),
                tint = PinchRed.copy(alpha = 0.6f)
            )
        }

        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BOTTOM CARD — Shared bottom sheet-like card overlay
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PinchWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFDDDDDD))
            )
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. LANDING SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LandingScreen(
    onSwipeHelp: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PinchTextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FASTER Logo
        Image(
            painter = painterResource(R.drawable.faster_red),
            contentDescription = "FASTER Logo",
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Two Ways to Get Medical Help",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Option 1: Wristband
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PinchRed.copy(alpha = 0.05f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = PinchRed
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Hold the Button on Your Wristband",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinchRed,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Press and hold the wristband button for 3 seconds to send an immediate alert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PinchTextMedium
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Option 2: Swipe for Help
        Button(
            onClick = onSwipeHelp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
        ) {
            Icon(
                imageVector = Icons.Default.SwipeRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Swipe for Help",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Call Button
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchDarkRed)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Emergency Call",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. SWIPE FOR HELP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SwipeForHelpScreen(
    onAlertSent: () -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 200.dp.toPx() }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(100),
        label = "swipe_offset"
    )

    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Swipe for Help",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Medical staff will be sent to your location.\nMake sure location services are on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Swipe track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(PinchRed.copy(alpha = 0.1f))
                        .border(2.dp, PinchRed.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
                ) {
                    // Track label
                    Text(
                        text = "Slide to Alert →",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PinchRed.copy(alpha = 0.5f)
                    )

                    // Swipe thumb
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
                                        if (swipeOffset > maxSwipePx * 0.7f) {
                                            onAlertSent()
                                        }
                                        swipeOffset = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        swipeOffset =
                                            (swipeOffset + dragAmount).coerceIn(0f, maxSwipePx)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Swipe",
                            tint = PinchWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = onCancel) {
                    Text(
                        text = "Cancel",
                        color = PinchTextLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. ALERT SENT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AlertSentScreen(
    onAnswerCall: () -> Unit,
    onCancel: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = PinchGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Help Alert Sent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A FASTER Dispatcher has received your alert.\nPlease stay at your location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAnswerCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Answer Call", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onCancel) {
                    Text("Cancel Help Alert", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. ANSWER CALL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnswerCallScreen(
    dispatcherName: String,
    dispatcherPhone: String,
    onHelpDispatched: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                // Call icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PinchGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = PinchWhite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Answer Call",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dispatcherName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )

                Text(
                    text = dispatcherPhone,
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Answer your phone to speak with the medical dispatcher",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onHelpDispatched,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                ) {
                    Text("Help is On the Way", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. HELP ON THE WAY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpOnTheWayScreen(
    etaMinutes: Int,
    responderName: String,
    onArrived: () -> Unit,
    onCancel: () -> Unit
) {
    MapBackground {
        // Responder pin on map
        Icon(
            imageVector = Icons.Default.MedicalServices,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .offset(x = 100.dp, y = 180.dp),
            tint = PinchGreen
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help is On the Way",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ETA: $etaMinutes min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinchRed
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Responder info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PinchBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PinchRed.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = PinchRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = responderName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PinchTextDark
                            )
                            Text(
                                text = "Medical staff are on route to your location",
                                style = MaterialTheme.typography.bodySmall,
                                color = PinchTextMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onArrived,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                ) {
                    Text("Help Arrived", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onCancel) {
                    Text("Cancel Help Alert", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. HELP ARRIVED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpArrivedScreen(
    responderName: String,
    onInProgress: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = PinchGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Help Arrived",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$responderName is now at your location.\nFollow their instructions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                ) {
                    Text("Help in Progress", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. HELP IN PROGRESS — Step tracker
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpInProgressScreen(
    onResolved: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help in Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress stepper
                val steps = listOf(
                    "Alert Sent" to true,
                    "Dispatched" to true,
                    "On the Way" to true,
                    "Arrived" to true,
                    "In Progress" to true,
                    "Resolved" to false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, (label, completed) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completed) PinchRed else Color(0xFFE0E0E0)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (completed) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PinchWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (completed) PinchTextDark else PinchTextLight,
                                textAlign = TextAlign.Center,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                maxLines = 2
                            )
                        }
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .height(2.dp)
                                    .background(
                                        if (completed && steps.getOrNull(index + 1)?.second == true)
                                            PinchRed else Color(0xFFE0E0E0)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Medical staff are currently assisting you.\nPlease follow their instructions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResolved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinchGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Resolved", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. EMERGENCY RESOLVED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmergencyResolvedScreen(
    onProvideFeedback: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = PinchGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Emergency Resolved",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We're glad you're safe. Your emergency has been resolved and closed by the medical team.",
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Follow-up care: If any issues re-appear, contact the on-site medical station or call the helpline.",
            style = MaterialTheme.typography.bodyMedium,
            color = PinchTextLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Provide Feedback
        Text(
            text = "Provide Feedback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "FASTER appreciates your experience and input.\nHelp us improve our medical response.",
            style = MaterialTheme.typography.bodyMedium,
            color = PinchTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onProvideFeedback,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
        ) {
            Text("Submit Feedback", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDone) {
            Text("Done", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. FEEDBACK PROMPT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackPromptScreen(
    onStartSurvey: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Provide Feedback",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your feedback helps us improve emergency response for all festival attendees.",
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartSurvey,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
        ) {
            Text("Start Feedback", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Skip", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. FEEDBACK SURVEY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackSurveyScreen(
    rating: Int,
    comment: String,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Feedback Survey",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "How was your experience with FASTER's medical response?",
            style = MaterialTheme.typography.bodyMedium,
            color = PinchTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Star rating
        Text(
            text = "Rate your experience",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = PinchTextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { star ->
                Icon(
                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Star $star",
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onRatingChange(star) },
                    tint = if (star <= rating) Color(0xFFFFC107) else Color(0xFFCCCCCC)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Comment
        Text(
            text = "Tell us more (optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = PinchTextDark,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Your feedback helps us improve...") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Using FASTER helps us improve.\nEvery bit of feedback makes a difference.",
            style = MaterialTheme.typography.bodySmall,
            color = PinchTextLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed),
            enabled = rating > 0
        ) {
            Text("Submit Feedback", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Skip", color = PinchTextLight, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. FEEDBACK THANK YOU
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackThankYouScreen(
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = PinchGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Thank You for Completing Our\nFeedback Survey",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your responses will help improve emergency services for all festival attendees.",
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 12. CANCEL CONFIRM
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelConfirmScreen(
    onConfirmCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Cancel Help Alert",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You will no longer receive medical help. If you still need a medical help, call 911.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirmCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Keep Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PinchTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Help alert status
                Text(
                    text = "Help Alert Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cancel Help Alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 13. HELP CANCELLED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpCancelledScreen(
    onDone: () -> Unit
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                        .padding(10.dp),
                    tint = PinchWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Help Alert Cancelled",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If you still need a medical help, call 911.\nYour safety is our priority.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Help Alert Status
                Text(
                    text = "Help Alert Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
