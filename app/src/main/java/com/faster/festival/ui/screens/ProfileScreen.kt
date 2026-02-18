package com.faster.festival.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.models.AccountProfile
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.components.ProfileCardSection
import com.faster.festival.ui.components.ProfileHeader
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.viewmodel.ProfileViewModel
import com.faster.festival.ui.viewmodel.UiState
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProfileScreen(
    onTicketsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(FakeFestivalRepository()) as T
            }
        }
    ),
    modifier: Modifier = Modifier
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
    onTicketsClick: () -> Unit,
    onUpdateProfile: (AccountProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        ProfileHeader(
            name = profile.name.ifEmpty { "Your Name" },
            email = profile.email.ifEmpty { "your@email.com" },
            onEditClick = {},
            onSettingsClick = {},
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Info Section
        ProfileCardSection(
            title = "Contact Info",
            items = listOf(
                "Phone" to (profile.phone.ifEmpty { "Add phone" }),
                "Email" to (profile.email.ifEmpty { "Add email" })
            ),
            onActionClick = {},
            actionLabel = "Edit"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Contact Section
        ProfileCardSection(
            title = "Emergency",
            items = listOf(
                "Contact" to (profile.emergencyContact.ifEmpty { "Add contact" })
            ),
            onActionClick = {},
            actionLabel = "Add"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Health Section
        ProfileCardSection(
            title = "Health",
            items = listOf(
                "Allergies" to (profile.allergies.ifEmpty { "None" }),
                "Medications" to (profile.medications.ifEmpty { "None" })
            ),
            onActionClick = {},
            actionLabel = "Edit"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Payments Section
        ProfileCardSection(
            title = "Payments",
            items = listOf(
                "Card" to "****4242"
            ),
            onActionClick = {},
            actionLabel = "Add"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Details Section
        ProfileCardSection(
            title = "Personal Details",
            items = listOf(
                "Progress" to "3/3"
            ),
            onActionClick = {},
            actionLabel = "View"
        )

        Spacer(modifier = Modifier.height(80.dp))
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
