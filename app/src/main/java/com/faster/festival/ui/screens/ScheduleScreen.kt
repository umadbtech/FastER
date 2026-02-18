package com.faster.festival.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.faster.festival.data.models.ScheduleItem
import com.faster.festival.data.repository.FakeFestivalRepository
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.viewmodel.ScheduleViewModel
import com.faster.festival.ui.viewmodel.UiState
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ScheduleScreen(
    onTicketsClick: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ScheduleViewModel(FakeFestivalRepository()) as T
            }
        }
    ),
    modifier: Modifier = Modifier
) {
    val scheduleState by viewModel.scheduleState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (scheduleState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Text(
                    text = "Error loading schedule",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UiState.Success -> {
                val items = (scheduleState as UiState.Success).data
                ScheduleScreenContent(
                    items = items,
                    onTicketsClick = onTicketsClick
                )
            }
        }
    }
}

@Composable
fun ScheduleScreenContent(
    items: List<ScheduleItem>,
    onTicketsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )

        // Schedule List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(items) { item ->
                ScheduleItemCard(item = item)
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    item: ScheduleItem,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.startTime.split(" ")[0],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Event details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.artistName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${item.stageName} • ${item.startTime} - ${item.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Action icon
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewScheduleScreen() {
    FastERTheme {
        ScheduleScreenContent(
            items = listOf(
                ScheduleItem("1", "Main Stage", "Luna Echo", "8:00 PM", "9:15 PM", "May 15"),
                ScheduleItem("2", "Main Stage", "The Midnight Collective", "10:00 PM", "11:30 PM", "May 15"),
                ScheduleItem("3", "Mountain Stage", "Harmony Waves", "2:00 PM", "3:15 PM", "May 16")
            ),
            onTicketsClick = {}
        )
    }
}
