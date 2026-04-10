package com.faster.festival.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.faster.festival.ui.viewmodel.NotificationSettingsViewModel

private val NotifBg = Color(0xFFF7F7F7)
private val NotifWhite = Color.White
private val NotifTextDark = Color(0xFF222222)
private val NotifTextMedium = Color(0xFF333333)
private val NotifDivider = Color(0xFFE0E0E0)
private val NotifGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current

    // Android 13+ notification permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setPushEnabled(true)
        }
    }

    // Register FCM token with backend when push is enabled
    LaunchedEffect(prefs.pushEnabled) {
        if (prefs.pushEnabled) {
            viewModel.registerDeviceToken()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotifBg)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Notifications",
                    fontWeight = FontWeight.Bold,
                    color = NotifTextDark
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NotifTextDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NotifWhite
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Master push toggle
            NotificationToggleRow(
                label = "Allow Push Notifications",
                checked = prefs.pushEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.setPushEnabled(true)
                        } else {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.setPushEnabled(enabled)
                    }
                }
            )

            HorizontalDivider(color = NotifDivider)

            // Sub-options visible when push is enabled
            AnimatedVisibility(
                visible = prefs.pushEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    NotificationToggleRow(
                        label = "Emergency Alerts",
                        checked = prefs.emergencyAlerts,
                        onCheckedChange = { viewModel.setEmergencyAlerts(it) },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider, modifier = Modifier.padding(start = 16.dp))

                    NotificationToggleRow(
                        label = "Festival Updates",
                        checked = prefs.festivalUpdates,
                        onCheckedChange = { viewModel.setFestivalUpdates(it) },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider, modifier = Modifier.padding(start = 16.dp))

                    NotificationToggleRow(
                        label = "Exclusive Promotions",
                        checked = prefs.exclusivePromotions,
                        onCheckedChange = { viewModel.setExclusivePromotions(it) },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Text & Email Notifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NotifTextDark,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            HorizontalDivider(color = NotifDivider)

            NotificationToggleRow(
                label = "Allow SMS Notifications",
                checked = prefs.smsNotifications,
                onCheckedChange = { viewModel.setSmsNotifications(it) }
            )

            HorizontalDivider(color = NotifDivider)

            NotificationToggleRow(
                label = "Allow Email Notifications",
                checked = prefs.emailNotifications,
                onCheckedChange = { viewModel.setEmailNotifications(it) }
            )

            HorizontalDivider(color = NotifDivider)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (indent) 16.dp else 0.dp,
                top = 14.dp,
                bottom = 14.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (indent) NotifTextMedium else NotifTextDark,
            fontWeight = if (indent) FontWeight.Normal else FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NotifWhite,
                checkedTrackColor = NotifGreen,
                uncheckedThumbColor = NotifWhite,
                uncheckedTrackColor = Color(0xFFBDBDBD),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
