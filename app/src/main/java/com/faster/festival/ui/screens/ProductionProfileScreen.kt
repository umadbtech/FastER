package com.faster.festival.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faster.festival.data.models.ProfileSummary
import com.faster.festival.ui.viewmodel.ProfileUiState
import com.faster.festival.ui.viewmodel.ProductionProfileViewModel

/**
 * Production-Grade Profile Screen
 * Implements all required states: Loading, Success, Error, Empty
 * NO hardcoded data, NO fake defaults
 */

// ============================================================================
// SHIMMER LOADING COMPONENTS
// ============================================================================

/**
 * Animated shimmer effect for loading state
 */
@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX = transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(shimmerX.value, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX.value + 500f, 0f)
    )
}

@Composable
fun ProfileShimmerPlaceholder(modifier: Modifier = Modifier) {
    val shimmer = shimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile card shimmer
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shimmer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(shimmer, shape = RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .background(shimmer, shape = RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp))
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(shimmer, shape = RoundedCornerShape(4.dp)))
            }
        }

        // Device card shimmer
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Spacer(modifier = Modifier
                .fillMaxSize()
                .background(shimmer))
        }

        // Settings section shimmer
        repeat(3) {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(shimmer, shape = RoundedCornerShape(12.dp)))
        }
    }
}

// ============================================================================
// ERROR STATE COMPONENTS
// ============================================================================

@Composable
fun ProfileErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Icon
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            // Error Title
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Error Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Retry Button
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text("Retry")
            }
        }
    }
}

// ============================================================================
// EMPTY STATE COMPONENTS
// ============================================================================

@Composable
fun ProfileEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonOff,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "No profile data found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = "Complete your profile to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ============================================================================
// SUCCESS STATE - PROFILE CARD SECTION
// ============================================================================

@Composable
fun ProfileCardSection(
    profile: ProfileSummary,
    modifier: Modifier = Modifier,
    onPersonalInfoClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {}
) {
    val fullName = profile.let { p ->
        when {
            p.legalFirstName != null && p.legalLastName != null ->
                "${p.legalFirstName} ${p.legalLastName}"
            p.legalFirstName != null -> p.legalFirstName
            p.username != null -> p.username
            else -> "User"
        }
    }

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
                        text = fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    profile.username?.let {
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
                        modifier = Modifier.padding(top = if (profile.username != null) 4.dp else 0.dp)
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

            // Row 2: Emergency Contacts
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
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency Contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${profile.emergencyContactsCount} contact${if (profile.emergencyContactsCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
// SUCCESS STATE - ADDITIONAL INFO SECTION (Simplified - No Device Data in API)
// ============================================================================

@Composable
fun AdditionalInfoSection(
    profile: ProfileSummary,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Avatar if available
            profile.avatarUrl?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile Avatar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "From API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Festival info if available
            profile.festivalId?.let {
                if (profile.avatarUrl != null) {
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Festival",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUCCESS STATE - MAIN PROFILE SCREEN
// ============================================================================

@Composable
fun ProfileScreenSuccess(
    profile: ProfileSummary,
    onPersonalInfoClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State management for logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            ProfileCardSection(
                profile = profile,
                onPersonalInfoClick = onPersonalInfoClick,
                onEmergencyContactsClick = onEmergencyContactsClick
            )
        }

        item {
            AdditionalInfoSection(profile = profile)
        }

        // Logout Button
        item {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text("Sign Out")
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Confirm Sign Out",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out? You will need to log in again to access your profile.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}


// ============================================================================
// MAIN PROFILE SCREEN - STATE ROUTER
// ============================================================================

/**
 * Production-grade Profile Screen
 * Routes through all states: Loading → Shimmer, Error → ErrorState, Empty → EmptyState, Success → ProfileScreenSuccess
 */
@Composable
fun ProfileScreen(
    viewModel: ProductionProfileViewModel,
    onPersonalInfoClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            ProfileUiState.Loading -> {
                ProfileShimmerPlaceholder()
            }

            is ProfileUiState.Success -> {
                ProfileScreenSuccess(
                    profile = state.profile,
                    onPersonalInfoClick = onPersonalInfoClick,
                    onEmergencyContactsClick = onEmergencyContactsClick,
                    onLogoutClick = onLogoutClick
                )
            }

            is ProfileUiState.Error -> {
                ProfileErrorState(
                    message = state.message,
                    onRetry = state.retryAction
                )
            }

            ProfileUiState.Empty -> {
                ProfileEmptyState()
            }
        }
    }
}
