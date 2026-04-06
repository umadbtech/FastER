package com.faster.festival.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Light theme palette (consistent with other screens)
private val NotifBg = Color(0xFFF7F7F7)
private val NotifWhite = Color.White
private val NotifTextDark = Color(0xFF222222)
private val NotifTextMedium = Color(0xFF333333)
private val NotifTextLight = Color(0xFF666666)
private val NotifDivider = Color(0xFFE0E0E0)
private val NotifGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pushNotifications by remember { mutableStateOf(false) }
    var emergencyAlerts by remember { mutableStateOf(true) }
    var festivalUpdates by remember { mutableStateOf(true) }
    var exclusivePromotions by remember { mutableStateOf(true) }
    var smsNotifications by remember { mutableStateOf(false) }
    var emailNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotifBg)
    ) {
        // Top App Bar
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

            // Push Notifications toggle
            NotificationToggleRow(
                label = "Allow Push Notifications",
                checked = pushNotifications,
                onCheckedChange = { pushNotifications = it }
            )

            HorizontalDivider(color = NotifDivider)

            // Sub-options: visible only when push is enabled
            AnimatedVisibility(
                visible = pushNotifications,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    NotificationToggleRow(
                        label = "Emergency Alerts",
                        checked = emergencyAlerts,
                        onCheckedChange = { emergencyAlerts = it },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider, modifier = Modifier.padding(start = 16.dp))

                    NotificationToggleRow(
                        label = "Festival Updates",
                        checked = festivalUpdates,
                        onCheckedChange = { festivalUpdates = it },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider, modifier = Modifier.padding(start = 16.dp))

                    NotificationToggleRow(
                        label = "Exclusive Promotions",
                        checked = exclusivePromotions,
                        onCheckedChange = { exclusivePromotions = it },
                        indent = true
                    )
                    HorizontalDivider(color = NotifDivider)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section header
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
                checked = smsNotifications,
                onCheckedChange = { smsNotifications = it }
            )

            HorizontalDivider(color = NotifDivider)

            NotificationToggleRow(
                label = "Allow Email Notifications",
                checked = emailNotifications,
                onCheckedChange = { emailNotifications = it }
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
