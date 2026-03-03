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
import com.faster.festival.data.models.Artist
import com.faster.festival.data.models.Festival
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.components.*
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.viewmodel.HomeViewModel
import com.faster.festival.ui.viewmodel.UiState
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onArtistClick: (String) -> Unit,
    onTicketsClick: () -> Unit,
    onFestivalHomeClick: () -> Unit,
    onFaqsClick: () -> Unit,
    accessToken: String? = null,
    festivalSlug: String = "floydfest-26",
    viewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                // Create API-compatible Festival with all required fields
                val apiFestival = Festival(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    slug = "floydfest-26",
                    name = "FloydFest 26",
                    timezone = "America/New_York",
                    startsAt = "2026-07-22T16:00:00+00:00",
                    endsAt = "2026-07-27T03:00:00+00:00",
                    logoUrl = "https://example.com/logo.png",
                    bannerUrl = "https://example.com/banner.jpg",
                    accentColorHex = "#00A86B",
                    contextState = "PRE"
                )
                val repository = FakeFestivalRepository(festival = apiFestival)
                return HomeViewModel(repository) as T
            }
        }
    )
) {
    val festivalState by viewModel.festivalState.collectAsState()
    val artistsState by viewModel.artistsState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (festivalState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Error -> {
                Text(
                    text = "Error loading festival",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val artists = when (artistsState) {
                    is UiState.Success -> (artistsState as UiState.Success).data
                    else -> emptyList()
                }

                HomeScreenContent(
                    artists = artists,
                    onArtistClick = onArtistClick,
                    onTicketsClick = onTicketsClick,
                    onFestivalHomeClick = onFestivalHomeClick,
                    onFaqsClick = onFaqsClick,
                    onSettingsClick = {},
                    festivalSlug = festivalSlug,
                    accessToken = accessToken
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    artists: List<Artist>,
    onArtistClick: (String) -> Unit,
    onTicketsClick: () -> Unit,
    onFestivalHomeClick: () -> Unit,
    onFaqsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    festivalSlug: String = "floydfest-26",
    accessToken: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Festival Header Screen with API integration
        FestivalHeaderScreen(
            modifier = Modifier,
            festivalSlug = festivalSlug,
            accessToken = accessToken
        )

        // Quick Actions
        QuickActionRow(
            actions = listOf(
                "Schedule" to Icons.Default.DateRange,
                "Lineup" to Icons.Default.Group,
                "Parking" to Icons.Default.DirectionsCar,
                "Wristband" to Icons.Default.CardGiftcard
            ),
            onActionClick = { action ->
                when (action) {
                    "Schedule" -> onFestivalHomeClick()
                    else -> {}
                }
            }
        )

        // Setup Account Card
        SetupAccountCard(
            onSetupClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Headliners
        if (artists.isNotEmpty()) {
            HeadlinerRowOrGrid(
                artists = artists.take(4),
                onArtistClick = onArtistClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Experience List
        ExperienceList(
            onTicketsClick = onTicketsClick,
            onFestivalHomeClick = onFestivalHomeClick,
            onFaqsClick = onFaqsClick
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview
@Composable
fun PreviewHomeScreen() {
    FastERTheme {
        HomeScreenContent(
            artists = listOf(
                Artist(id = "1", name = "Luna Echo"),
                Artist(id = "2", name = "The Midnight Collective"),
                Artist(id = "3", name = "Harmony Waves"),
                Artist(id = "4", name = "Desert Bloom")
            ),
            onArtistClick = {},
            onTicketsClick = {},
            onFestivalHomeClick = {},
            onFaqsClick = {},
            onSettingsClick = {},
            festivalSlug = "floydfest-26",
            accessToken = null
        )
    }
}
