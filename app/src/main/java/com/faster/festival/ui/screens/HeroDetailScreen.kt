package com.faster.festival.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.faster.festival.data.models.HeroCarouselItem

// Light theme palette (consistent with HomeScreen)
private val HeroBg = Color(0xFFF7F7F7)
private val HeroWhite = Color.White
private val HeroCoralRed = Color(0xFFE53935)
private val HeroTextDark = Color(0xFF222222)
private val HeroTextMedium = Color(0xFF333333)
private val HeroTextLight = Color(0xFF666666)
private val HeroBorderLight = Color(0xFFE0E0E0)

@Composable
fun HeroDetailScreen(
    heroItem: HeroCarouselItem,
    onBackClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HeroBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroWhite)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HeroTextDark
                )
            }

            Text(
                text = heroItem.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HeroTextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            IconButton(onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, heroItem.title + (heroItem.subtitle?.let { " - $it" } ?: ""))
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                } catch (_: Exception) { }
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = HeroTextDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero image
            if (heroItem.imageUrl != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        AsyncImage(
                            model = heroItem.imageUrl,
                            contentDescription = heroItem.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Dark gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.05f),
                                            0.5f to Color.Black.copy(alpha = 0.15f),
                                            1.0f to Color.Black.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        // Kind badge — top-right
                        heroItem.kind?.let { kind ->
                            Text(
                                text = kind.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        // Star icon — bottom-right
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(28.dp),
                            tint = HeroCoralRed
                        )
                    }
                }
            }

            // Title + subtitle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeroWhite)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = heroItem.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = HeroTextDark
                    )

                    heroItem.subtitle?.let { sub ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HeroTextMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        heroItem.kind?.let { kind ->
                            HeroTagChip(
                                text = kind.replaceFirstChar { it.uppercase() },
                                isHighlighted = kind == "artist"
                            )
                        }
                    }
                }
            }

            // Quick info pills (date, location)
            val hasDateOrLocation = heroItem.startsAt != null || heroItem.locationText != null || heroItem.venueName != null
            if (hasDateOrLocation) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        heroItem.startsAt?.let { date ->
                            HeroInfoPill(
                                icon = Icons.Default.CalendarMonth,
                                text = formatDateTime(date, heroItem.endsAt),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val venue = heroItem.venueName ?: heroItem.locationText
                        venue?.let {
                            HeroInfoPill(
                                icon = Icons.Default.LocationOn,
                                text = it,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Image gallery
            if (heroItem.mediaUrls.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        items(heroItem.mediaUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Gallery image",
                                modifier = Modifier
                                    .size(width = 160.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // View Artist / CTA button
                    if (heroItem.kind == "artist" && heroItem.refId != null) {
                        Button(
                            onClick = { onArtistClick(heroItem.refId) },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCoralRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Artist", fontWeight = FontWeight.SemiBold)
                        }
                    } else if (heroItem.ctaUrl != null) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(heroItem.ctaUrl)))
                                } catch (_: Exception) { }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeroCoralRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = heroItem.ctaLabel ?: "Learn More",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Directions (if location available)
                    val location = heroItem.locationText ?: heroItem.venueName
                    if (location != null) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) { }
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HeroBorderLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = HeroTextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Directions", fontWeight = FontWeight.SemiBold, color = HeroTextDark)
                        }
                    }
                }
            }

            // Description / About
            heroItem.description?.let { desc ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 20.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroTextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HeroWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = HeroTextMedium,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Schedule info
            if (heroItem.startsAt != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 20.dp, bottom = 44.dp)
                    ) {
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroTextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HeroWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = HeroCoralRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = formatDateTime(heroItem.startsAt, heroItem.endsAt),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = HeroTextDark
                                        )
                                        val venue = heroItem.venueName ?: heroItem.locationText
                                        if (venue != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = venue,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = HeroTextLight
                                            )
                                        }
                                    }
                                     Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format date/time display from ISO strings.
 * Shows a simple readable format.
 */
private fun formatDateTime(startsAt: String?, endsAt: String?): String {
    if (startsAt == null) return ""
    // Extract date and time portion from ISO string
    val startDate = startsAt.substringBefore("T")
    val startTime = startsAt.substringAfter("T").substringBefore("+").substringBefore("Z").take(5)
    val endTime = endsAt?.substringAfter("T")?.substringBefore("+")?.substringBefore("Z")?.take(5)

    return if (endTime != null) {
        "$startDate  $startTime - $endTime"
    } else {
        "$startDate  $startTime"
    }
}

@Composable
private fun HeroTagChip(
    text: String,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isHighlighted) HeroCoralRed else HeroBorderLight,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (isHighlighted) HeroCoralRed.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) HeroCoralRed else HeroTextMedium,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun HeroInfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HeroWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HeroCoralRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = HeroTextMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
