package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.faster.festival.ui.theme.FastERTheme

// ============================================================================
// SECTION 1: PROFILE CARD (Red Border)
// ============================================================================

@Composable
fun ProfileCardSection(
    name: String = "First Last",
    username: String? = null,
    onPersonalInfoClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.error
            ).brush
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Name + Description + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPersonalInfoClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    username?.let {
                        Text(
                            text = "@$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = "Update your personal information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (username != null) 4.dp else 0.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Row 2: Emergency Contacts + Icon + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEmergencyContactsClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// SECTION 2: DEVICE CARD (Dark Navy)
// ============================================================================

@Composable
fun DeviceCardSection(
    wristbandName: String = "FASTER Wristband",
    batteryPercentage: Int = 82,
    connectionStatus: String = "Strong Connection",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2340) // Dark Navy
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gray image placeholder (60x60)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFAAAAAA))
            )

            // Right: Device info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Device name
                Text(
                    text = wristbandName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Battery row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "$batteryPercentage%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Signal/Connection row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================================
// SECTION 3: MY SETTINGS
// ============================================================================

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Composable
fun MySettingsSection(
    onHealthClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onPaymentsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "My Settings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsMenuItem(
                    icon = Icons.Default.HealthAndSafety,
                    label = "Health",
                    onClick = onHealthClick,
                    showDivider = true
                )
                SettingsMenuItem(
                    icon = Icons.Default.Notifications,
                    label = "Notifications",
                    onClick = onNotificationsClick,
                    showDivider = true
                )
                SettingsMenuItem(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    onClick = onLocationClick,
                    showDivider = true
                )
                SettingsMenuItem(
                    icon = Icons.Default.Payment,
                    label = "Payments",
                    onClick = onPaymentsClick,
                    showDivider = false
                )
            }
        }
    }
}

// ============================================================================
// SECTION 4: SUPPORT
// ============================================================================

@Composable
fun SupportMenuItem(
    label: String,
    onClick: () -> Unit = {},
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Composable
fun SupportSection(
    onAboutClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onFaqClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Support",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SupportMenuItem(
                    label = "About FASTER",
                    onClick = onAboutClick,
                    showDivider = true
                )
                SupportMenuItem(
                    label = "Report an Issue",
                    onClick = onReportClick,
                    showDivider = true
                )
                SupportMenuItem(
                    label = "Terms & Conditions",
                    onClick = onTermsClick,
                    showDivider = true
                )
                SupportMenuItem(
                    label = "Privacy Policy",
                    onClick = onPrivacyClick,
                    showDivider = true
                )
                SupportMenuItem(
                    label = "FAQ",
                    onClick = onFaqClick,
                    showDivider = false
                )
            }
        }
    }
}

// ============================================================================
// SECTION 5: BOTTOM ACTIONS
// ============================================================================

@Composable
fun BottomActionsSection(
    onManageAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onManageAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Manage Account",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = "Logout ›",
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = TextDecoration.Underline
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = onLogoutClick)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ============================================================================
// MAIN PROFILE SCREEN (NEW DESIGN)
// ============================================================================

@Composable
fun ProfileScreenNew(
    name: String = "First Last",
    username: String? = null,
    wristbandName: String = "FASTER Wristband",
    batteryPercentage: Int = 82,
    connectionStatus: String = "Strong Connection",
    onPersonalInfoClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    onHealthClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onPaymentsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onFaqClick: () -> Unit = {},
    onManageAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Section 1: Profile Card
        item {
            ProfileCardSection(
                name = name,
                username = username,
                onPersonalInfoClick = onPersonalInfoClick,
                onEmergencyContactsClick = onEmergencyContactsClick
            )
        }

        // Section 2: Device Card
        item {
            DeviceCardSection(
                wristbandName = wristbandName,
                batteryPercentage = batteryPercentage,
                connectionStatus = connectionStatus
            )
        }

        // Section 3: My Settings
        item {
            MySettingsSection(
                onHealthClick = onHealthClick,
                onNotificationsClick = onNotificationsClick,
                onLocationClick = onLocationClick,
                onPaymentsClick = onPaymentsClick
            )
        }

        // Section 4: Support
        item {
            SupportSection(
                onAboutClick = onAboutClick,
                onReportClick = onReportClick,
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick,
                onFaqClick = onFaqClick
            )
        }

        // Section 5: Bottom Actions
        item {
            BottomActionsSection(
                onManageAccountClick = onManageAccountClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

// ============================================================================
// ENHANCED PROFILE SCREEN WITH FULL NAVIGATION & LOGOUT CONFIRMATION
// ============================================================================

/**
 * Enhanced ProfileScreen wrapper with logout confirmation dialog
 * and full navigation support for all menu items
 */
@Composable
fun EnhancedProfileScreenWithNavigation(
    accessToken: String,
    fullName: String = "First Last",
    username: String? = null,
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReportIssue: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToFAQ: () -> Unit,
    onNavigateToManageAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onNavigateToLogin()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main Profile Screen
    ProfileScreenNew(
        name = fullName,
        username = username,
        wristbandName = "FASTER Wristband",
        batteryPercentage = 82,
        connectionStatus = "Strong Connection",
        onPersonalInfoClick = onNavigateToPersonalInfo,
        onEmergencyContactsClick = onNavigateToEmergencyContacts,
        onHealthClick = onNavigateToHealth,
        onNotificationsClick = onNavigateToNotifications,
        onLocationClick = onNavigateToLocation,
        onPaymentsClick = onNavigateToPayments,
        onReportClick = onNavigateToReportIssue,
        onTermsClick = onNavigateToTerms,
        onPrivacyClick = onNavigateToPrivacy,
        onFaqClick = onNavigateToFAQ,
        onManageAccountClick = onNavigateToManageAccount,
        onLogoutClick = { showLogoutConfirm = true },
        modifier = modifier
    )
}

// ============================================================================
// PREVIEW
// ============================================================================

@Preview(showBackground = true, device = "spec:shape=Normal,width=412,height=915,unit=dp,dpi=420")
@Composable
fun PreviewProfileScreen() {
    FastERTheme {
        PreviewProfileScreen()
    }
}
