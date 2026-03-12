package com.faster.festival.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.faster.festival.R
import com.faster.festival.data.models.Artist
import com.faster.festival.ui.theme.*

// Festival Hero Header
@Composable
fun FestivalHeroHeader(
        modifier: Modifier = Modifier,
        festivalName: String,
        bannerUrl: String? = null,
        logoUrl: String? = null,
        dateText: String = "",
        accentColorHex: String? = null
) {
        Box(
                modifier =
                        modifier.fillMaxWidth()
                                .height(280.dp)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors = listOf(FasterRed, FasterRedLight)
                                                )
                                )
        ) {
                // Gradient overlay with pattern
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                FasterRed.copy(
                                                                                        alpha = 0.7f
                                                                                ),
                                                                                FasterRedLight.copy(
                                                                                        alpha = 0.9f
                                                                                )
                                                                        )
                                                        )
                                        )
                )

                // Banner background image if available
                if (!bannerUrl.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                                model = bannerUrl,
                                contentDescription = "Festival Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.3f
                        )
                }

                // Content
                Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Logo from API if available, otherwise use app logo
                        if (!logoUrl.isNullOrEmpty()) {
                                coil.compose.AsyncImage(
                                        model = logoUrl,
                                        contentDescription = "Festival Logo",
                                        modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                )
                        } else {
                                Image(
                                        painter = painterResource(id = R.drawable.faster_red),
                                        contentDescription = "Festival Logo",
                                        modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(0.dp)),
                                        contentScale = ContentScale.Fit
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                                text = festivalName,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Display date text if available
                        if (dateText.isNotEmpty()) {
                                Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                        }
                }
        }
}

// Quick Action Row
@Composable
fun QuickActionRow(
        actions: List<Pair<String, ImageVector>>,
        onActionClick: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                repeat((actions.size + 1) / 2) { index ->
                        val action1 = actions.getOrNull(index * 2)
                        val action2 = actions.getOrNull(index * 2 + 1)

                        if (action1 != null) {
                                QuickActionCircle(
                                        label = action1.first,
                                        icon = action1.second,
                                        onClick = { onActionClick(action1.first) },
                                        modifier = Modifier.weight(1f)
                                )
                        }

                        if (action2 != null) {
                                QuickActionCircle(
                                        label = action2.first,
                                        icon = action2.second,
                                        onClick = { onActionClick(action2.first) },
                                        modifier = Modifier.weight(1f)
                                )
                        } else if (action1 != null) {
                                Spacer(modifier = Modifier.weight(1f))
                        }
                }
        }
}

@Composable
fun QuickActionCircle(
        label: String,
        icon: ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Column(
                modifier = modifier.clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onClick
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.fillMaxSize().padding(12.dp)
                        )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
        }
}

// Setup Account Card
@Composable
fun SetupAccountCard(onSetupClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSetupClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF081836)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.faster_red),
                contentDescription = "FASTER Logo",
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connect your FASTER Band",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "To access our life saving technology.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

// Headliner Row
@Composable
fun HeadlinerRowOrGrid(
        artists: List<Artist>,
        onArtistClick: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        Column(modifier = modifier.fillMaxWidth()) {
                Text(
                        text = "Headliners",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        items(artists) { artist ->
                                HeadlinerCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) },
                                        modifier = Modifier.width(140.dp)
                                )
                        }
                }
        }
}

@Composable
fun HeadlinerCard(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
        ElevatedCard(
                modifier = modifier.clickable(onClick = onClick).height(160.dp),
                shape = RoundedCornerShape(16.dp)
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outlineVariant,
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surface
                                                                        )
                                                        )
                                        )
                ) {
                        // Placeholder for artist image
                        Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = artist.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                        Modifier.align(Alignment.TopCenter)
                                                .padding(16.dp)
                                                .size(60.dp)
                        )

                        // Artist name at bottom
                        Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

// Experience List
@Composable
fun ExperienceList(
        onTicketsClick: () -> Unit,
        onFestivalHomeClick: () -> Unit,
        onFaqsClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                        text = "Experience",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 12.dp)
                )

                ExperienceListItem(
                        icon = Icons.Default.ConfirmationNumber,
                        label = "Tickets",
                        onClick = onTicketsClick
                )

                ExperienceListItem(
                        icon = Icons.Default.Home,
                        label = "Festival Home",
                        onClick = onFestivalHomeClick
                )

                ExperienceListItem(icon = Icons.Filled.Help, label = "FAQs", onClick = onFaqsClick)
        }
}

@Composable
fun ExperienceListItem(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                        )
                        Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                }

                Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// Tickets FAB Pill
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsFabPill(onClick: () -> Unit, modifier: Modifier = Modifier, isVisible: Boolean = true) {
        AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
        ) {
                ExtendedFloatingActionButton(
                        onClick = onClick,
                        modifier = Modifier.height(48.dp).wrapContentWidth(),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = {
                                Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = "Tickets"
                                )
                        },
                        text = {
                                Text(text = "Tickets", style = MaterialTheme.typography.labelLarge)
                        }
                )
        }
}

@Preview
@Composable
fun PreviewFestivalHeroHeader() {
        FastERTheme {
                FestivalHeroHeader(
                        festivalName = "FloydFest 26",
                        bannerUrl = null,
                        logoUrl = null,
                        dateText = "July 21 - 27, 2026",
                        accentColorHex = "#00A86B"
                )
        }
}

@Preview
@Composable
fun PreviewQuickActionCircle() {
        FastERTheme {
                QuickActionCircle(label = "Schedule", icon = Icons.Default.DateRange, onClick = {})
        }
}
