package com.faster.festival.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.R
import com.faster.festival.data.models.AccountProfile
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.theme.NavyBlue
import com.faster.festival.ui.theme.SuccessGreen
import com.faster.festival.ui.viewmodel.ProfileViewModel
import com.faster.festival.ui.viewmodel.UiState

// ============================================================================
// REUSABLE COMPOSABLES
// ============================================================================

/**
 * Badge/Chip component for displaying user status badges like "Carrying Narcan", "Minor"
 */
@Composable
fun BadgeChip(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NavyBlue,
    textColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * User handle pill component (e.g., "#xyz123")
 */
@Composable
fun UserHandlePill(
    handle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavyBlue,
        modifier = modifier
    ) {
        Text(
            text = handle,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Top app bar with logo, user handle pill, and action buttons
 */
@Composable
fun AccountTopAppBar(
    userHandle: String,
    onSearch: () -> Unit,
    onSend: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.faster_logo),
            contentDescription = "Festival Logo",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        // User handle pill
        UserHandlePill(handle = userHandle)

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NavyBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onSend, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = NavyBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color(0xFFFFA500),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onProfileClick)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Green header bar with back button and title
 */
@Composable
fun AccountHeaderBar(
    title: String = "Account",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SuccessGreen)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Profile card with badges, avatar, name, and contact info
 */
@Composable
fun ProfileInfoCard(
    badges: List<String>,
    name: String,
    subtitle: String,
    phone: String,
    email: String,
    emergencyContact: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Badge row with edit button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    badges.forEach { badge ->
                        BadgeChip(label = badge)
                    }
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = NavyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // Avatar and name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = NavyBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // Contact info rows
            ContactInfoRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = phone,
                showDivider = true
            )

            ContactInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = email,
                showDivider = true
            )

            ContactInfoRow(
                icon = Icons.Default.Warning,
                label = "Emergency Contact",
                value = emergencyContact,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Single contact info row with icon, label, and value
 */
@Composable
fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NavyBlue,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
        }
    }
}

/**
 * Wristband status card (dark navy background)
 */
@Composable
fun WristbandStatusCard(
    wristbandName: String,
    userHandle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wristband icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF6B6B),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Wristband",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Wristband name
            Text(
                text = wristbandName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // User handle pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier
            ) {
                Text(
                    text = userHandle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Settings row component for quick action list
 */
@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NavyBlue,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
        }
    }
}

/**
 * Quick settings card with multiple action rows
 */
@Composable
fun QuickSettingsCard(
    onNotificationsClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onHealthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsRow(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                onClick = onNotificationsClick,
                showDivider = true
            )

            SettingsRow(
                icon = Icons.Default.Group,
                label = "Friends",
                onClick = onFriendsClick,
                showDivider = true
            )

            SettingsRow(
                icon = Icons.Default.Payment,
                label = "Payments",
                onClick = onPaymentsClick,
                showDivider = true
            )

            SettingsRow(
                icon = Icons.Default.LocalHospital,
                label = "Health",
                onClick = onHealthClick,
                showDivider = false
            )
        }
    }
}

/**
 * Promo banner with icon, title, and subtitle
 */
@Composable
fun PromoBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA500)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spotify icon placeholder
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1DB954),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connect Music Streaming",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "For Exclusive Access to All Artist Setlists",
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// MAIN ACCOUNT SCREEN
// ============================================================================

