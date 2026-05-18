package com.faster.festival.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.faster.festival.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FasterScreen(
    onPinchHelp: () -> Unit = {},
    onPairWristband: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    onReconnect: () -> Unit = {},
    onSosHistory: () -> Unit = {}
) {
    // First time the user lands on the FASTER tab on Android 13+, request
    // POST_NOTIFICATIONS so the SOS foreground alert can actually appear.
    // Idempotent + silent — denial doesn't block the rest of the screen.
    com.faster.festival.presentation.sos.EnsureNotificationPermission()
    // Combines persisted Room row with live BLE Mesh ConnectionStatus.
    val viewModel: com.faster.festival.wristband.ui.faster.FasterWristbandViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.faster.festival.wristband.ui.faster.FasterWristbandViewModel.Factory(
            pairedRepo = com.faster.festival.di.DatabaseModule.wristbandRepository,
            observeConnection = com.faster.festival.wristband.di.WristbandModule.observeConnection,
            reconnect = com.faster.festival.wristband.di.WristbandModule.reconnect,
            unpair = com.faster.festival.wristband.di.WristbandModule.unpair
        )
    )
    val uiState by viewModel.state.collectAsState()
    val paired = uiState.paired
    val mode = uiState.mode
    val connection = uiState.connection

    var showUnpairDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    if (showUnpairDialog && paired != null) {
        UnpairConfirmDialog(
            wristbandId = paired.wristbandId,
            confirming = uiState.unpairing,
            onConfirm = {
                viewModel.unpair { showUnpairDialog = false }
            },
            onDismiss = { showUnpairDialog = false }
        )
    }

    val isPaired = paired != null
    val isConnected = connection is com.faster.festival.wristband.domain.model.ConnectionStatus.Connected
    val batteryLevel = paired?.batteryLevel ?: 0
    val firmwareVersion = paired?.firmwareVersion ?: "—"
    val wristbandId = paired?.wristbandId ?: "—"
    val lastSynced = when {
        !isPaired -> "Not paired"
        paired.lastSeenAt != null -> formatPairedAt(paired.lastSeenAt)
        else -> formatPairedAt(paired.pairedAt)
    }
    val pairedDate = paired?.let { formatPairedDate(it.pairedAt) } ?: "—"

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FASTER",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { scaffoldPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp  // leave room for the floating FAB at the bottom
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Wristband Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Wristband Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connection Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BluetoothConnected,
                                    contentDescription = null,
                                    tint = if (isConnected) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Connection",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isConnected) Color(0xFF16A34A)
                                            else MaterialTheme.colorScheme.error
                                        )
                                )
                                Text(
                                    text = if (isConnected) "Connected" else "Disconnected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isConnected) Color(0xFF16A34A)
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Battery Section — animated indicator
                        AnimatedBatteryIndicator(batteryLevel = batteryLevel)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Last Synced
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Last Synced",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = lastSynced,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Wristband Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Wristband Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        WristbandInfoRow(
                            label = "Wristband ID",
                            value = wristbandId,
                            icon = Icons.Default.Watch
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        WristbandInfoRow(
                            label = "Firmware",
                            value = "v$firmwareVersion",
                            icon = Icons.Default.Info
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        WristbandInfoRow(
                            label = "Paired",
                            value = pairedDate,
                            icon = Icons.Default.BluetoothConnected
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

       

            item {
                WristbandActionBlock(
                    mode = mode,
                    onPair = onPairWristband,
                    onOpenDashboard = onOpenDashboard,
                    onReconnect = {
                        viewModel.reconnect()
                        onReconnect()
                    },
                    onUnpair = { showUnpairDialog = true }
                )
            }

            // SOS History menu card
            item {
                Card(
                    onClick = onSosHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sos,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOS History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View your past emergency alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            // Features Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wristband Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Contactless,
                            title = "NFC Tap to Pay",
                            description = "Make cashless payments at vendors by tapping your wristband"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FeatureRow(
                            icon = Icons.Default.Sos,
                            title = "Emergency SOS",
                            description = "Press and hold the wristband button for 3 seconds to alert medical staff",
                            iconTint = Color.White,
                            iconBackground = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                            )
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FeatureRow(
                            icon = Icons.Default.NearMe,
                            title = "Friend Detection",
                            description = "Get notified when friends with wristbands are nearby"
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Floating Pinch Help Emergency FAB — pinned to bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            PinchHelpFab(onClick = onPinchHelp)
        }
    }
    }
}

@Composable
private fun WristbandInfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val BlueGradient = androidx.compose.ui.graphics.Brush.linearGradient(
    colors = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1))
)

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color = Color.White,
    iconBackground: androidx.compose.ui.graphics.Brush = BlueGradient
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Pair Wristband Button — blue gradient style with watch icon
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PairWristbandButton(
    label: String = "Connect your FASTER Wristband",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BlueGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Date/time formatters for the paired wristband card
private fun formatPairedAt(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hr ago"
        else -> "${minutes / 1440} days ago"
    }
}

