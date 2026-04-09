package com.faster.festival.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.faster.festival.R
import com.faster.festival.ui.viewmodel.ProvisionStep
import com.faster.festival.ui.viewmodel.ProvisionUiState
import com.faster.festival.ui.viewmodel.ProvisionViewModel

private val ProvRed = Color(0xFFD32F2F)
private val ProvDarkRed = Color(0xFFB71C1C)
private val ProvGreen = Color(0xFF2E7D32)
private val ProvBlue = Color(0xFF1976D2)
private val ProvTextDark = Color(0xFF1A1A1A)
private val ProvTextMedium = Color(0xFF555555)
private val ProvTextLight = Color(0xFF888888)
private val ProvGray = Color(0xFFE0E0E0)
private val ProvBg = Color(0xFFF5F5F5)
private val ProvCardBg = Color(0xFF2A2A2A)

/**
 * Reusable Provision Flow screen.
 *
 * @param viewModel The ProvisionViewModel driving the state machine
 * @param onBackClick Called when user presses back on the first screen
 * @param onComplete Called when the entire flow is done (Go Home / continue onboarding)
 * @param onLocationPermissionRequest Called to trigger the system location permission dialog
 * @param showBackOnSplash Whether to show back button on splash (true from FasterScreen, false during onboarding)
 * @param completeButtonText Text for the final button ("Go Home" vs "Continue")
 */
