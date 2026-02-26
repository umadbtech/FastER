package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.CountDownTimer

/**
 * Screen 4: Wristband Pairing - 2-Step Flow
 * Step 1: Introduction and Pair button
 * Step 2: Countdown timer for pairing mode
 */
@Composable
fun WristbandScreen(
    modifier: Modifier = Modifier,
    formState: OnboardingFormState? = null,
    @Suppress("UNUSED_PARAMETER") onWristbandCodeChange: (String) -> Unit = {},
    onSkipPairing: () -> Unit = {},
    onPairingReady: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // Step state: INTRO or COUNTDOWN
    var currentStep by remember { mutableStateOf(WristbandStep.INTRO) }

    // Countdown state
    var remainingSeconds by remember { mutableStateOf(10) }
    var countdownTimer by remember { mutableStateOf<CountDownTimer?>(null) }

    // Handle cleanup when composable leaves
    DisposableEffect(Unit) {
        onDispose {
            countdownTimer?.cancel()
        }
    }

    // Show appropriate step
    when (currentStep) {
        WristbandStep.INTRO -> {
            WristbandIntroStep(
                onPairClick = {
                    // Transition to countdown
                    currentStep = WristbandStep.COUNTDOWN
                    remainingSeconds = 10

                    // Start countdown timer
                    countdownTimer = object : CountDownTimer(11000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            remainingSeconds = (millisUntilFinished / 1000).toInt()
                        }

                        override fun onFinish() {
                            remainingSeconds = 0
                            onPairingReady()
                        }
                    }.start()
                },
                onSkipClick = onSkipPairing,
                onBackClick = onBackPressed,
                modifier = modifier
            )
        }

        WristbandStep.COUNTDOWN -> {
            WristbandCountdownStep(
                remainingSeconds = remainingSeconds,
                onBackClick = {
                    countdownTimer?.cancel()
                    currentStep = WristbandStep.INTRO
                    remainingSeconds = 10
                    onBackPressed()
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Step 1: Wristband Intro Screen
 * Shows title, description, and "Pair Wristband" button
 */
@Composable
private fun WristbandIntroStep(
    onPairClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top: Back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = "Wristband",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 24.dp)
            )

            // Title (tvPairTitle)
            Text(
                text = "Pair Wristband",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Body text (tvPairBody)
            Text(
                text = "Once you connect your wristband, you will have to pay a transfer fee to disconnect or transfer your ticket.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary button: "Pair Wristband" (btnPairWristband)
            Button(
                onClick = onPairClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = "Pair Wristband",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Text button: "Skip Pairing the Wristband" (btnSkipPairing)
            TextButton(
                onClick = onSkipClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip Pairing the Wristband",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Step 2: Wristband Countdown Screen
 * Shows countdown timer and instructions
 */
@Composable
private fun WristbandCountdownStep(
    remainingSeconds: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top: Back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Content: Centered countdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title (tvCountdownTitle)
            Text(
                text = "Pair Wristband",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Subtitle (tvCountdownSubtitle)
            Text(
                text = "Hold the center button for 10 seconds to enter pairing mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Large countdown number (tvCountdownNumber)
            Text(
                text = remainingSeconds.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // "Seconds" label (tvCountdownLabel)
            Text(
                text = "Seconds",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Enum for wristband pairing steps
 */
enum class WristbandStep {
    INTRO, COUNTDOWN
}

