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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.utils.DateFormatter

// ═══════════════════════════════════════════════════════════════════════════════
// FESTIVAL DETAILS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

// Palette — consistent with LineupScreen
private val ScreenBg = Color(0xFFF7F7F7)
private val TextDark = Color(0xFF222222)
private val TextMedium = Color(0xFF333333)
private val TextLight = Color(0xFF666666)
private val DefaultAccent = Color(0xFFE53935)
private val DividerLight = Color(0xFFE0E0E0)
private val DarkNavy = Color(0xFF0D1B2A)

// ═══════════════════════════════════════════════════════════════════════════════
// COLOR HELPER
// ═══════════════════════════════════════════════════════════════════════════════

fun String?.toComposeColorOrDefault(default: Color = DefaultAccent): Color {
    if (this.isNullOrBlank() || this == "null") return default
    return try {
        val hex = this.removePrefix("#")
        when (hex.length) {
            6 -> Color(0xFF000000 or hex.toLong(16))
            8 -> Color(hex.toLong(16))
            else -> default
        }
    } catch (_: Exception) { default }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DATE HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

private fun formatDay(isoDate: String): String = formatWith(isoDate, "EEEE")
private fun formatFullDate(isoDate: String): String = formatWith(isoDate, "MMMM dd, yyyy")
private fun formatTime(isoDate: String): String = formatWith(isoDate, "h:mm a")

private fun formatWith(isoDate: String, pattern: String): String {
    return try {
        val cleaned = isoDate.replace("Z", "").replace(Regex("[+\\-]\\d{2}:\\d{2}$"), "")
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH)
        fmt.isLenient = true
        val date = fmt.parse(cleaned) ?: return ""
        java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).format(date)
    } catch (_: Exception) { "" }
}