private fun formatPairedDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}

// ═══════════════════════════════════════════════════════════════════════════════
// Wristband action block — branches on FasterWristbandUiState.Mode
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WristbandActionBlock(
    mode: com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode,
    onPair: () -> Unit,
    onOpenDashboard: () -> Unit,
    onReconnect: () -> Unit,
    onUnpair: () -> Unit
) {
    when (mode) {
        com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode.NotPaired -> {
            PairWristbandButton(
                label = "Pair Your Wristband",
                onClick = onPair
            )
        }
        com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode.Connected -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PairWristbandButton(
                    label = "Open Wristband Dashboard",
                    onClick = onOpenDashboard
                )
                DangerOutlinedButton(label = "Unpair Wristband", onClick = onUnpair)
            }
        }
        com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode.Disconnected -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PairWristbandButton(
                    label = "Reconnect",
                    onClick = onReconnect
                )
                OutlinedButton(
                    onClick = onOpenDashboard,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Open Wristband Dashboard") }
                DangerOutlinedButton(label = "Unpair Wristband", onClick = onUnpair)
            }
        }
        com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode.Reconnecting,
        com.faster.festival.wristband.ui.faster.FasterWristbandUiState.Mode.Connecting -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFB68E1A).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFB68E1A),
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            "Reconnecting to your wristband…",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB68E1A)
                        )
                    }
                }
                DangerOutlinedButton(label = "Unpair Wristband", onClick = onUnpair)
            }
        }
    }
}

@Composable
private fun DangerOutlinedButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFB3261E)
        )
    ) {
        Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UnpairConfirmDialog(
    wristbandId: String,
    confirming: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unpair $wristbandId?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("This will:")
                Spacer(Modifier.height(8.dp))
                Text("• Disconnect your wristband")
                Text("• Remove its security keys from this phone")
                Text("• Stop SOS, telemetry, and reconnect")
                Spacer(Modifier.height(8.dp))
                Text(
                    "You'll need to pair it again to use FastER features.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
                enabled = !confirming
            ) {
                Text(
                    if (confirming) "Unpairing…" else "Unpair Wristband",
                    color = Color(0xFFB3261E),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss, enabled = !confirming) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Pinch Help Emergency CTA — simple solid red button, icon + label
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PinchHelpFab(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Get medical help"
            },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Sos,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Get Medical Help",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Animated Battery Indicator — battery shape with fill animation + counter
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedBatteryIndicator(batteryLevel: Int) {
    // Animate fill from 0 to target on first composition
    val startAnim = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(batteryLevel) { startAnim.value = true }

    val animatedFill by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnim.value) batteryLevel / 100f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "battery_fill"
    )

    val animatedCount by androidx.compose.animation.core.animateIntAsState(
        targetValue = if (startAnim.value) batteryLevel else 0,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "battery_count"
    )

    // Pulse glow when low
    val isLow = batteryLevel <= 20
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "battery_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "battery_pulse_alpha"
    )

    val baseColor = when {
        batteryLevel > 50 -> Color(0xFF16A34A)
        batteryLevel > 20 -> Color(0xFFFFC107)
        else -> Color(0xFFD32F2F)
    }
    val fillColor = if (isLow) baseColor.copy(alpha = pulseAlpha) else baseColor
    val trackColor = Color(0xFFE5E7EB)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(baseColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (batteryLevel > 90) Icons.Default.BatteryFull
                    else Icons.Default.Battery4Bar,
                    contentDescription = null,
                    tint = baseColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "Battery",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        batteryLevel > 50 -> "Healthy"
                        batteryLevel > 20 -> "Charge soon"
                        else -> "Low — charge now"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = baseColor
                )
            }
        }

        // Right: animated battery shape with bar fill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$animatedCount%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = baseColor
            )

            // Battery body + tip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(trackColor)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFill)
                            .clip(RoundedCornerShape(3.dp))
                            .background(fillColor)
                    )
                }
                // Battery tip cap
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 12.dp)
                        .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        .background(trackColor)
                )
            }
        }
    }
}