/**
 * Main Account/Profile screen with all sections
 *
 * Usage:
 * ```
 * AccountScreen(
 *     userHandle = "#xyz123",
 *     badges = listOf("Carrying Narcan", "Minor"),
 *     name = "First Last",
 *     subtitle = "Age | Demographics",
 *     phone = "123-456-7890",
 *     email = "first@website.com",
 *     emergencyContact = "123-456-7890",
 *     wristbandName = "FASTER Wristband",
 *     onBack = { navController.popBackStack() },
 *     onEdit = { /* Handle edit */ },
 *     onSearch = { /* Handle search */ },
 *     onSend = { /* Handle send */ },
 *     onProfileClick = { /* Handle profile click */ },
 *     onNotifications = { /* Navigate to notifications */ },
 *     onFriends = { /* Navigate to friends */ },
 *     onPayments = { /* Navigate to payments */ },
 *     onHealth = { /* Navigate to health */ },
 *     onPromoClick = { /* Handle promo */ },
 *     onManageAccount = { /* Navigate to manage account */ }
 * )
 * ```
 */
@Composable
fun AccountScreen(
    userHandle: String,
    badges: List<String> = emptyList(),
    name: String,
    subtitle: String,
    phone: String,
    email: String,
    emergencyContact: String,
    wristbandName: String = "FASTER Wristband",
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSearch: () -> Unit,
    onSend: () -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    onFriends: () -> Unit,
    onPayments: () -> Unit,
    onHealth: () -> Unit,
    onPromoClick: () -> Unit,
    onManageAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AccountTopAppBar(
            userHandle = userHandle,
            onSearch = onSearch,
            onSend = onSend,
            onProfileClick = onProfileClick
        )

        AccountHeaderBar(
            title = "Account",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                badges = badges,
                name = name,
                subtitle = subtitle,
                phone = phone,
                email = email,
                emergencyContact = emergencyContact,
                onEditClick = onEdit
            )

            Spacer(modifier = Modifier.height(20.dp))

            WristbandStatusCard(
                wristbandName = wristbandName,
                userHandle = userHandle
            )

            Spacer(modifier = Modifier.height(20.dp))

            QuickSettingsCard(
                onNotificationsClick = onNotifications,
                onFriendsClick = onFriends,
                onPaymentsClick = onPayments,
                onHealthClick = onHealth
            )

            Spacer(modifier = Modifier.height(20.dp))

            PromoBanner(onClick = onPromoClick)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onManageAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Manage Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ============================================================================
// LEGACY WRAPPERS (backward compatibility)
// ============================================================================

@Composable
fun ProfileScreen(
    onTicketsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(FakeFestivalRepository()) as T
            }
        }
    )
) {
    val profileState by viewModel.profileState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (profileState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(
                    text = "Error loading profile",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val profile = (profileState as UiState.Success).data
                ProfileScreenContent(
                    profile = profile,
                    onTicketsClick = onTicketsClick,
                    onUpdateProfile = { viewModel.updateProfile(it) }
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    profile: AccountProfile,
    @Suppress("UNUSED_PARAMETER")
    onTicketsClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onUpdateProfile: (AccountProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    AccountScreen(
        userHandle = "#xyz123",
        badges = listOf("Carrying Narcan", "Minor"),
        name = profile.name.ifEmpty { "First Last" },
        subtitle = "Age | Demographics",
        phone = profile.phone.ifEmpty { "123-456-7890" },
        email = profile.email.ifEmpty { "first@website.com" },
        emergencyContact = profile.emergencyContact.ifEmpty { "123-456-7890" },
        wristbandName = "FASTER Wristband",
        onBack = { /* TODO */ },
        onEdit = { /* TODO */ },
        onSearch = { /* TODO */ },
        onSend = { /* TODO */ },
        onProfileClick = { /* TODO */ },
        onNotifications = { /* TODO */ },
        onFriends = { /* TODO */ },
        onPayments = { /* TODO */ },
        onHealth = { /* TODO */ },
        onPromoClick = { /* TODO */ },
        onManageAccount = { /* TODO */ },
        modifier = modifier
    )
}

// ============================================================================
// PREVIEW
// ============================================================================

@Preview(showBackground = true, device = "spec:shape=Normal,width=412,height=915,unit=dp,dpi=420")
@Composable
fun PreviewAccountScreen() {
    FastERTheme {
        AccountScreen(
            userHandle = "#xyz123",
            badges = listOf("Carrying Narcan", "Minor"),
            name = "First Last",
            subtitle = "Age | Demographics",
            phone = "123-456-7890",
            email = "first@website.com",
            emergencyContact = "123-456-7890",
            wristbandName = "FASTER Wristband",
            onBack = {},
            onEdit = {},
            onSearch = {},
            onSend = {},
            onProfileClick = {},
            onNotifications = {},
            onFriends = {},
            onPayments = {},
            onHealth = {},
            onPromoClick = {},
            onManageAccount = {}
        )
    }
}

@Preview
@Composable
fun PreviewProfileScreen() {
    FastERTheme {
        ProfileScreenContent(
            profile = AccountProfile(
                id = "1",
                name = "Alex Johnson",
                email = "alex@example.com",
                phone = "+1 (555) 123-4567",
                emergencyContact = "+1 (555) 987-6543",
                allergies = "Peanuts",
                medications = "None"
            ),
            onTicketsClick = {},
            onUpdateProfile = {}
        )
    }
}
