package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.data.models.Artist
import com.faster.festival.data.models.FestivalSet
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.theme.*
import com.faster.festival.ui.viewmodel.ArtistDetailViewModel
import com.faster.festival.ui.viewmodel.UiState

@Composable
fun ArtistDetailScreen(
        artistId: String,
        onBackClick: () -> Unit,
        viewModel: ArtistDetailViewModel =
                viewModel(
                        factory =
                                object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(
                                            modelClass: Class<T>
                                    ): T {
                                        return ArtistDetailViewModel(FakeFestivalRepository()) as T
                                    }
                                }
                ),
        modifier: Modifier = Modifier
) {
    LaunchedEffect(artistId) { viewModel.loadArtist(artistId) }

    val artistState by viewModel.artistState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (artistState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(text = "Error loading artist", modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Success -> {
                val artist = (artistState as UiState.Success).data
                if (artist != null) {
                    ArtistDetailContent(artist = artist, onBackClick = onBackClick)
                }
            }
        }
    }
}

@Composable
fun ArtistDetailContent(artist: Artist, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero Image Box
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(300.dp)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors = listOf(NavyBlue, NavyBlueDark)
                                                )
                                )
        ) {
            // Artist icon placeholder
            Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = artist.name,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.Center).size(100.dp)
            )

            // Back button
            IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }

        // Artist Info
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                    text = artist.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "Electronic • Live Performance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text =
                            artist.bio.ifEmpty {
                                "Discover this amazing artist and their unique sound. Join us for an unforgettable performance at the festival."
                            },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Festival Sets
            if (artist.sets.isNotEmpty()) {
                Text(
                        text = "Festival Sets",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                artist.sets.forEach { set ->
                    FestivalSetCard(set = set)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                    Text("Save")
                }

                Button(onClick = {}, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                    Text("Get Tickets")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FestivalSetCard(set: FestivalSet, modifier: Modifier = Modifier) {
    ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                    text = set.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                        text = set.stageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                )

                Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                        text = "${set.startTime} - ${set.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewArtistDetailScreen() {
    FastERTheme {
        ArtistDetailContent(
                artist =
                        Artist(
                                id = "1",
                                name = "Luna Echo",
                                bio =
                                        "Luna Echo is a mesmerizing electronic artist known for immersive live performances. Their signature sound blends ambient textures with driving beats, creating an unforgettable sonic journey.",
                                sets =
                                        listOf(
                                                FestivalSet(
                                                        "1",
                                                        "Opening Night",
                                                        "Main Stage",
                                                        "8:00 PM",
                                                        "9:15 PM"
                                                ),
                                                FestivalSet(
                                                        "2",
                                                        "Sunset Session",
                                                        "Campground Stage",
                                                        "4:30 PM",
                                                        "5:45 PM"
                                                )
                                        )
                        ),
                onBackClick = {}
        )
    }
}
