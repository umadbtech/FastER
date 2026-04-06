package com.faster.festival.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.faster.festival.data.models.Announcement
import com.faster.festival.data.models.HeroCarouselItem
import com.faster.festival.data.models.UpcomingEvent

/** Home category section with title and scrollable cards */
fun LazyListScope.homeCategorySection(
        title: String,
        modifier: Modifier = Modifier,
        content: LazyListScope.() -> Unit
) {
    item {
        Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier =
                        modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 16.dp)
        )
    }
    content()
}

/** Empty state message for a category */
@Composable
fun HomeCategoryEmpty(message: String, modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .height(80.dp),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Hero carousel card - Full width with image and text overlay Matches reference design: background
 * image, dark gradient, title/subtitle top-left, icon bottom-left
 */
@Composable
fun HomeExploreCard(item: HeroCarouselItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Card with rounded corners
    Card(
            modifier =
                    modifier.fillMaxWidth() // ✅ Fill grid column width
                            .aspectRatio(0.8f) // ✅ Maintain 320x400 aspect ratio (portrait)
                            .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp), // ✅ Prominent rounded corners
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image - fills entire card
            if (item.imageUrl != null) {
                AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier =
                                Modifier.fillMaxSize()
                                        .clip(
                                                RoundedCornerShape(16.dp)
                                        ), // ✅ Ensure image respects rounded corners
                        contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder background
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = "No image",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✅ Dark Gradient Overlay - from top to bottom for text readability
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            brush =
                                                    androidx.compose.ui.graphics.Brush
                                                            .verticalGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.9f
                                                                                            ), // Dark at top
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.5f
                                                                                            ), // Medium in middle
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.7f
                                                                                            ) // Darker at bottom for icon
                                                                            )
                                                            )
                                    )
            )

            // ✅ Content Layout: Title & Subtitle at TOP-LEFT
            Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Top, // ✅ Align to top
                    horizontalAlignment = Alignment.Start // ✅ Align to left
            ) {
                // Title
                Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red, // White for visibility
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )

                // Subtitle (optional)
                if (item.subtitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ✅ Bottom-Left Icon/Badge
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Icon(
                        imageVector = Icons.Default.LocalActivity, // ✅ Festival icon for /hero
                        contentDescription = item.ctaLabel ?: "View",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                )
            }
        }
    }
}

/** Announcement card */
@Composable
fun HomeAnnouncementCard(
        announcement: Announcement,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    ElevatedCard(
            modifier = modifier.width(280.dp).height(200.dp).clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Image if available
            if (announcement.imageUrl != null) {
                AsyncImage(
                        model = announcement.imageUrl,
                        contentDescription = announcement.title,
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentScale = ContentScale.Crop
                )
            } else {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(100.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = "No image",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title and content
            Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
                announcement.content?.let {
                    Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Upcoming event card */
@Composable
fun HomeEventCard(event: UpcomingEvent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
            modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Event title
            Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
            )

            // Venue and time
            if (event.venue != null) {
                Text(
                        text = event.venue.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // Description
            event.description?.let {
                Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // Time info
            Text(
                    text = event.startsAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Grid of hero carousel cards (2-column grid layout) Matches reference design showing cards in a
 * grid view Uses Column+Row pattern to avoid nested scrollable constraint issues
 */
fun LazyListScope.homeHeroCarouselSection(
        items: List<HeroCarouselItem>,
        onItemClick: (HeroCarouselItem) -> Unit,
        modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        item { HomeCategoryEmpty("No hero items") }
    } else {
        items(items.chunked(2).size) { index ->
            val row = items.chunked(2)[index]
            Row(
                    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        HomeExploreCard(item = item, onClick = { onItemClick(item) })
                    }
                }
                // Fill empty space if row has 1 item (odd count)
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Grid of announcement cards (2-column grid) NOTE: This is now a simple list renderer - grid layout
 * is handled by parent LazyColumn
 */
fun LazyListScope.homeAnnouncementsSection(
        items: List<Announcement>,
        onItemClick: (Announcement) -> Unit,
        modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        item { HomeCategoryEmpty("No announcements") }
    } else {
        items(items.chunked(2).size) { index ->
            val row = items.chunked(2)[index]
            Row(
                    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        HomeAnnouncementCard(announcement = item, onClick = { onItemClick(item) })
                    }
                }
                // Fill empty space if odd number
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Grid of upcoming event cards (2-column grid) NOTE: This is now a simple list renderer - grid
 * layout is handled by parent LazyColumn
 */
fun LazyListScope.homeUpcomingEventsSection(
        items: List<UpcomingEvent>,
        onItemClick: (UpcomingEvent) -> Unit,
        modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        item { HomeCategoryEmpty("No upcoming events") }
    } else {
        items(items.chunked(2).size) { index ->
            val row = items.chunked(2)[index]
            Row(
                    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        HomeEventCard(event = item, onClick = { onItemClick(item) })
                    }
                }
                // Fill empty space if odd number
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
