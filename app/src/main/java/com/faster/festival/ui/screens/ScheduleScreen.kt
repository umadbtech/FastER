package com.faster.festival.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.AppConfig
import com.faster.festival.data.models.ContentScheduleEvent
import com.faster.festival.data.models.ContentStageSchedule
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.StageScheduleViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Light theme palette (consistent with other screens)
private val ScheduleBg = Color(0xFFF7F7F7)
private val ScheduleWhite = Color.White
private val ScheduleCoralRed = Color(0xFFE53935)
private val ScheduleTextDark = Color(0xFF222222)
private val ScheduleTextMedium = Color(0xFF333333)
private val ScheduleTextLight = Color(0xFF666666)
private val ScheduleBorderLight = Color(0xFFE0E0E0)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBackClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    festivalSlug: String = AppConfig.DEFAULT_FESTIVAL_SLUG
) {
    val viewModel: StageScheduleViewModel = viewModel(
        factory = StageScheduleViewModel.Factory(
            contentStageScheduleApi = NetworkModule.contentStageScheduleApi,
            festivalSlug = festivalSlug
        )
    )
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScheduleBg)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Stage Schedule",
                    fontWeight = FontWeight.Bold,
                    color = ScheduleTextDark
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ScheduleTextDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ScheduleWhite
            )
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ScheduleCoralRed)
                }
            }
            state.error != null -> {
                ScheduleErrorContent(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.retry() }
                )
            }
            else -> {
                ScheduleContent(
                    stages = state.stages,
                    events = state.filteredEvents,
                    days = state.days,
                    selectedStageId = state.selectedStageId,
                    selectedDay = state.selectedDay,
                    onStageSelected = { viewModel.selectStage(it) },
                    onDaySelected = { viewModel.selectDay(it) },
                    onArtistClick = onArtistClick
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ScheduleContent(
    stages: List<ContentStageSchedule>,
    events: List<ContentScheduleEvent>,
    days: List<Int>,
    selectedStageId: String?,
    selectedDay: Int?,
    onStageSelected: (String?) -> Unit,
    onDaySelected: (Int?) -> Unit,
    onArtistClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Day filter tabs
        if (days.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ScheduleFilterChip(
                            label = "All Days",
                            isSelected = selectedDay == null,
                            onClick = { onDaySelected(null) }
                        )
                    }
                    items(days) { day ->
                        ScheduleFilterChip(
                            label = "Day $day",
                            isSelected = selectedDay == day,
                            onClick = { onDaySelected(day) }
                        )
                    }
                }
            }
        }

        // Stage filter chips
        if (stages.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        StageChip(
                            label = "All Stages",
                            isSelected = selectedStageId == null,
                            onClick = { onStageSelected(null) }
                        )
                    }
                    items(stages) { stage ->
                        StageChip(
                            label = stage.name,
                            isSelected = selectedStageId == stage.id,
                            onClick = { onStageSelected(stage.id) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Event count
        item {
            Text(
                text = "${events.size} ${if (events.size == 1) "performance" else "performances"}",
                style = MaterialTheme.typography.bodySmall,
                color = ScheduleTextLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Events list
        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No performances found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ScheduleTextLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                ScheduleEventCard(
                    event = event,
                    onArtistClick = { onArtistClick(event.artistId) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier.background(ScheduleCoralRed)
                } else {
                    Modifier
                        .background(ScheduleWhite)
                        .border(1.dp, ScheduleBorderLight, RoundedCornerShape(20.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else ScheduleTextMedium
        )
    }
}

@Composable
private fun StageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.background(ScheduleTextDark)
                } else {
                    Modifier
                        .background(ScheduleWhite)
                        .border(1.dp, ScheduleBorderLight, RoundedCornerShape(12.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) Color.White else ScheduleTextLight
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else ScheduleTextMedium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ScheduleEventCard(
    event: ContentScheduleEvent,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onArtistClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = ScheduleWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artist image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (event.artistImageUrl != null) {
                    AsyncImage(
                        model = event.artistImageUrl,
                        contentDescription = event.artistName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ScheduleTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Event details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.artistName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ScheduleTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.stageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = ScheduleCoralRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Genres
                if (!event.genres.isNullOrEmpty()) {
                    Text(
                        text = event.genres.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ScheduleTextLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time column
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = ScheduleTextLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTime(event.startTime),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ScheduleTextDark
                    )
                }
                Text(
                    text = formatTime(event.endTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = ScheduleTextLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Day ${event.day}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Formats an ISO datetime string to a human-readable time (e.g. "8:00 PM").
 * Falls back to the raw string if parsing fails.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(isoTime: String): String {
    return try {
        val dateTime = LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_DATE_TIME)
        dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: DateTimeParseException) {
        // Try parsing just the time portion
        try {
            val dateTime = LocalDateTime.parse(isoTime.replace("Z", ""))
            dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e2: Exception) {
            isoTime.substringAfter("T").substringBefore("Z").take(5)
        }
    }
}

@Composable
private fun ScheduleErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = ScheduleCoralRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = ScheduleTextMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry")
        }
    }
}
