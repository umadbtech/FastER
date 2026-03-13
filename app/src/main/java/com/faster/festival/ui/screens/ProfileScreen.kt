package com.faster.festival.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.faster.festival.ui.theme.FastERTheme
import kotlinx.coroutines.delay

// ============================================================================
// SECTION 1: PROFILE CARD
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD6D6D6)
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
// SECTION 1.5: AVATAR DISPLAY (WITH SIGNED URL)
// ============================================================================

@Composable
fun AvatarSection(
    avatarUrl: String?,
    displayName: String = "User",
    onUploadAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar Circle with fallback
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                val context = LocalContext.current

                // ✅ Display avatar with cache DISABLED (signed URLs expire in 60s)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.DISABLED)    // ✅ Don't cache to disk
                        .memoryCachePolicy(CachePolicy.DISABLED)  // ✅ Don't cache in memory
                        .build(),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                    error = painterResource(android.R.drawable.ic_menu_report_image),
                    onError = { error ->
                        Log.e("AvatarSection", "Failed to load avatar: ${error.result.throwable}")
                    },
                    onSuccess = {
                        Log.d("AvatarSection", "✅ Avatar loaded from: $avatarUrl")
                    }
                )
            } else {
                // Fallback: Show icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Placeholder",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Display Name
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Upload Avatar Button
        OutlinedButton(
            onClick = onUploadAvatarClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Update Avatar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ============================================================================
// SECTION 1.8: EMERGENCY CONTACTS LIST
// ============================================================================

data class EmergencyContactInfo(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String,
    val isPrimary: Boolean = false
)

@Composable
fun EmergencyContactCard(
    contact: EmergencyContactInfo,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with name and primary badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (contact.isPrimary) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Primary", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Phone number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactsSection(
    contacts: List<EmergencyContactInfo> = emptyList(),
    onAddContactClick: () -> Unit = {},
    onEditContact: (EmergencyContactInfo) -> Unit = {},
    onDeleteContact: (EmergencyContactInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = onAddContactClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Contacts list
        if (contacts.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No emergency contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add at least one emergency contact",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            contacts.forEach { contact ->
                EmergencyContactCard(
                    contact = contact,
                    onEditClick = { onEditContact(contact) },
                    onDeleteClick = { onDeleteContact(contact) }
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
    avatarUrl: String? = null,
    emergencyContacts: List<EmergencyContactInfo> = emptyList(),
    wristbandName: String = "FASTER Wristband",
    batteryPercentage: Int = 82,
    connectionStatus: String = "Strong Connection",
    onPersonalInfoClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    onUploadAvatarClick: () -> Unit = {},
    onAddEmergencyContactClick: () -> Unit = {},
    onEditEmergencyContact: (EmergencyContactInfo) -> Unit = {},
    onDeleteEmergencyContact: (EmergencyContactInfo) -> Unit = {},
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
        // Section 0: Avatar
        item {
            AvatarSection(
                avatarUrl = avatarUrl,
                displayName = name,
                onUploadAvatarClick = onUploadAvatarClick
            )
        }

        // Section 1: Profile Card
        item {
            ProfileCardSection(
                name = name,
                username = username,
                onPersonalInfoClick = onPersonalInfoClick,
                onEmergencyContactsClick = onEmergencyContactsClick
            )
        }

        item {
            EmergencyContactsSection(
                contacts = emergencyContacts,
                onAddContactClick = onAddEmergencyContactClick,
                onEditContact = onEditEmergencyContact,
                onDeleteContact = onDeleteEmergencyContact
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
    avatarUrl: String? = null,
    emergencyContacts: List<EmergencyContactInfo> = emptyList(),
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToUploadAvatar: () -> Unit = {},
    onNavigateToAddContact: () -> Unit = {},
    onNavigateToEditContact: (EmergencyContactInfo) -> Unit = {},
    onNavigateToDeleteContact: (EmergencyContactInfo) -> Unit = {},
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
    onRefreshProfile: () -> Unit = {},  // ✅ Called to refresh profile on screen appear
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // ✅ Refresh profile when screen appears (signed URL expires in 60s)
    // ✅ Also auto-refresh every 50s to keep URL fresh before expiry
    LaunchedEffect(Unit) {
        while (true) {
            onRefreshProfile()
            delay(50_000L)  // Refresh every 50 seconds (before 60s expiry)
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout? You will need to login again.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout", color = Color.White)
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
        avatarUrl = avatarUrl,
        emergencyContacts = emergencyContacts,
        wristbandName = "FASTER Wristband",
        batteryPercentage = 82,
        connectionStatus = "Strong Connection",
        onPersonalInfoClick = onNavigateToPersonalInfo,
        onEmergencyContactsClick = onNavigateToEmergencyContacts,
        onUploadAvatarClick = onNavigateToUploadAvatar,
        onAddEmergencyContactClick = onNavigateToAddContact,
        onEditEmergencyContact = onNavigateToEditContact,
        onDeleteEmergencyContact = onNavigateToDeleteContact,
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
