package com.faster.festival.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.festival.data.models.UpcomingEvent
import com.faster.festival.data.models.Venue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ═══════════════════════════════════════════════════════════════════════════════
// UPCOMING EVENT DETAIL SCREEN — LineupScreen-consistent light theme
// ═══════════════════════════════════════════════════════════════════════════════

private val ScreenBg = Color(0xFFF7F7F7)
private val TextDark = Color(0xFF222222)
private val TextMedium = Color(0xFF333333)
private val TextLight = Color(0xFF666666)
private val CoralRed = Color(0xFFE53935)
private val DividerLight = Color(0xFFE0E0E0)

// ═══════════════════════════════════════════════════════════════════════════════
// DATE HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDay(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE"))
} catch (_: Exception) { "" }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatFullDate(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
} catch (_: Exception) { "" }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
} catch (_: Exception) { "" }

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateRange(start: String, end: String?): String {
    if (end == null) return "${formatDay(start)}, ${formatFullDate(start)}"
    val s = formatFullDate(start)
    val e = formatFullDate(end)
    return if (s == e) s else "$s — $e"
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDuration(start: String, end: String?): String {
    if (end == null) return ""
    return try {
        val s = Instant.parse(start)
        val e = Instant.parse(end)
        val hours = ChronoUnit.HOURS.between(s, e)
        val mins = ChronoUnit.MINUTES.between(s, e) % 60
        when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> ""
        }
    } catch (_: Exception) { "" }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpcomingEventDetailScreen(
    event: UpcomingEvent,
    onBackClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Event Details",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLight,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header: Title + Status ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 5 }
            ) {
                EventHeaderSection(event = event)
            }

            // ── Schedule Card ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 80)) + slideInVertically(tween(500, 80)) { it / 5 }
            ) {
                EventScheduleCard(event = event)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Venue Card ──
            event.venue?.let { venue ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(550, 160)) + slideInVertically(tween(550, 160)) { it / 5 }
                ) {
                    VenueCard(venue = venue)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Description / About ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 240)) + slideInVertically(tween(600, 240)) { it / 5 }
            ) {
                EventAboutCard(event = event)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HEADER SECTION
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventHeaderSection(event: UpcomingEvent) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(4.dp))

        // Large event icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CoralRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = CoralRed,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Event name (use name if available, else title)
        Text(
            text = event.name ?: event.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 34.sp
            ),
            color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Date summary line
        Text(
            text = formatDateRange(event.startsAt, event.endsAt),
            style = MaterialTheme.typography.bodyLarge,
            color = CoralRed,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status + duration chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val duration = formatDuration(event.startsAt, event.endsAt)
            if (duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(TextLight.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accent divider
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CoralRed)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCHEDULE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventScheduleCard(event: UpcomingEvent) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "Schedule")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Start
                ScheduleRow(
                    icon = Icons.Default.EventAvailable,
                    label = "STARTS",
                    day = formatDay(event.startsAt),
                    date = formatFullDate(event.startsAt),
                    time = formatTime(event.startsAt)
                )

                // Connecting line
                Row(modifier = Modifier.padding(start = 19.dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(DividerLight)
                    )
                }

                // End
                event.endsAt?.let { end ->
                    ScheduleRow(
                        icon = Icons.Default.EventBusy,
                        label = "ENDS",
                        day = formatDay(end),
                        date = formatFullDate(end),
                        time = formatTime(end)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    icon: ImageVector,
    label: String,
    day: String,
    date: String,
    time: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CoralRed.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CoralRed, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = TextLight,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (day.isNotBlank()) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMedium
            )
        }
        if (time.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoralRed.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// VENUE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VenueCard(venue: Venue) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "Venue")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CoralRed.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (venue.kind?.lowercase()) {
                                "stage" -> Icons.Default.Stadium
                                "area" -> Icons.Default.Place
                                else -> Icons.Default.LocationOn
                            },
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = venue.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        venue.kind?.let {
                            Text(
                                text = it.replaceFirstChar { c -> c.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight
                            )
                        }
                    }
                }

                venue.location?.let {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerLight)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMedium
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ABOUT CARD
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventAboutCard(event: UpcomingEvent) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "About")
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Accent top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CoralRed,
                                    CoralRed.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))

                val descriptionText = event.description
                if (!descriptionText.isNullOrBlank()) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = TextMedium
                    )
                } else {
                    // Auto-generated summary
                    val summary = buildString {
                        append("\"${event.title}\"")
                        event.venue?.let { append(" takes place at ${it.name}") }
                        append(" on ${formatDay(event.startsAt)}, ${formatFullDate(event.startsAt)}")
                        val time = formatTime(event.startsAt)
                        if (time.isNotBlank()) append(" at $time")
                        append(".")
                        val duration = formatDuration(event.startsAt, event.endsAt)
                        if (duration.isNotBlank()) append(" Duration: $duration.")
                    }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = TextMedium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CoralRed)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun UpcomingEventDetailLoadingState() {
    val transition = rememberInfiniteTransition(label = "eventShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "eventAlpha"
    )
    val s = Color.Gray.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(top = 64.dp, start = 20.dp, end = 20.dp)
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(s))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(30.dp).clip(RoundedCornerShape(8.dp)).background(s))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.5f).height(20.dp).clip(RoundedCornerShape(6.dp)).background(s))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(90.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(s))
            Box(Modifier.width(60.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(s))
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(s))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(s))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).background(s))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ERROR STATE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingEventDetailErrorState(message: String, onBackClick: () -> Unit) {
    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Error", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = TextDark)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                Icon(Icons.Default.WarningAmber, null, tint = CoralRed, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Unable to load event", style = MaterialTheme.typography.titleMedium,
                color = TextDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = TextLight, textAlign = TextAlign.Center)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun UpcomingEventDetailPreview() {
    val mock = UpcomingEvent(
        id = "evt-001",
        title = "Main Stage Opening Ceremony",
        name = "Opening Ceremony",
        description = "Join us for the spectacular opening ceremony featuring live performances, fireworks, and special guest appearances. This year's ceremony celebrates 10 years of festival history.",
        startsAt = "2026-07-22T18:00:00+00:00",
        endsAt = "2026-07-22T20:30:00+00:00",
        status = "published",
        venue = Venue(
            id = "v-001",
            kind = "stage",
            name = "Thunder Stage",
            slug = "thunder-stage",
            location = "North Field, Section A"
        )
    )
    MaterialTheme {
        UpcomingEventDetailScreen(event = mock, onBackClick = {})
    }
}
