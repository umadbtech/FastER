package com.faster.festival.ui.screens

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.AppConfig
import com.faster.festival.data.models.AppFestivalHeader
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.repository.AppHomeRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.components.*
import com.faster.festival.ui.theme.FastERTheme
import com.faster.festival.ui.viewmodel.AppHomeViewModel
import com.faster.festival.ui.viewmodel.UiState
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
        modifier: Modifier = Modifier,
        onTicketsClick: () -> Unit = {},
        onFestivalHomeClick: () -> Unit = {},
        onFaqsClick: () -> Unit = {},
        onDeepLink: (String) -> Unit = {},
        accessToken: String? = null,
        festivalSlug: String =
                AppConfig.DEFAULT_FESTIVAL_SLUG // ✅ Use AppConfig instead of hard-coded
) {
    // Create ViewModel with AppHomeRepository (real API)
    val viewModel: AppHomeViewModel =
            viewModel(
                    factory =
                            AppHomeViewModel.createFactory(
                                    appHomeRepository =
                                            AppHomeRepository(
                                                    NetworkModule.appHomeApi,
                                                    NetworkModule.festivalApiService
                                            ),
                                    festivalSlug = festivalSlug // Use festivalSlug parameter
                            )
            )

    val bundleState by viewModel.bundleState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (bundleState) {
            is UiState.Loading -> {
                Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                    )
                    Text(
                            text = "Loading festival...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is UiState.Error -> {
                val errorMessage = (bundleState as UiState.Error).message

                // Determine error type
                val is401Error = errorMessage.contains("401")
                val is404Error = errorMessage.contains("404")
                val is500Error = errorMessage.contains("500")

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        // ERROR BANNER (Non-blocking, at top)
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                            text =
                                                    when {
                                                        is401Error ->
                                                                "Missing Authorization (Backend Issue)"
                                                        is404Error -> "Festival Not Found"
                                                        is500Error -> "Server Error"
                                                        else -> "Connection Error"
                                                    },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                        text =
                                                when {
                                                    is401Error ->
                                                            "The backend needs to be updated to support public festivals without authentication headers. Please contact support or try again."
                                                    is404Error ->
                                                            "The festival could not be found. Verify the festival slug."
                                                    is500Error ->
                                                            "The server is experiencing issues. Please try again later."
                                                    else -> errorMessage
                                                },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(
                                        onClick = { viewModel.refreshBundle() },
                                        modifier =
                                                Modifier.align(Alignment.End).padding(top = 8.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                )
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                    Text("Retry", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    item {
                        // FALLBACK UI (Show basic festival info even on error)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        // Festival Name
                        Text(
                                text = "Festival Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        // Static Festival Info Card
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                                            contentDescription = "Festival",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                                text = "Festival Slug: $festivalSlug",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = "You're viewing: $festivalSlug",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline,
                                        thickness = 1.dp
                                )

                                // Quick Action Tiles (Even on error)
                                Text(
                                        text = "Quick Links",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                )

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickActionTile(
                                            icon = Icons.Default.Groups,
                                            label = "Artists",
                                            onClick = onFestivalHomeClick,
                                            modifier = Modifier.weight(1f)
                                    )
                                    QuickActionTile(
                                            icon = Icons.Default.Schedule,
                                            label = "Schedule",
                                            onClick = onFestivalHomeClick,
                                            modifier = Modifier.weight(1f)
                                    )
                                    QuickActionTile(
                                            icon = Icons.AutoMirrored.Filled.Help,
                                            label = "FAQ",
                                            onClick = onFaqsClick,
                                            modifier = Modifier.weight(1f)
                                    )
                                    QuickActionTile(
                                            icon = Icons.Default.Settings,
                                            label = "Settings",
                                            onClick = onFestivalHomeClick,
                                            modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        // Support Note
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer
                                        )
                        ) {
                            Text(
                                    text =
                                            "The full festival content will appear once the backend is configured to support public access. Basic navigation is still available above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
            is UiState.Success -> {
                val bundle = (bundleState as UiState.Success).data
                HomeScreenContent(
                        modifier = Modifier.fillMaxSize(),
                        bundle = bundle,
                        onTicketsClick = onTicketsClick,
                        onFestivalHomeClick = onFestivalHomeClick,
                        onFaqsClick = onFaqsClick,
                        onDeepLink = onDeepLink,
                        onSettingsClick = {},
                        festivalSlug = festivalSlug,
                        accessToken = accessToken
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreenContent(
        modifier: Modifier = Modifier,
        bundle: AppHomeBundleResponse,
        onTicketsClick: () -> Unit = {},
        onFestivalHomeClick: () -> Unit = {},
        onFaqsClick: () -> Unit = {},
        onDeepLink: (String) -> Unit = {},
        onSettingsClick: () -> Unit = {},
        @Suppress("UNUSED_PARAMETER") festivalSlug: String = "floydfest-26",
        accessToken: String? = null
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            // Full Festival Header Banner Carousel with Image Slider
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(280.dp)
                                    .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(0.dp)
                                    )
            ) {
                // Banner Slider - Auto-rotates through multiple banner URLs
                if (bundle.festival.bannerUrls.isNotEmpty()) {
                    BannerSlider(
                            bannerUrls = bundle.festival.bannerUrls,
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            autoScrollInterval = 5000L // 5 seconds
                    )
                } else if (!bundle.festival.bannerUrl.isNullOrEmpty()) {
                    // Fallback to single banner URL if array is empty
                    AsyncImage(
                            model = bundle.festival.bannerUrl,
                            contentDescription = "${bundle.festival.name} banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                    )

                    // Dark Gradient Overlay
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(
                                                    brush =
                                                            Brush.verticalGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.3f
                                                                                            ),
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.6f
                                                                                            )
                                                                            )
                                                            )
                                            )
                    )
                } else {
                    // Fallback gradient if no banner URLs
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(
                                                    brush =
                                                            Brush.linearGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    MaterialTheme
                                                                                            .colorScheme
                                                                                            .primary
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.5f
                                                                                            ),
                                                                                    MaterialTheme
                                                                                            .colorScheme
                                                                                            .secondary
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.5f
                                                                                            )
                                                                            )
                                                            )
                                            )
                    )

                    // Dark Gradient Overlay
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(
                                                    brush =
                                                            Brush.verticalGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.3f
                                                                                            ),
                                                                                    Color.Black
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.6f
                                                                                            )
                                                                            )
                                                            )
                                            )
                    )
                }

                // Festival Content (text overlay)
                Column(
                        modifier =
                                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                ) {
                    // Logo Circle Avatar (Left Side) - if available
                    if (!bundle.festival.logoUrl.isNullOrEmpty()) {
                        Box(
                                modifier =
                                        Modifier.size(72.dp)
                                                .background(
                                                        color = Color.White.copy(alpha = 0.95f),
                                                        shape = RoundedCornerShape(50)
                                                )
                                                .padding(4.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                    model = bundle.festival.logoUrl,
                                    contentDescription = "Festival logo",
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .background(
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .surface,
                                                            shape = RoundedCornerShape(50)
                                                    ),
                                    contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                            text = bundle.festival.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location and Date Info
                    Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Location Text
                        Text(
                                text = "FestivalPark, Floyd County, Virginia",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )

                        // Formatted Date Range
                        Text(
                                text =
                                        formatFestivalDateRange(
                                                bundle.festival.startsAt,
                                                bundle.festival.endsAt,
                                                bundle.festival.timezone
                                        ),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            // Quick Actions
            QuickActionRow(
                    actions =
                            listOf(
                                    "Schedule" to Icons.Default.DateRange,
                                    "Lineup" to Icons.Default.Group,
                                    "Parking" to Icons.Default.DirectionsCar,
                                    "Wristband" to Icons.Default.CardGiftcard
                            ),
                    onActionClick = { action ->
                        when (action) {
                            "Schedule" -> onFestivalHomeClick()
                            else -> {}
                        }
                    }
            )
        }

        item {
            // Setup Account Card
            SetupAccountCard(
                    onSetupClick = onSettingsClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // EXPLORE SECTION - Check login and published status gate
        val isLoggedIn = accessToken != null && accessToken.isNotBlank()
        val isFestivalPublished = bundle.festival.status == "published"

        // Only show login gate if festival is published AND user is not logged in
        val shouldShowLoginGate = isFestivalPublished && !isLoggedIn

        if (shouldShowLoginGate) {
            // Show login gate
            item {
                HomeLoginGate(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp))
            }
        } else {
            // Show Explore categories (either festival is not published, or user is logged in)
            item {
                Text(
                        text = "Explore ${bundle.festival.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Hero Carousel Items - Always show section
            homeCategorySection(title = "Featured") {
                homeHeroCarouselSection(
                        items = bundle.heroCarouselItems,
                        onItemClick = { item ->
                            if (item.ctaUrl != null) {
                                // Parse the ctaUrl and navigate appropriately
                                when {
                                    item.ctaUrl.contains("/artists/") -> {
                                        // Extract artist slug from /artists/{slug}
                                        val artistSlug = item.ctaUrl.substringAfterLast("/")
                                        // Use Uri.encode() to properly encode special characters
                                        // like hyphens
                                        val encodedSlug = Uri.encode(artistSlug)
                                        onDeepLink("artist/$encodedSlug")
                                    }
                                    item.ctaUrl.contains("/schedule") -> {
                                        onDeepLink("schedule")
                                    }
                                    item.ctaUrl.contains("/parking") -> {
                                        onDeepLink("web/parking")
                                    }
                                    else -> onDeepLink(item.ctaUrl)
                                }
                            }
                        }
                )
            }

            // Announcements - Always show section
            homeCategorySection(title = "Announcements") {
                homeAnnouncementsSection(
                        items = bundle.announcements,
                        onItemClick = { _announcement ->
                            // Handle announcement click
                        }
                )
            }

            // Upcoming Events - Always show section
            homeCategorySection(title = "Upcoming Events") {
                homeUpcomingEventsSection(
                        items = bundle.upcomingEvents,
                        onItemClick = { _event -> onFestivalHomeClick() }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            // Experience List
            ExperienceList(
                    onTicketsClick = onTicketsClick,
                    onFestivalHomeClick = onFestivalHomeClick,
                    onFaqsClick = onFaqsClick
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/** Login gate UI shown when festival is published but user is not logged in */
@Composable
fun HomeLoginGate(modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Login required",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
            )
            Text(
                    text = "Please log in to view this festival",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                    text = "Sign in to explore festival content, events, and announcements",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QuickActionTile(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    OutlinedButton(
            onClick = onClick,
            modifier =
                    modifier.height(72.dp)
                            .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                            ),
            shape = RoundedCornerShape(8.dp),
            colors =
                    ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                    ),
            contentPadding = PaddingValues(4.dp)
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/**
 * Format festival date range from ISO timestamps Example: "July 21 - 27, 2026" or "Jul 30 - Aug 2,
 * 2026"
 */
@RequiresApi(Build.VERSION_CODES.O)
fun formatFestivalDateRange(startsAt: String, endsAt: String, timezone: String): String {
    return try {
        val zoneId = ZoneId.of(timezone)
        val startInstant = Instant.parse(startsAt)
        val endInstant = Instant.parse(endsAt)

        val startZoned = startInstant.atZone(zoneId)
        val endZoned = endInstant.atZone(zoneId)

        val startMonth = startZoned.month.toString().take(3)
        val startDay = startZoned.dayOfMonth
        val endMonth = endZoned.month.toString().take(3)
        val endDay = endZoned.dayOfMonth
        val year = endZoned.year

        if (startZoned.month == endZoned.month) {
            // Same month: "July 21 - 27, 2026"
            val monthFull = startZoned.month.toString()
            "$monthFull $startDay - $endDay, $year"
        } else {
            // Different months: "Jul 30 - Aug 2, 2026"
            "$startMonth $startDay - $endMonth $endDay, $year"
        }
    } catch (e: Exception) {
        // Fallback if parsing fails
        "Festival Dates"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PreviewHomeScreen() {
    FastERTheme {
        @Suppress("NewApi")
        run {
            HomeScreenContent(
                    bundle =
                            AppHomeBundleResponse(
                                    schemaVersion = "1.0",
                                    generatedAt = "2026-03-03T10:30:00Z",
                                    festival =
                                            AppFestivalHeader(
                                                    id = "550e8400-e29b-41d4-a716-446655440000",
                                                    slug = "floydfest-26",
                                                    name = "FloydFest 26",
                                                    timezone = "America/New_York",
                                                    startsAt = "2026-07-22T16:00:00+00:00",
                                                    endsAt = "2026-07-27T03:00:00+00:00",
                                                    logoUrl = "https://example.com/logo.png",
                                                    bannerUrl = "https://example.com/banner.jpg",
                                                    accentColorHex = "#00A86B",
                                                    contextState = "PRE",
                                                    status = "published"
                                            ),
                                    modules = emptyList(),
                                    uiConfig =
                                            com.faster.festival.data.models.UiConfig(
                                                    emptyList(),
                                                    emptyList()
                                            )
                            ),
                    onTicketsClick = {},
                    onFestivalHomeClick = {},
                    onFaqsClick = {},
                    onDeepLink = {},
                    onSettingsClick = {},
                    festivalSlug = "floydfest-26",
                    accessToken = "valid_token"
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun PreviewHomeScreenNotLoggedIn() {
    FastERTheme {
        HomeScreenContent(
                bundle =
                        AppHomeBundleResponse(
                                schemaVersion = "1.0",
                                generatedAt = "2026-03-03T10:30:00Z",
                                festival =
                                        AppFestivalHeader(
                                                id = "550e8400-e29b-41d4-a716-446655440000",
                                                slug = "floydfest-26",
                                                name = "FloydFest 26",
                                                timezone = "America/New_York",
                                                startsAt = "2026-07-22T16:00:00+00:00",
                                                endsAt = "2026-07-27T03:00:00+00:00",
                                                logoUrl = "https://example.com/logo.png",
                                                bannerUrl = "https://example.com/banner.jpg",
                                                accentColorHex = "#00A86B",
                                                contextState = "PRE",
                                                status = "published"
                                        ),
                                modules = emptyList(),
                                uiConfig =
                                        com.faster.festival.data.models.UiConfig(
                                                emptyList(),
                                                emptyList()
                                        )
                        ),
                onTicketsClick = {},
                onFestivalHomeClick = {},
                onFaqsClick = {},
                onDeepLink = {},
                onSettingsClick = {},
                festivalSlug = "floydfest-26",
                accessToken = null
        )
    }
}