@Composable
fun ProvisionFlowScreen(
    viewModel: ProvisionViewModel,
    onBackClick: () -> Unit = {},
    onComplete: () -> Unit = {},
    onLocationPermissionRequest: () -> Unit = {},
    showBackOnSplash: Boolean = true,
    completeButtonText: String = "Go Home"
) {
    val state by viewModel.uiState.collectAsState()

    when (state.currentStep) {
        ProvisionStep.Splash -> ProvisionSplashScreen(
            onPairWristband = { viewModel.proceedToLocationPermission() },
            onBack = if (showBackOnSplash) onBackClick else null
        )
        ProvisionStep.LocationPermission -> ProvisionLocationScreen(
            onAlwaysAllow = {
                viewModel.onLocationPermissionResult(true)
                onLocationPermissionRequest()
            },
            onAllowWhileUsing = {
                viewModel.onLocationPermissionResult(true)
                onLocationPermissionRequest()
            },
            onDoNotAllow = {
                viewModel.onLocationPermissionResult(false)
            },
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.PowerOn -> ProvisionPowerOnScreen(
            onActivate = { viewModel.activatePairingMode() },
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.Connecting -> ProvisionConnectingScreen(
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.Detected -> ProvisionDetectedScreen(
            onPair = { viewModel.pairWristband() },
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.Confirm -> ProvisionConfirmScreen(
            isLoading = state.isLoading,
            onFinishPairing = { viewModel.finishPairing() },
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.Complete -> ProvisionCompleteScreen(
            onQuickGuide = { viewModel.showQuickGuide() },
            onBack = { viewModel.goBack() }
        )
        ProvisionStep.QuickGuide -> WristbandQuickGuideScreen(
            onDone = {
                viewModel.reset()
                onComplete()
            },
            completeButtonText = completeButtonText,
            onBack = { viewModel.goBack() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Shared Components
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionTopBar(
    title: String = "",
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ProvTextDark
                )
            }
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProvTextDark,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProvisionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ProvRed,
            disabledContainerColor = ProvGray
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun BottomButtonBar(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        content()
    }
}

@Composable
private fun WristbandGif(
    drawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(drawableRes)
            .build(),
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(220.dp),
        contentScale = ContentScale.Fit
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. PROVISION SPLASH — About FASTER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionSplashScreen(
    onPairWristband: () -> Unit,
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (onBack != null) {
            ProvisionTopBar(title = "About FASTER", onBack = onBack)
        } else {
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(16.dp)
            )
            Text(
                text = "About FASTER",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Our Mission
            Text(
                text = "Our Mission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProvTextMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // How We Keep You Safe
            Text(
                text = "How We Keep You Safe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                BulletPoint("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
                BulletPoint("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
                BulletPoint("Ut enim ad minim veniam, quis nostrud exercitation.")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FASTER Wristband
            Text(
                text = "FASTER Wristband",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProvTextMedium
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        BottomButtonBar {
            ProvisionPrimaryButton(
                text = "Pair Your Wristband",
                onClick = onPairWristband
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "•  ",
            style = MaterialTheme.typography.bodyMedium,
            color = ProvTextMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ProvTextMedium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. PROVISION LOCATION — Permission Request
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionLocationScreen(
    onAlwaysAllow: () -> Unit,
    onAllowWhileUsing: () -> Unit,
    onDoNotAllow: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ProvisionTopBar(onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        // Permission card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Allow Location Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProvTextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A description should be a short, complete sentence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProvTextMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Always Allow
                Button(
                    onClick = onAlwaysAllow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProvBlue)
                ) {
                    Text("Always Allow", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Allow While Using
                OutlinedButton(
                    onClick = onAllowWhileUsing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        "Allow While Using",
                        fontWeight = FontWeight.SemiBold,
                        color = ProvTextDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Do Not Allow
                TextButton(
                    onClick = onDoNotAllow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Do Not Allow",
                        color = ProvRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. PROVISION POWER ON — Turn on wristband
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionPowerOnScreen(
    onActivate: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisionTopBar(title = "Turn on Your Wristband", onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        WristbandGif(
            drawableRes = R.drawable.wristband_active,
            contentDescription = "Turn on wristband"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Press the Center Button",
            style = MaterialTheme.typography.bodyLarge,
            color = ProvTextMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomButtonBar {
            ProvisionPrimaryButton(
                text = "Activate Pairing Mode",
                onClick = onActivate
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. PROVISION CONNECTING — Searching for wristband
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionConnectingScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisionTopBar(title = "Pair Your Wristband", onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        WristbandGif(
            drawableRes = R.drawable.wristband_pair,
            contentDescription = "Pairing wristband"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Press and Hold the Center Button\nfor 4 seconds",
            style = MaterialTheme.typography.bodyLarge,
            color = ProvTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        CircularProgressIndicator(
            color = ProvRed,
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomButtonBar {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Having an Issue?",
                    color = ProvTextLight,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. PROVISION DETECTED — Wristband found
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionDetectedScreen(
    onPair: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisionTopBar(title = "Wristband Detected", onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        WristbandGif(
            drawableRes = R.drawable.wristband_active,
            contentDescription = "Wristband detected"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your device sees your wristband",
            style = MaterialTheme.typography.bodyLarge,
            color = ProvTextMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomButtonBar {
            ProvisionPrimaryButton(
                text = "Pair Your Wristband",
                onClick = onPair
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. PROVISION CONFIRM — Finish pairing
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionConfirmScreen(
    isLoading: Boolean,
    onFinishPairing: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisionTopBar(title = "Finish Pairing Your Wristband", onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        WristbandGif(
            drawableRes = R.drawable.wristband_confirm,
            contentDescription = "Confirm pairing"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Click the button to pair your wristband\nand enter the event",
            style = MaterialTheme.typography.bodyLarge,
            color = ProvTextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomButtonBar {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ProvRed)
                }
            } else {
                ProvisionPrimaryButton(
                    text = "Finish Pairing Your Wristband",
                    onClick = onFinishPairing
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. PROVISION COMPLETE — Successfully paired
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProvisionCompleteScreen(
    onQuickGuide: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvisionTopBar(onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        // Large blue checkmark
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(4.dp, ProvBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = ProvBlue,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Wristband Successfully Paired",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ProvTextDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your wristband is set up for the event!",
            style = MaterialTheme.typography.bodyLarge,
            color = ProvTextMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        BottomButtonBar {
            ProvisionPrimaryButton(
                text = "FASTER Wristband Quick Guide",
                onClick = onQuickGuide
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. WRISTBAND QUICK GUIDE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WristbandQuickGuideScreen(
    onDone: () -> Unit,
    completeButtonText: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ProvisionTopBar(title = "FASTER Wristband Quick Guide", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Enter the Event
            QuickGuideItem(
                number = "1.",
                title = "Enter the Event",
                description = "Tap your wristband at the gate",
                icon = Icons.Default.MeetingRoom
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Emergency Help Button
            QuickGuideItem(
                number = "2.",
                title = "Emergency Help Button",
                description = "Press the center button for 4 seconds to alert medical staff you need help.",
                icon = Icons.Default.Sos
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Contactless Payments
            QuickGuideItem(
                number = "3.",
                title = "Contactless Payments",
                description = "Add funds to your account and enable tap to pay on your wristband.",
                icon = Icons.Default.Contactless
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Want to Learn More
            Text(
                text = "Want to Learn More?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Navigate to your account settings and click About FASTER for more information about FASTER",
                style = MaterialTheme.typography.bodyMedium,
                color = ProvTextMedium
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        BottomButtonBar {
            ProvisionPrimaryButton(
                text = completeButtonText,
                onClick = onDone
            )
        }
    }
}

@Composable
private fun QuickGuideItem(
    number: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProvTextDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = ProvTextMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Placeholder illustration card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProvBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProvRed,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