private fun countDays(startsAt: String, endsAt: String): Int {
    return try {
        fun parse(iso: String): Long {
            val cleaned = iso.replace("Z", "").replace(Regex("[+\\-]\\d{2}:\\d{2}$"), "")
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ENGLISH)
            fmt.isLenient = true
            return fmt.parse(cleaned)?.time ?: 0L
        }
        val diff = parse(endsAt) - parse(startsAt)
        maxOf(1, (diff / (1000 * 60 * 60 * 24)).toInt() + 1)
    } catch (_: Exception) { 0 }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FestivalDetailsScreen(
    festival: FestivalHeader,
    bannerUrls: List<String> = emptyList(),
    location: String? = null,
    onBackClick: () -> Unit
) {
    val accent = festival.accentColorHex.toComposeColorOrDefault()
    val images = bannerUrls.ifEmpty { listOfNotNull(festival.bannerUrl.ifBlank { null }) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var popupImageUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = festival.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "/${festival.slug}",
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
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(festival.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${festival.name} logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
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
            // ═════════════════════════════════════════════════════════════════
            // SECTION 1 — About / Overview (top)
            // ═════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 5 }
            ) {
                AboutSection(
                    festival = festival,
                    location = location,
                    accent = accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ═════════════════════════════════════════════════════════════════
            // SECTION 2 — Festival Intro
            // ═════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 80)) + slideInVertically(tween(500, 80)) { it / 5 }
            ) {
                FestivalIntroSection(festival = festival, accent = accent)
            }

            // ═════════════════════════════════════════════════════════════════
            // SECTION 3 — Event Schedule
            // ═════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(550, 160)) + slideInVertically(tween(550, 160)) { it / 5 }
            ) {
                EventScheduleSection(
                    festival = festival,
                    location = location,
                    accent = accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ═════════════════════════════════════════════════════════════════
            // SECTION 4 — Quick Info Chips
            // ═════════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 240)) + slideInVertically(tween(600, 240)) { it / 5 }
            ) {
                QuickInfoSection(festival = festival, accent = accent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═════════════════════════════════════════════════════════════════
            // SECTION 5 — Gallery (2×2 grid)
            // ═════════════════════════════════════════════════════════════════
            if (images.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(650, 320)) + slideInVertically(tween(650, 320)) { it / 5 }
                ) {
                    GallerySection(
                        images = images,
                        festivalName = festival.name,
                        accent = accent,
                        onImageClick = { popupImageUrl = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    popupImageUrl?.let { url ->
        FullscreenImagePopup(imageUrl = url, onDismiss = { popupImageUrl = null })
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 1 — Festival Intro
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FestivalIntroSection(
    festival: FestivalHeader,
    accent: Color
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(4.dp))

        // Date range — prominent
        Text(
            text = DateFormatter.formatDateRange(
                festival.startsAt, festival.endsAt, festival.timezone
            ),
            style = MaterialTheme.typography.titleMedium,
            color = TextDark,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status chip
        StatusChip(state = festival.contextState, accentColor = accent)

        Spacer(modifier = Modifier.height(16.dp))

        // Accent divider
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 2 — Event Schedule
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EventScheduleSection(
    festival: FestivalHeader,
    location: String?,
    accent: Color
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "Event Schedule", accent = accent)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // ── Opens ──
                ScheduleBlock(
                    icon = Icons.Default.EventAvailable,
                    label = "OPENS",
                    day = formatDay(festival.startsAt),
                    date = formatFullDate(festival.startsAt),
                    time = formatTime(festival.startsAt),
                    accent = accent
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Connecting line
                Row(modifier = Modifier.padding(start = 19.dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(DividerLight)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── Closes ──
                ScheduleBlock(
                    icon = Icons.Default.EventBusy,
                    label = "CLOSES",
                    day = formatDay(festival.endsAt),
                    date = formatFullDate(festival.endsAt),
                    time = formatTime(festival.endsAt),
                    accent = accent
                )

                // ── Location ──
                location?.let {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerLight)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "VENUE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleBlock(
    icon: ImageVector,
    label: String,
    day: String,
    date: String,
    time: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = TextLight,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = day,
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMedium
            )
        }

        // Time pill
        if (time.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 3 — Quick Info Chips
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickInfoSection(
    festival: FestivalHeader,
    accent: Color
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "Quick Info", accent = accent)
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                icon = Icons.Default.Public,
                label = festival.timezone,
                accent = accent
            )
            InfoChip(
                icon = Icons.Default.CalendarMonth,
                label = "${countDays(festival.startsAt, festival.endsAt)} days",
                accent = accent
            )
            InfoChip(
                icon = Icons.Default.AccessTime,
                label = "Gates ${formatTime(festival.startsAt)}",
                accent = accent
            )
            InfoChip(
                icon = Icons.Default.Fingerprint,
                label = festival.id.take(8) + "…",
                accent = accent,
                subtle = true
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    subtle: Boolean = false
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (subtle) ScreenBg else Color.White)
            .border(1.dp, if (subtle) DividerLight else accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (subtle) TextLight else accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (subtle) TextLight else TextMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 4 — About / Overview
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AboutSection(
    festival: FestivalHeader,
    location: String?,
    accent: Color
) {
    val days = countDays(festival.startsAt, festival.endsAt)
    val dateRange = DateFormatter.formatDateRange(
        festival.startsAt, festival.endsAt, festival.timezone
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "About", accent = accent)
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
                                    accent,
                                    accent.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Summary text
                Text(
                    text = buildString {
                        append("${festival.name} is a ")
                        if (days > 0) append("$days-day ")
                        append("festival")
                        if (location != null) append(" at $location")
                        append(", running $dateRange.")
                        append(" All times shown in ${festival.timezone.substringAfterLast("/")}.")
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = TextMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBlock(
                        value = if (days > 0) "$days" else "—",
                        label = "Days",
                        accent = accent
                    )
                    StatBlock(
                        value = festival.timezone.substringAfterLast("/").replace("_", " "),
                        label = "Timezone",
                        accent = accent
                    )
                    StatBlock(
                        value = festival.contextState.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        label = "Status",
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextLight,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION 5 — Gallery
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GallerySection(
    images: List<String>,
    festivalName: String,
    accent: Color,
    onImageClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(text = "Gallery", accent = accent)
        Spacer(modifier = Modifier.height(12.dp))

        val rows = images.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { url ->
                        GalleryGridImage(
                            imageUrl = url,
                            festivalName = festivalName,
                            onClick = { onImageClick(url) },
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

// ═══════════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
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

@Composable
private fun StatusChip(
    state: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val (chipColor, chipText) = when (state.uppercase()) {
        "PRE" -> Pair(Color(0xFFFFA726), "Pre-Festival")
        "LIVE", "ACTIVE" -> Pair(Color(0xFF66BB6A), "Live Now")
        "POST" -> Pair(Color(0xFF78909C), "Post-Festival")
        "DRAFT" -> Pair(Color(0xFFFFCA28), "Coming Soon")
        "PUBLISHED" -> Pair(accentColor, "Published")
        else -> Pair(accentColor, state)
    }

    Box(
        modifier = modifier
            .background(chipColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.uppercase() == "LIVE" || state.uppercase() == "ACTIVE") {
                val transition = rememberInfiniteTransition(label = "pulse")
                val alpha by transition.animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(800, easing = LinearEasing), RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(chipColor.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = chipText,
                style = MaterialTheme.typography.labelSmall,
                color = chipColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun GalleryGridImage(
    imageUrl: String,
    festivalName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl).crossfade(300).build(),
                contentDescription = festivalName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { ImageShimmer() },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1A0033), DarkNavy))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.1f),
                                0.5f to Color.Black.copy(alpha = 0.2f),
                                1.0f to Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun FullscreenImagePopup(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl).crossfade(300).build(),
                contentDescription = "Gallery image",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                )
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ImageShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        startX = shimmerX * 1000f,
                        endX = (shimmerX + 0.6f) * 1000f
                    )
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOADING STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FestivalDetailsLoadingState() {
    val transition = rememberInfiniteTransition(label = "loadShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "loadAlpha"
    )
    val s = Color.Gray.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(top = 64.dp, start = 20.dp, end = 20.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.8f).height(32.dp).clip(RoundedCornerShape(8.dp)).background(s))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.4f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(s))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(90.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(s))
            Box(Modifier.width(140.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(s))
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(s))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(Modifier.width(100.dp).height(36.dp).clip(RoundedCornerShape(10.dp)).background(s)) }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(s))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(s)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ERROR STATE
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalDetailsErrorState(message: String, onBackClick: () -> Unit) {
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
                Icon(Icons.Default.Info, null, tint = DefaultAccent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Unable to load festival", style = MaterialTheme.typography.titleMedium,
                color = TextDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = TextLight, textAlign = TextAlign.Center)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun FestivalDetailsScreenPreview() {
    val mock = FestivalHeader(
        id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        slug = "electric-dusk-2026",
        name = "Electric Dusk Festival",
        timezone = "America/New_York",
        startsAt = "2026-07-22T16:00:00+00:00",
        endsAt = "2026-07-27T03:00:00+00:00",
        logoUrl = "https://example.com/logo.png",
        bannerUrl = "https://example.com/banner.jpg",
        accentColorHex = "#00A86B",
        contextState = "PRE"
    )
    MaterialTheme {
        FestivalDetailsScreen(
            festival = mock,
            bannerUrls = listOf(
                "https://example.com/banner1.jpg",
                "https://example.com/banner2.jpg",
                "https://example.com/banner3.jpg",
                "https://example.com/banner4.jpg"
            ),
            location = "Central Park, New York",
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FestivalDetailsLoadingPreview() {
    MaterialTheme { FestivalDetailsLoadingState() }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun FestivalDetailsErrorPreview() {
    MaterialTheme {
        FestivalDetailsErrorState("Network connection failed.", onBackClick = {})
    }
}
