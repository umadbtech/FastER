package com.faster.festival.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.AppConfig
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.LineupArtist
import com.faster.festival.ui.viewmodel.LineupUiState
import com.faster.festival.ui.viewmodel.LineupViewModel

// Light theme palette (consistent with HomeScreen)
private val LineupBg = Color(0xFFF7F7F7)
private val LineupWhite = Color.White
private val LineupCoralRed = Color(0xFFE53935)
private val LineupDarkNavy = Color(0xFF0D1B2A)
private val LineupTextDark = Color(0xFF222222)
private val LineupTextMedium = Color(0xFF333333)
private val LineupTextLight = Color(0xFF666666)
private val LineupBorderLight = Color(0xFFE0E0E0)
private val LineupCardBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupScreen(
    onArtistClick: (String) -> Unit = {},
    festivalSlug: String = AppConfig.DEFAULT_FESTIVAL_SLUG
) {
    val viewModel: LineupViewModel = viewModel(
        factory = LineupViewModel.Factory(
            contentLineupApi = NetworkModule.contentLineupApi,
            contentStageScheduleApi = NetworkModule.contentStageScheduleApi,
            festivalSlug = festivalSlug
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { LineupTopBar() },
        containerColor = LineupBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is LineupUiState.Loading -> {
                    LineupShimmerLoading()
                }
                is LineupUiState.Error -> {
                    LineupErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is LineupUiState.Success -> {
                    LineupSuccessContent(
                        state = state,
                        onArtistClick = onArtistClick,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onDayFilterChanged = { viewModel.onDayFilterChanged(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineupTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Artist Lineup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LineupTextDark
                )
                Text(
                    text = "Discover the performers",
                    style = MaterialTheme.typography.labelSmall,
                    color = LineupTextLight,
                    letterSpacing = 0.5.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LineupBg
        )
    )
}

@Composable
private fun LineupShimmerLoading() {
    val transition = rememberInfiniteTransition(label = "lineup_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lineup_shimmer_alpha"
    )

    val shimmerColor = Color.Gray.copy(alpha = alpha)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar shimmer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerColor)
            )
        }
        // Filter chips shimmer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
        // Featured artist shimmer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
        // Artist list shimmer
        items(5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerColor)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LineupErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Error",
                modifier = Modifier.size(40.dp),
                tint = LineupCoralRed
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Failed to load lineup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LineupTextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = LineupTextLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = LineupCoralRed
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LineupSuccessContent(
    state: LineupUiState.Success,
    onArtistClick: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onDayFilterChanged: (Int?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Search bar
        item {
            LineupSearchBar(
                query = state.searchQuery,
                onQueryChanged = onSearchQueryChanged
            )
        }

        // Day filter chips
        item {
            LineupDayFilters(
                days = state.days,
                selectedDay = state.selectedDay,
                onDaySelected = onDayFilterChanged
            )
        }

        if (state.filteredArtists.isEmpty()) {
            item {
                LineupEmptyState(
                    message = if (state.searchQuery.isNotBlank()) {
                        "No artists found for \"${state.searchQuery}\""
                    } else {
                        "No artists scheduled for this day"
                    }
                )
            }
        } else {
            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.searchQuery.isNotBlank()) "Results" else "All Artists",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LineupTextDark
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${state.filteredArtists.size} artists",
                        style = MaterialTheme.typography.labelSmall,
                        color = LineupTextLight
                    )
                }
            }

            // 2×2 Artist Grid
            val chunkedArtists = state.filteredArtists.chunked(2)
            items(
                count = chunkedArtists.size,
                key = { index -> chunkedArtists[index].first().id }
            ) { index ->
                val rowItems = chunkedArtists[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { artist ->
                        ArtistGridCard(
                            artist = artist,
                            onClick = { onArtistClick(artist.slug ?: artist.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LineupSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = "Search artists, genres...",
                color = LineupTextLight
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = LineupTextLight
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = LineupTextLight
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = LineupTextDark,
            unfocusedTextColor = LineupTextDark,
            cursorColor = LineupCoralRed,
            focusedBorderColor = LineupBorderLight,
            unfocusedBorderColor = LineupBorderLight,
            focusedContainerColor = LineupWhite,
            unfocusedContainerColor = LineupWhite
        ),
        singleLine = true
    )
}

@Composable
private fun LineupDayFilters(
    days: List<Int>,
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedDay == null,
            onClick = { onDaySelected(null) },
            label = {
                Text(
                    text = "All Days",
                    fontWeight = if (selectedDay == null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LineupCoralRed,
                selectedLabelColor = Color.White,
                containerColor = LineupWhite,
                labelColor = LineupTextMedium
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = LineupBorderLight,
                selectedBorderColor = Color.Transparent,
                enabled = true,
                selected = selectedDay == null
            )
        )
        days.forEach { day ->
            FilterChip(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                label = {
                    Text(
                        text = "Day $day",
                        fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LineupCoralRed,
                    selectedLabelColor = Color.White,
                    containerColor = LineupWhite,
                    labelColor = LineupTextMedium
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedDay == day
                )
            )
        }
    }
}

@Composable
private fun ArtistGridCard(
    artist: LineupArtist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Artist image
            if (artist.imageUrl != null) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A0033),
                                    LineupDarkNavy
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = LineupTextLight,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Full-card dark gradient overlay with blur effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.15f),
                                0.3f to Color.Black.copy(alpha = 0.25f),
                                0.55f to Color.Black.copy(alpha = 0.45f),
                                0.75f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            // Radial vignette for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            radius = 500f
                        )
                    )
            )

            // Text content — bottom-aligned
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                if (artist.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist.genres.take(2).joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (artist.setTime != null || artist.stageName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            artist.setTime?.let { append(it) }
                            artist.stageName?.let {
                                if (isNotEmpty()) append(" @ ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Day badge — top-right
            if (artist.day != null) {
                Text(
                    text = "Day ${artist.day}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun LineupEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(LineupBorderLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = LineupTextLight
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = LineupTextMedium,
            textAlign = TextAlign.Center
        )
    }
}
