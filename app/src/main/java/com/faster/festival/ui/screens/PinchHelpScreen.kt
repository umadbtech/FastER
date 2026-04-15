package com.faster.festival.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.faster.festival.data.pinch.model.EmergencyCategory
import com.faster.festival.data.pinch.model.FeedbackConfig
import com.faster.festival.data.pinch.model.FeedbackQuestion
import com.faster.festival.ui.pinch.components.BackButton
import com.faster.festival.ui.pinch.components.BottomCard
import com.faster.festival.ui.pinch.components.MapBackground
import com.faster.festival.ui.pinch.map.LiveMapBackground
import com.faster.festival.ui.pinch.components.PinchDarkRed
import com.faster.festival.ui.pinch.components.PinchGray
import com.faster.festival.ui.pinch.components.PinchGreen
import com.faster.festival.ui.pinch.components.PinchPrimaryButton
import com.faster.festival.ui.pinch.components.PinchRed
import com.faster.festival.ui.pinch.components.PinchSecondaryButton
import com.faster.festival.ui.pinch.components.PinchTextDark
import com.faster.festival.ui.pinch.components.PinchTextLight
import com.faster.festival.ui.pinch.components.PinchTextMedium
import com.faster.festival.ui.pinch.components.PinchWhite
import com.faster.festival.ui.pinch.components.PinchAmber
import com.faster.festival.ui.pinch.components.PinchOrange
import com.faster.festival.ui.pinch.components.RadioOption
import com.faster.festival.ui.pinch.components.RatingSelector
import com.faster.festival.ui.pinch.components.TimelineIndicator
import com.faster.festival.ui.viewmodel.PinchHelpState
import com.faster.festival.ui.viewmodel.PinchHelpUiState
import com.faster.festival.ui.viewmodel.PinchHelpViewModel
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN ENTRY — State-driven router
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PinchHelpScreen(
    viewModel: PinchHelpViewModel,
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Load real device location once for all map screens
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.initLocationIfNeeded(context)
    }

    if (state.isLoading && state.currentState == PinchHelpState.Landing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinchRed)
        }
        return
    }

    when (state.currentState) {
        PinchHelpState.Landing -> LandingScreen(
            onSwipeHelp = { viewModel.navigateToSwipe() },
            onBackClick = onBackClick
        )
        PinchHelpState.SwipeForHelp -> SwipeForHelpScreen(
            state = state,
            onAlertSent = { viewModel.swipeToAlert() },
            onCancel = onBackClick
        )
        PinchHelpState.AlertSent -> AlertSentScreen(
            state = state,
            onAnswerCall = { viewModel.proceedToAnswerCall() },
            onCancel = { viewModel.requestCancel() }
        )
        PinchHelpState.AnswerCall -> AnswerCallScreen(
            state = state,
            onProvideMoreInfo = { viewModel.proceedToEmergencyLocation() }
        )
        PinchHelpState.EmergencyLocation -> EmergencyLocationScreen(
            state = state,
            onUseCurrentLocation = { viewModel.setUseCurrentLocation(true) },
            onTypedLocation = { viewModel.setUseCurrentLocation(false) },
            onCustomLocationChange = { viewModel.setCustomLocationText(it) },
            onNext = { viewModel.proceedToContactPhone() },
            onCancel = { viewModel.requestCancel() }
        )
        PinchHelpState.ContactPhone -> ContactPhoneScreen(
            state = state,
            onUseMyPhone = { viewModel.setUseMyPhone(true) },
            onUseDifferentPhone = { viewModel.setUseMyPhone(false) },
            onCustomPhoneChange = { viewModel.setCustomPhone(it) },
            onNext = { viewModel.proceedToCategorySelection() },
            onCancel = { viewModel.requestCancel() }
        )
        PinchHelpState.CategorySelection -> CategorySelectionScreen(
            state = state,
            onToggleCategory = { viewModel.toggleCategory(it) },
            onNext = { viewModel.proceedToAdditionalInfoChoice() }
        )
        PinchHelpState.AdditionalInfoChoice -> AdditionalInfoChoiceScreen(
            onYes = { viewModel.proceedToAdditionalInfoForm() },
            onNo = { viewModel.skipAdditionalInfo() },
            isLoading = state.isLoading
        )
        PinchHelpState.AdditionalInfoForm -> AdditionalInfoFormScreen(
            state = state,
            onInfoChange = { viewModel.setAdditionalInfo(it) },
            onSubmit = { viewModel.submitAdditionalInfo() },
            isLoading = state.isLoading
        )
        PinchHelpState.FormSubmitted -> FormSubmittedScreen(
            requestId = state.requestId,
            onContinue = { viewModel.helpOnTheWay() }
        )
        PinchHelpState.OnTheWay -> HelpOnTheWayScreen(
            state = state,
            onArrived = { viewModel.helpArrived() }
        )
        PinchHelpState.HelpArrived -> HelpArrivedScreen(
            state = state,
            onInProgress = { viewModel.helpInProgress() }
        )
        PinchHelpState.InProgress -> HelpInProgressScreen(
            state = state,
            onResolved = { viewModel.emergencyResolved() }
        )
        PinchHelpState.Resolved -> EmergencyResolvedScreen(
            state = state,
            onRatingChange = { viewModel.setOverallRating(it) },
            onSubmitFeedback = { viewModel.showFeedbackIntro() },
            onFeedbackSurvey = { viewModel.startFeedbackSurvey() }
        )
        PinchHelpState.FeedbackIntro -> FeedbackIntroScreen(
            state = state,
            config = state.feedbackConfig,
            onStartSurvey = { viewModel.startFeedbackSurvey() }
        )
        PinchHelpState.FeedbackSurvey -> FeedbackSurveyScreen(
            state = state,
            currentQuestion = viewModel.getCurrentQuestion(),
            isLast = viewModel.isLastQuestion(),
            onRatingChange = { qId, rating -> viewModel.setQuestionRating(qId, rating) },
            onNext = { viewModel.nextQuestion() }
        )
        PinchHelpState.FeedbackComplete -> FeedbackCompleteScreen(
            state = state,
            config = state.feedbackConfig,
            onDone = {
                viewModel.reset()
                onBackClick()
            }
        )
        PinchHelpState.CancelSwipe -> CancelSwipeScreen(
            onSwipeCancel = { viewModel.showCancelConfirmDialog() },
            onBack = { viewModel.cancelSwipeBack() }
        )
        PinchHelpState.CancelConfirm -> CancelConfirmScreen(
            onConfirmCancel = { viewModel.confirmCancel() },
            onGoBack = { viewModel.dismissCancel() }
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
// Shared: Top-right Cancel button for emergency form screens
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TopCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(end = 12.dp, top = 40.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(PinchWhite.copy(alpha = 0.95f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Cancel",
            tint = PinchRed,
            modifier = Modifier.size(22.dp)
        )
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = painterResource(R.drawable.faster_logo),
            contentDescription = "FASTER Logo",
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Two Ways to Get Medical Help",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PinchTextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Wristband option card
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = PinchRed.copy(alpha = 0.05f)
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hold the Button on Your Wristband",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "For 4 seconds to get help",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "or",
            style = MaterialTheme.typography.bodyLarge,
            color = PinchTextLight
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Get Help Now button
        Button(
            onClick = onSwipeHelp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
        ) {
            Text(
                text = "Get Help Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "By sending the alert, I agree to share necessary\npersonal information with Medical Staff.",
            style = MaterialTheme.typography.bodySmall,
            color = PinchTextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        }

        // Top-right cancel button overlay
        TopCancelButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. SWIPE FOR HELP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SwipeForHelpScreen(
    state: PinchHelpUiState,
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

    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
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
                    text = "By swiping the button, you are alerting\nthe medical staff that you need help.",
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
                    Text(
                        text = "Get Help Now",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PinchRed.copy(alpha = 0.5f)
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

                Spacer(modifier = Modifier.height(16.dp))

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
    state: PinchHelpUiState,
    onAnswerCall: () -> Unit,
    onCancel: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help Alert Sent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Medical staff have received your alert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 0
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Once your alert is received, medical staff will\ncall your phone number to get more details\nabout your emergency",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                PinchSecondaryButton(
                    text = "Cancel Help Alert",
                    onClick = onCancel
                )

                Spacer(modifier = Modifier.height(6.dp))

                PinchPrimaryButton(
                    text = "Answer Call",
                    onClick = onAnswerCall
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. ANSWER CALL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnswerCallScreen(
    state: PinchHelpUiState,
    onProvideMoreInfo: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Answer Call",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Answer the call from the medical staff to\nconfirm your emergency and provide\nmore details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "If you cannot answer the phone call, confirm\nyour emergency using the button below",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                PinchPrimaryButton(
                    text = "Provide More Information",
                    onClick = onProvideMoreInfo
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. EMERGENCY LOCATION
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmergencyLocationScreen(
    state: PinchHelpUiState,
    onUseCurrentLocation: () -> Unit,
    onTypedLocation: () -> Unit,
    onCustomLocationChange: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    MapBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            TopCancelButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Where is the emergency?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (state.useCurrentLocation) {
                    Text(
                        text = "Your Current Location",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinchTextLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = PinchRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.userLocationLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = PinchTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.userGpsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = PinchTextLight
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                RadioOption(
                    label = "My Current Location",
                    selected = state.useCurrentLocation,
                    onClick = onUseCurrentLocation,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (state.useCurrentLocation) PinchRed else PinchTextLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                RadioOption(
                    label = "Describe Location (Type Location)",
                    selected = !state.useCurrentLocation,
                    onClick = onTypedLocation
                )

                if (!state.useCurrentLocation) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.customLocationText,
                        onValueChange = onCustomLocationChange,
                        label = { Text("Describe your location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinchRed,
                            cursorColor = PinchRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PinchPrimaryButton(
                    text = "NEXT",
                    onClick = onNext,
                    enabled = state.useCurrentLocation || state.customLocationText.isNotBlank()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. CONTACT PHONE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ContactPhoneScreen(
    state: PinchHelpUiState,
    onUseMyPhone: () -> Unit,
    onUseDifferentPhone: () -> Unit,
    onCustomPhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    MapBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            TopCancelButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "What number can medical staff\nuse to contact you?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Show phone
                Text(
                    text = "Your Phone Number",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = PinchRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.useMyPhone) state.userPhone else state.customPhone.ifBlank { state.userPhone },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PinchTextDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                RadioOption(
                    label = "My Phone Number",
                    selected = state.useMyPhone,
                    onClick = onUseMyPhone,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (state.useMyPhone) PinchRed else PinchTextLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                RadioOption(
                    label = "Different Number (Type Number)",
                    selected = !state.useMyPhone,
                    onClick = onUseDifferentPhone
                )

                if (!state.useMyPhone) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.customPhone,
                        onValueChange = onCustomPhoneChange,
                        label = { Text("Phone number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinchRed,
                            cursorColor = PinchRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PinchPrimaryButton(
                    text = "Next",
                    onClick = onNext,
                    enabled = state.useMyPhone || state.customPhone.length >= 7
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. CATEGORY SELECTION
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelectionScreen(
    state: PinchHelpUiState,
    onToggleCategory: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PinchWhite)
    ) {
        // Top timeline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            TimelineIndicator(
                steps = listOf("1", "2", "3", "4", "5"),
                activeStep = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tell Us What Happened",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PinchTextDark
            )
        }

        // Scrollable categories
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            state.categories.forEach { category ->
                Spacer(modifier = Modifier.height(16.dp))
                CategorySection(
                    category = category,
                    selectedIds = state.selectedCategoryIds,
                    onToggle = onToggleCategory
                )
            }

            // OTHER option
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = if (state.selectedCategoryIds.contains("other")) 2.dp else 1.dp,
                        color = if (state.selectedCategoryIds.contains("other")) PinchRed else PinchGray,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(
                        if (state.selectedCategoryIds.contains("other")) PinchRed.copy(alpha = 0.1f) else Color.Transparent
                    )
                    .clickable { onToggleCategory("other") }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OTHER",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PinchTextDark
                )
            }

            if (state.selectedCategoryIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${state.selectedCategoryIds.size} item(s) selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Report line
        if (state.selectedCategoryIds.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Report the following symptoms:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PinchTextMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.selectedCategoryIds.forEach { id ->
                        val label = state.categories
                            .flatMap { it.items }
                            .find { it.id == id }?.label ?: id.uppercase()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PinchRed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = PinchWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            PinchPrimaryButton(
                text = "Next",
                onClick = onNext,
                enabled = state.selectedCategoryIds.isNotEmpty()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    category: EmergencyCategory,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        PinchRed
    }

    Text(
        text = category.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )

    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        category.items.forEach { item ->
            val isSelected = selectedIds.contains(item.id)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else PinchGray,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { onToggle(item.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) color else PinchTextDark
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. ADDITIONAL INFO CHOICE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdditionalInfoChoiceScreen(
    onYes: () -> Unit,
    onNo: () -> Unit,
    isLoading: Boolean
) {
    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Can you provide more\ninformation?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please describe your emergency, the\nmore you describe, the faster we can\nfind help. Use as many descriptors as\npossible. If help is for you, try to stay as\nstationary as possible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                RadioOption(
                    label = "Yes, I don't mind.",
                    selected = false,
                    onClick = onYes
                )

                Spacer(modifier = Modifier.height(10.dp))

                RadioOption(
                    label = "No, I cannot at this time.",
                    selected = false,
                    onClick = {
                        if (!isLoading) onNo()
                    }
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = PinchRed,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 9. ADDITIONAL INFO FORM
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdditionalInfoFormScreen(
    state: PinchHelpUiState,
    onInfoChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .windowInsetsPadding(WindowInsets.ime)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFBBBBBB))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TimelineIndicator(
                        steps = listOf("1", "2", "3", "4", "5"),
                        activeStep = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Provide more information about\nyour emergency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Please describe what is happening, the\nmore you describe, the faster we can\nfind help. Use as many descriptors as\npossible. If help is for you, try to stay as\nstationary as possible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.additionalInfo,
                        onValueChange = onInfoChange,
                        label = { Text("Provide more information here. Please be as detailed as possible.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinchRed,
                            cursorColor = PinchRed
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PinchRed,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        PinchPrimaryButton(
                            text = "Submit",
                            onClick = onSubmit,
                            enabled = state.additionalInfo.isNotBlank()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 10. FORM SUBMITTED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FormSubmittedScreen(
    requestId: String,
    onContinue: () -> Unit
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
                    modifier = Modifier.size(56.dp),
                    tint = PinchGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Form Submitted",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Medical staff have been alerted. They have\nbeen provided with the information you have\nsubmitted. For the safety of you and your\nfriend, Please stay put for\nhelp to find you more easily.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                PinchPrimaryButton(
                    text = "Return to Emergency Screen",
                    onClick = onContinue
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 11. HELP ON THE WAY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpOnTheWayScreen(
    state: PinchHelpUiState,
    onArrived: () -> Unit
) {
    // Auto-advance countdown: 20 seconds → HelpArrived
    var secondsLeft by remember { mutableIntStateOf(20) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsLeft--
        }
        onArrived()
    }

    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations,
        responderLatLng = state.responderLatLng,
        showRoute = true
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help is On the Way",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Arrives between ${state.etaStartTime}-${state.etaEndTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Medical staff are ${state.nearestStation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Remain in your location unless it is unsafe",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown timer
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PinchRed.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Arriving in ${secondsLeft}s",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchRed
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 12. HELP ARRIVED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpArrivedScreen(
    state: PinchHelpUiState,
    onInProgress: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations,
        responderLatLng = state.userLatLng
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help Arrived",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Identify yourself to the medical staff.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Medical staff are at your location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Remain in your location unless it is unsafe",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onInProgress) {
                    Text(
                        text = "Continue →",
                        color = PinchTextLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 13. HELP IN PROGRESS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpInProgressScreen(
    state: PinchHelpUiState,
    onResolved: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Help in Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Medical staff are responding to your help\nalert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                TimelineIndicator(
                    steps = listOf("1", "2", "3", "4", "5"),
                    activeStep = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Medical staff are at your location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Remain in your location unless it is unsafe",
                    style = MaterialTheme.typography.bodySmall,
                    color = PinchTextLight
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onResolved) {
                    Text(
                        text = "Continue →",
                        color = PinchTextLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 14. EMERGENCY RESOLVED
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmergencyResolvedScreen(
    state: PinchHelpUiState,
    onRatingChange: (Int) -> Unit,
    onSubmitFeedback: () -> Unit,
    onFeedbackSurvey: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFBBBBBB))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Emergency Resolved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Medical staff have responded to your\nhelp alert.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TimelineIndicator(
                        steps = listOf("1", "2", "3", "4", "5"),
                        activeStep = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Follow the next steps suggested by the\nmedical staff.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Provide Feedback section
                    Text(
                        text = "Provide Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "FASTER successfully connected me\nwith the help I needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RatingSelector(
                        maxRating = 5,
                        currentRating = state.overallRating,
                        onRatingSelected = onRatingChange
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Strongly\nDisagree",
                            style = MaterialTheme.typography.labelSmall,
                            color = PinchTextLight
                        )
                        Text(
                            text = "Strongly\nAgree",
                            style = MaterialTheme.typography.labelSmall,
                            color = PinchTextLight,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PinchPrimaryButton(
                        text = "Submit Feedback",
                        onClick = onSubmitFeedback
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = onFeedbackSurvey) {
                        Text(
                            text = "Feedback Survey",
                            color = PinchTextLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 15. FEEDBACK INTRO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackIntroScreen(
    state: PinchHelpUiState,
    config: FeedbackConfig?,
    onStartSurvey: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = config?.introTitle ?: "Thank You for Your Feedback",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = config?.introMessage ?: "Your response will help us improve our emergency response technology.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = config?.introCta ?: "If you have a few more minutes, complete our quick 3 question survey.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                PinchPrimaryButton(
                    text = "Feedback Survey",
                    onClick = onStartSurvey
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 16. FEEDBACK SURVEY (per-question)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackSurveyScreen(
    state: PinchHelpUiState,
    currentQuestion: FeedbackQuestion?,
    isLast: Boolean,
    onRatingChange: (String, Int) -> Unit,
    onNext: () -> Unit
) {
    if (currentQuestion == null) return

    val currentRating = state.questionRatings[currentQuestion.id] ?: 0

    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Text(
                    text = "Feedback Survey",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentQuestion.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = PinchTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                RatingSelector(
                    maxRating = currentQuestion.scaleMax,
                    currentRating = currentRating,
                    onRatingSelected = { onRatingChange(currentQuestion.id, it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentQuestion.scaleMinLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PinchTextLight
                    )
                    Text(
                        text = currentQuestion.scaleMaxLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PinchTextLight,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PinchPrimaryButton(
                    text = if (isLast) "Submit Feedback Survey" else "Next Question",
                    onClick = onNext,
                    enabled = currentRating > 0
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 17. FEEDBACK COMPLETE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FeedbackCompleteScreen(
    state: PinchHelpUiState,
    config: FeedbackConfig?,
    onDone: () -> Unit
) {
    LiveMapBackground(
        userLatLng = state.userLatLng,
        medicalStations = state.medicalStations
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = PinchGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = config?.completionTitle ?: "Thank You for Completing Our\nFeedback Survey",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PinchTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = config?.completionMessage ?: "Your responses will help us improve our\nemergency response technology.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                PinchPrimaryButton(
                    text = "Done",
                    onClick = onDone
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL CONFIRM DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL SWIPE — Swipe to cancel with "I do not need help"
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelSwipeScreen(
    onSwipeCancel: () -> Unit,
    onBack: () -> Unit
) {
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 200.dp.toPx() }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(100),
        label = "cancel_swipe"
    )

    MapBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            BottomCard {
                // Back arrow + title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PinchTextDark,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Help Alert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "If you are not in need of help, swipe to\ncancel the alert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PinchTextMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Swipe track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFF0F0F0))
                        .border(1.dp, PinchGray, RoundedCornerShape(28.dp))
                ) {
                    // Label
                    Text(
                        text = "I do not need help",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PinchTextMedium
                    )

                    // Swipe thumb
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                            .size(52.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(PinchTextDark)
                            .align(Alignment.CenterStart)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (swipeOffset > maxSwipePx * 0.7f) {
                                            onSwipeCancel()
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
                            contentDescription = "Swipe to cancel",
                            tint = PinchWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL CONFIRM — Modal dialog over map
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelConfirmScreen(
    onConfirmCancel: () -> Unit,
    onGoBack: () -> Unit
) {
    MapBackground {
        // Dim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            // Modal card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PinchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cancel Help Alert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "You will not receive medical help if you cancel the emergency.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel (destructive, red)
                        Button(
                            onClick = onConfirmCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PinchRed)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Go Back (neutral)
                        OutlinedButton(
                            onClick = onGoBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Go Back",
                                fontWeight = FontWeight.Bold,
                                color = PinchTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELP CANCELLED — Confirmation screen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HelpCancelledScreen(
    onDone: () -> Unit
) {
    MapBackground {
        // Dim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PinchWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Help Alert Cancelled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinchTextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "If you still need medical help, call 911\nor locate medical staff",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PinchTextMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Blue "Ok" button per wireframe
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    ) {
                        Text(
                            text = "Ok",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
