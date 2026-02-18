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
    onArtistClick: (String) -> Unit,
    onTicketsClick: () -> Unit,
    onFestivalHomeClick: () -> Unit,
    onFaqsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(FakeFestivalRepository()) as T
            }
        }
    ),
    modifier: Modifier = Modifier
) {
    val festivalState by viewModel.festivalState.collectAsState()
    val artistsState by viewModel.artistsState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Main content
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
                val festival = (festivalState as UiState.Success).data
                val artists = when (artistsState) {
                    is UiState.Success -> (artistsState as UiState.Success).data
                    else -> emptyList()
                }

                HomeScreenContent(
                    festival = festival,
                    artists = artists,
                    onArtistClick = onArtistClick,
                    onTicketsClick = onTicketsClick,
                    onFestivalHomeClick = onFestivalHomeClick,
                    onFaqsClick = onFaqsClick,
                    onSettingsClick = {}
                )
            }
        }

        // Floating Tickets Button
        TicketsFabPill(
            onClick = onTicketsClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
fun HomeScreenContent(
    festival: Festival,
    artists: List<Artist>,
    onArtistClick: (String) -> Unit,
    onTicketsClick: () -> Unit,
    onFestivalHomeClick: () -> Unit,
    onFaqsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Header
        FestivalHeroHeader(
            festivalName = festival.name,
            location = festival.location,
            date = festival.date
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
            festival = Festival(
                id = "1",
                name = "FASTER",
                location = "Desert Valley, California",
                date = "May 15-17, 2026"
            ),
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
            onSettingsClick = {}
        )
    }
}
