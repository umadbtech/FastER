package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.festival.R
import com.faster.festival.data.models.Poi
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.components.*
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.theme.NavyBlue
import com.faster.festival.ui.theme.NavyBlueDark
import com.faster.festival.ui.viewmodel.MapViewModel
import com.faster.festival.ui.viewmodel.UiState
import androidx.compose.ui.res.stringResource
import com.faster.festival.ui.theme.Grey
import com.faster.festival.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
        onTicketsClick: () -> Unit,
        viewModel: MapViewModel =
                viewModel(
                        factory =
                                object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(
                                            modelClass: Class<T>
                                    ): T {
                                        return MapViewModel(FakeFestivalRepository()) as T
                                    }
                                }
                ),
        modifier: Modifier = Modifier
) {
    val poisState by viewModel.poisState.collectAsState()
    var selectedPoi by remember { mutableStateOf<Poi?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (poisState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(text = stringResource(id = R.string.error_loading_map), modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Success -> {
                val pois = (poisState as UiState.Success).data
                MapScreenContent(
                        pois = pois,
                        selectedPoi = selectedPoi,
                        onPoiClick = { poi -> selectedPoi = poi },
                        onTicketsClick = onTicketsClick
                )
            }
        }
    }

    // POI Detail Bottom Sheet
    if (selectedPoi != null) {
        ModalBottomSheet(
                onDismissRequest = { selectedPoi = null },
                modifier = Modifier.fillMaxHeight(0.5f)
        ) { PoiDetailContent(poi = selectedPoi!!, onDismiss = { selectedPoi = null }) }
    }
}

@Composable
fun MapScreenContent(
        pois: List<Poi>,
        selectedPoi: Poi?,
        onPoiClick: (Poi) -> Unit,
        onTicketsClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar
        MapTopBar(onSettingsClick = {})

        // Map Area
        Box(
                modifier =
                        Modifier.weight(1f)
                                .fillMaxWidth()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                    White.copy(alpha = 0.6f),
                                                                    Grey.copy(
                                                                                alpha = 0.6f
                                                                        )
                                                                )
                                                )
                                )
        ) {
            // POI Markers positioned on map
            pois.forEach { poi ->
                MapMarkerItem(
                        poi = poi,
                        isSelected = selectedPoi?.id == poi.id,
                        onClick = { onPoiClick(poi) },
                        modifier =
                                Modifier.align(Alignment.Center)
                                        .offset(
                                                x = ((pois.indexOf(poi) % 3 - 1) * 60).dp,
                                                y = ((pois.indexOf(poi) / 3 - 1) * 60).dp
                                        )
                )
            }
        }

        // Shortcuts Sheet (draggable)
        DraggableShortcutsSheet(
                shortcuts =
                        listOf(
                                stringResource(id = R.string.stages),
                                stringResource(id = R.string.medical_tents),
                                stringResource(id = R.string.hydration),
                                stringResource(id = R.string.food),
                                stringResource(id = R.string.restrooms),
                                stringResource(id = R.string.info)
                        ),
                onShortcutClick = { shortcut ->
                    // Handle shortcut click
                },
                modifier = Modifier.fillMaxWidth()
        )
    }

    // Floating Tickets Button
    Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 280.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
    ) { TicketsFabPill(onClick = onTicketsClick) }
}

@Composable
fun MapTopBar(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
            modifier =
                    modifier.fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = {}) {
                Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                    text = "Map",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                    modifier =
                            Modifier.background(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            shape =
                                                    androidx.compose.foundation.shape
                                                            .RoundedCornerShape(16.dp)
                                    )
                                    .clickable {}
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                    )
                    Text(
                            text = "Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
fun MapMarkerItem(
        poi: Poi,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    MapMarker(poi = poi, isSelected = isSelected, onClick = onClick, modifier = modifier)
}

@Composable
fun PoiDetailContent(poi: Poi, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(24.dp)) {
        Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = poi.type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                text = poi.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(40.dp)) {
                Text(stringResource(id = R.string.close))
            }

            Button(onClick = {}, modifier = Modifier.weight(1f).height(40.dp)) { Text(stringResource(id = R.string.more_info)) }
        }
    }
}

@Preview
@Composable
fun PreviewMapScreen() {
    FastERTheme {
        MapScreenContent(
                pois =
                        listOf(
                                Poi(
                                        "1",
                                        "Main Stage",
                                        "stage",
                                        description = "Primary performance venue"
                                ),
                                Poi(
                                        "2",
                                        "Camping Area",
                                        "camp",
                                        description = "Campground facilities"
                                )
                        ),
                selectedPoi = null,
                onPoiClick = {},
                onTicketsClick = {}
        )
    }
}
