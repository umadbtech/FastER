package com.faster.festival.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.AppConfig
import com.faster.festival.R
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.components.SponsorsPromotionsSection
import com.faster.festival.ui.viewmodel.HomeUiState
import com.faster.festival.ui.viewmodel.HomeViewModel
import com.faster.festival.ui.viewmodel.MapUiState
import com.faster.festival.ui.viewmodel.MapVenue
import com.faster.festival.ui.viewmodel.NewMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    festivalSlug: String = AppConfig.DEFAULT_FESTIVAL_SLUG,
    onSponsorClick: (String) -> Unit = {},
    onPromotionClick: (String) -> Unit = {},
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val viewModel: NewMapViewModel = viewModel(
        factory = NewMapViewModel.Factory(
            contentMapApi = NetworkModule.contentMapApi,
            festivalSlug = festivalSlug,
            networkMonitor = com.faster.festival.di.ConnectivityModule.networkMonitor
        )
    )

    // Shares the Home bundle API so sponsors/promotions stay in sync with HomeScreen.
    val homeViewModel: HomeViewModel = viewModel(
        key = "MapScreenHomeBundle",
        factory = HomeViewModel.Factory(
            appHomeApi = NetworkModule.appHomeApi,
            festivalSlug = festivalSlug,
            networkMonitor = com.faster.festival.di.ConnectivityModule.networkMonitor
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Festival Map",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is MapUiState.Loading -> {
                    MapShimmerLoading()
                }
                is MapUiState.Offline -> {
                    com.faster.festival.ui.components.network.NoInternetScreen(
                        onRetry = { viewModel.refresh() }
                    )
                }
                is MapUiState.Error -> {
                    MapErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is MapUiState.Success -> {
                    MapSuccessContent(
                        state = state,
                        homeBundleState = homeUiState,
                        onFilterChanged = { viewModel.onFilterChanged(it) },
                        onSponsorClick = onSponsorClick,
                        onPromotionClick = onPromotionClick,
                        onCtaClick = onCtaClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MapShimmerLoading() {
    val transition = rememberInfiniteTransition(label = "map_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "map_shimmer_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
            }
        }
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun MapErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to load map",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun MapSuccessContent(
    state: MapUiState.Success,
    homeBundleState: HomeUiState,
    onFilterChanged: (String) -> Unit,
    onSponsorClick: (String) -> Unit,
    onPromotionClick: (String) -> Unit,
    onCtaClick: (url: String, title: String) -> Unit
) {
    val filters = listOf("All", "Stage", "Area", "Service")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        // Top map header — switches drawable based on selected filter
        item {
            MapHeaderImage(selectedFilter = state.selectedFilter)
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter.equals(filter, ignoreCase = true),
                        onClick = { onFilterChanged(filter) },
                        label = { Text(filter) },
                        leadingIcon = if (state.selectedFilter.equals(filter, ignoreCase = true)) {
                            {
                                Icon(
                                    imageVector = getFilterIcon(filter),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Section header
        item {
            Text(
                text = "Points of Interest (${state.filteredVenues.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Venue list
        if (state.filteredVenues.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No venues found for this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = state.filteredVenues,
                key = { it.id }
            ) { venue ->
                VenueCard(
                    venue = venue,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom section: reusable Sponsors & Promotions carousels shared with Home.
        if (homeBundleState is HomeUiState.Success) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                SponsorsPromotionsSection(
                    bundle = homeBundleState.data,
                    onSponsorClick = onSponsorClick,
                    onPromotionClick = onPromotionClick,
                    onCtaClick = onCtaClick
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun VenueCard(
    venue: MapVenue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFilterIcon(venue.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = venue.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = venue.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (venue.description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = venue.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (venue.nextEventTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Up next: ${venue.nextEventTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getFilterIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "stage", "stages" -> Icons.Default.MusicNote
        "food" -> Icons.Default.Fastfood
        "restrooms", "restroom" -> Icons.Default.Wc
        "medical" -> Icons.Default.LocalHospital
        "service" -> Icons.Default.Info
        "area" -> Icons.Default.Store
        else -> Icons.Default.LocationOn
    }
}

/**
 * Maps the selected filter chip to its corresponding map drawable.
 */
private fun mapDrawableForFilter(filter: String): Int {
    return when (filter.lowercase()) {
        "stage" -> R.drawable.main_stage_map
        "area" -> R.drawable.service_area_map
        "service" -> R.drawable.box_office_map
        else -> R.drawable.all_map
    }
}

/**
 * Top header image that animates between map drawables when the filter changes.
 */
@Composable
private fun MapHeaderImage(selectedFilter: String) {
    val drawableRes = mapDrawableForFilter(selectedFilter)

    AnimatedContent(
        targetState = drawableRes,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "map_header_swap"
    ) { resId ->
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Festival map for $selectedFilter",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
