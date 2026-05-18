package com.faster.festival.ui.screens

import android.os.Build
import com.faster.festival.R
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.AppConfig
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.models.Announcement
import com.faster.festival.data.models.AppFestivalHeader
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.FaqItem
import com.faster.festival.data.models.HeroCarouselItem
import com.faster.festival.data.models.HomeModule
import com.faster.festival.data.models.PromotionItem
import com.faster.festival.data.models.SponsorOffer
import com.faster.festival.data.models.TileConfig
import com.faster.festival.data.models.AlertItem
import com.faster.festival.data.models.PerkItem
import com.faster.festival.data.models.UpcomingEvent
import com.faster.festival.data.models.Venue
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.HomeUiState
import com.faster.festival.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

// ─── Colors ──────────────────────────────────────────────────────────────────
private val CoralRed = Color(0xFFE53935)
private val DarkNavy = Color(0xFF0D1B2A)
private val LightBlue = Color(0xFFB0D4F1)
private val PromoGradientStart = Color(0xFF0D2B4E)
private val PromoGradientEnd = Color(0xFF1A4A7A)

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE KEY → DISPLAY TITLE RESOLVER
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Resolves a display title for a module.
 * Priority: module.displayTitle > module.title > humanized key
 */
private fun resolveModuleTitle(module: HomeModule): String {
    return module.displayTitle
        ?: module.title
        ?: defaultSectionTitle(module.key)
}

/**
 * Resolves a section title from module key by looking up the module in the bundle.
 * Falls back to humanized key if module not found or has no title.
 */
private fun resolveSectionTitle(bundle: AppHomeBundleResponse, moduleKey: String): String {
    val module = bundle.moduleByKey(moduleKey)
    return if (module != null) resolveModuleTitle(module) else defaultSectionTitle(moduleKey)
}

private val sectionTitleOverrides = mapOf(
    "hero_carousel" to "Must See"
)

private fun defaultSectionTitle(key: String): String {
    return sectionTitleOverrides[key] ?: humanizeModuleKey(key)
}

/**
 * Humanizes a snake_case module key into a Title Case display string.
 * e.g. "hero_carousel" → "Hero Carousel", "upcoming_events" → "Upcoming Events"
 */
private fun humanizeModuleKey(key: String): String {
    return key.split("_").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Resolves a tile label from API-provided label or humanized key fallback.
 */
private fun resolveTileLabel(tile: TileConfig): String {
    return tile.label ?: humanizeModuleKey(tile.key)
}

/**
 * Resolves a tile description from API-provided description or empty fallback.
 */
private fun resolveTileDescription(tile: TileConfig): String {
    return tile.description ?: ""
}

// ═══════════════════════════════════════════════════════════════════════════════
// HOME SCREEN — Entry point
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onArtistClick: (String) -> Unit = {},
    onHeroItemClick: (String) -> Unit = {},
    onPromotionClick: (String) -> Unit = {},
    onSponsorClick: (String) -> Unit = {},
    onAnnouncementClick: (String) -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToFaster: () -> Unit = {},
    onFestivalBannerClick: (String) -> Unit = {},
    onUpcomingEventClick: (String) -> Unit = {},
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> },
    festivalSlug: String = AppConfig.DEFAULT_FESTIVAL_SLUG,
    sessionManager: EncryptedSessionManager? = null
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            appHomeApi = NetworkModule.appHomeApi,
            festivalSlug = festivalSlug,
            networkMonitor = com.faster.festival.di.ConnectivityModule.networkMonitor
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState is HomeUiState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val topBarHeight = 44.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(topBarHeight))
                        HomeShimmerLoading()
                    }
                }
                is HomeUiState.Error -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(topBarHeight))
                        HomeErrorState(
                            message = state.message,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
                is HomeUiState.Offline -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(topBarHeight))
                        com.faster.festival.ui.components.network.NoInternetScreen(
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
                is HomeUiState.Success -> {
                    HomeSuccessContent(
                        bundle = state.data,
                        onArtistClick = onArtistClick,
                        onHeroItemClick = onHeroItemClick,
                        onPromotionClick = onPromotionClick,
                        onSponsorClick = onSponsorClick,
                        onAnnouncementClick = onAnnouncementClick,
                        onNavigateToSchedule = onNavigateToSchedule,
                        onNavigateToMap = onNavigateToMap,
                        onNavigateToFAQ = onNavigateToFAQ,
                        onNavigateToFaster = onNavigateToFaster,
                        onFestivalBannerClick = onFestivalBannerClick,
                        onUpcomingEventClick = onUpcomingEventClick,
                        onCtaClick = onCtaClick
                    )
                }
            }

            // Floating top bar — live wristband status from SQLite
            val showComingSoonTopBar = com.faster.festival.utils.rememberComingSoonToast(
                "This Feature is coming soon"
            )
            val pairedForTopBar by com.faster.festival.di.DatabaseModule.wristbandRepository
                .activeWristband
                .collectAsState(initial = null)
            FasterTopAppBar(
                logoUrl = (uiState as? HomeUiState.Success)?.data?.festival?.logoUrl,
                isConnected = pairedForTopBar != null,
                onSearchClick = { showComingSoonTopBar() },
                onWristbandClick = onNavigateToFaster,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHIMMER & ERROR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HomeShimmerLoading() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = alpha))
                    )
                }
            }
        }
        items(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun HomeErrorState(message: String, onRetry: () -> Unit) {
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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(humanizeModuleKey("retry"))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HOME SUCCESS CONTENT — Priority Hierarchy (Safety First)
//
// Rendering order:
//   1. Emergency Access (future persistent CTA)
//   2. Wristband Status (DeviceCard)
//   3. Happening Now (announcements + upcoming_events)
//   4. Safety Messaging (alerts)
//   5. Festival Exploration (hero_carousel + explore tiles)
//   6. Sponsors, Promotions, Last set FAQ
//   Plus: Perks, Footer
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeSuccessContent(
    bundle: AppHomeBundleResponse,
    onArtistClick: (String) -> Unit,
    onHeroItemClick: (String) -> Unit,
    onPromotionClick: (String) -> Unit,
    onSponsorClick: (String) -> Unit,
    onAnnouncementClick: (String) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFAQ: () -> Unit,
    onNavigateToFaster: () -> Unit,
    onFestivalBannerClick: (String) -> Unit,
    onUpcomingEventClick: (String) -> Unit,
    onCtaClick: (url: String, title: String) -> Unit
) {
    // Paired wristband from SQLite — live-updates across app
    val pairedWristband by com.faster.festival.di.DatabaseModule.wristbandRepository
        .activeWristband
        .collectAsState(initial = null)
    val festival = bundle.festival
    val tiles = bundle.uiConfig.tiles.filter { it.enabled }.sortedBy { it.order }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── 0. Festival Banner Header ──
        item {
            FestivalBannerHeader(
                festival = festival,
                modifier = Modifier.clickable { onFestivalBannerClick(festival.slug) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 1: Emergency Access (future persistent CTA)
        // ══════════════════════════════════════════════════════════════════
        item { EmergencyAccessPlaceholder() }
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 2: Wristband Status (from SQLite, live-updating)
        // ══════════════════════════════════════════════════════════════════
        item {
            val paired = pairedWristband
            com.faster.festival.ui.components.DeviceCard(
                wristbandName = paired?.deviceName ?: "FASTER Wristband",
                batteryPercentage = paired?.batteryLevel ?: 0,
                connectionStatus = paired?.connectionStatus ?: "Not paired",
                isPaired = paired != null,
                onClick = onNavigateToFaster,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 3: Happening Now (announcements + upcoming_events)
        // ══════════════════════════════════════════════════════════════════
        if (bundle.isModuleEnabled("announcements")) {
            val announcements = bundle.announcements
            if (announcements.isNotEmpty()) {
                item {
                    AnnouncementsSection(
                        title = resolveSectionTitle(bundle, "announcements"),
                        announcements = announcements,
                        onAnnouncementClick = onAnnouncementClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                item {
                    SectionTitle(
                        title = resolveSectionTitle(bundle, "announcements"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item { ModuleEmptyState(label = "announcements") }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (bundle.isModuleEnabled("upcoming_events")) {
            val upcomingEvents = bundle.upcomingEvents
            item {
                SectionTitle(
                    title = resolveSectionTitle(bundle, "upcoming_events"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (upcomingEvents.isNotEmpty()) {
                items(upcomingEvents.size) { index ->
                    UpcomingEventCard(
                        event = upcomingEvents[index],
                        onClick = { onUpcomingEventClick(upcomingEvents[index].id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                item { ModuleEmptyState(label = "upcoming events") }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 4: Safety Messaging (alerts)
        // ══════════════════════════════════════════════════════════════════
        if (bundle.isModuleEnabled("alerts")) {
            val alerts = bundle.alerts
            item {
                SectionTitle(
                    title = resolveSectionTitle(bundle, "alerts"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (alerts.isNotEmpty()) {
                items(alerts.size) { index ->
                    AlertCard(
                        alert = alerts[index],
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                item { ModuleEmptyState(label = "alerts") }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 5: Festival Exploration (hero_carousel + explore tiles)
        // ══════════════════════════════════════════════════════════════════
        if (bundle.isModuleEnabled("hero_carousel")) {
            val heroItems = bundle.heroCarouselItems
            item {
                SectionTitle(
                    title = resolveSectionTitle(bundle, "hero_carousel"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (heroItems.isNotEmpty()) {
                item {
                    HeroGridSection(
                        items = heroItems,
                        onItemClick = { item -> onHeroItemClick(item.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                item { ModuleEmptyState(label = "hero items") }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (tiles.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "${humanizeModuleKey("explore")} ${festival.name}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                ExploreCategoryGrid(
                    tiles = tiles,
                    onNavigateToSchedule = onNavigateToSchedule,
                    onNavigateToMap = onNavigateToMap,
                    onNavigateToFAQ = onNavigateToFAQ
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ══════════════════════════════════════════════════════════════════
        // PRIORITY 6: Sponsors, Promotions, Last set FAQ
        // ══════════════════════════════════════════════════════════════════
        val showSponsorsSection = bundle.isModuleEnabled("sponsors") &&
            bundle.sponsorOffers.isNotEmpty()
        val showPromotionsSection = bundle.isModuleEnabled("promotions") &&
            bundle.promotions.isNotEmpty()

        if (showSponsorsSection || showPromotionsSection) {
            item {
                com.faster.festival.ui.components.SponsorsPromotionsSection(
                    bundle = bundle,
                    sponsorsTitle = resolveSectionTitle(bundle, "sponsors"),
                    promotionsTitle = resolveSectionTitle(bundle, "promotions"),
                    onSponsorClick = onSponsorClick,
                    onPromotionClick = onPromotionClick,
                    onCtaClick = onCtaClick
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        } else {
            // Preserve the pre-existing "module enabled but empty" placeholder
            // behavior for each individual module.
            if (bundle.isModuleEnabled("sponsors")) {
                item {
                    SectionTitle(
                        title = resolveSectionTitle(bundle, "sponsors"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item { ModuleEmptyState(label = "sponsors") }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            if (bundle.isModuleEnabled("promotions")) {
                item {
                    SectionTitle(
                        title = resolveSectionTitle(bundle, "promotions"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item { ModuleEmptyState(label = "promotions") }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        if (bundle.isModuleEnabled("faq")) {
            val faqItems = bundle.faqItems
            item {
                SectionTitle(
                    title = resolveSectionTitle(bundle, "faq"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (faqItems.isNotEmpty()) {
                items(faqItems.size) { index ->
                    FaqExpandableItem(
                        faqItem = faqItems[index],
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToFAQ)
                            .padding(vertical = 22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "See all FAQ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222),
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF222222)
                        )
                    }
                }
            } else {
                item { ModuleEmptyState(label = "FAQ") }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Perks (lower priority) ──
        if (bundle.isModuleEnabled("perks")) {
            val perks = bundle.perks
            item {
                SectionTitle(
                    title = resolveSectionTitle(bundle, "perks"),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (perks.isNotEmpty()) {
                items(perks.size) { index ->
                    PerkCard(
                        perk = perks[index],
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                item { ModuleEmptyState(label = "perks") }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ── Footer ──
        item { FooterSection() }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMERGENCY ACCESS — Placeholder for future persistent CTA
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmergencyAccessPlaceholder() {
    val showComingSoon = com.faster.festival.utils.rememberComingSoonToast(
        "This Feature is coming soon"
    )
    Card(
        onClick = { showComingSoon() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE53935)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Services",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tap for help, medical, or safety assistance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHARED — Section title composable
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. ANNOUNCEMENTS — alert-style items + CTA
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnnouncementsSection(
    title: String,
    announcements: List<Announcement>,
    onAnnouncementClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            announcements.forEach { announcement ->
                AnnouncementAlertItem(
                    announcement = announcement,
                    onClick = { onAnnouncementClick(announcement.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            val showComingSoonAllAnnouncements = com.faster.festival.utils.rememberComingSoonToast()
            Button(
                onClick = { showComingSoonAllAnnouncements() },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralRed,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AnnouncementAlertItem(
    announcement: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFFFF8F00)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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

        announcement.publishedAt?.let { time ->
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFFBDBDBD)
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// 2. HERO GRID — 2×2 grid view of hero_carousel items (replaces banner slider)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HeroGridSection(
    items: List<HeroCarouselItem>,
    onItemClick: (HeroCarouselItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    HeroGridCard(
                        item = item,
                        onClick = { onItemClick(item) },
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

@Composable
private fun HeroGridCard(
    item: HeroCarouselItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A0033),
                                    Color(0xFF0D1B2A)
                                )
                            )
                        )
                )
            }

            // Full-card dark gradient overlay with blur effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.55f to Color.Black.copy(alpha = 0.45f),
                                0.75f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            // Radial vignette for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            radius = 500f
                        )
                    )
            )

            // Text content — bottom-aligned
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                item.subtitle?.let { sub ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = when (item.kind?.lowercase()) {
                        "artist" -> Icons.Default.MusicNote
                        "event" -> Icons.Default.Event
                        else -> Icons.Default.Star
                    },
                    contentDescription = item.kind ?: "Featured",
                    modifier = Modifier.size(22.dp),
                    tint = CoralRed
                )
            }

            // Kind badge — top-right
            item.kind?.let { kind ->
                Text(
                    text = kind.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. EXPLORE CATEGORY GRID — 2×2 image cards from ui_config tiles
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExploreCategoryGrid(
    tiles: List<TileConfig>,
    onNavigateToSchedule: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFAQ: () -> Unit
) {
    val showComingSoon = com.faster.festival.utils.rememberComingSoonToast(
        "This feature is coming soon"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTiles.forEach { tile ->
                    val label = resolveTileLabel(tile)
                    val isLineupSchedule = tile.key == "lineup_schedule" ||
                        label.equals("Lineup Schedule", ignoreCase = true)
                    val isEventSafety = tile.key == "event_safety" ||
                        tile.key == "festival_experience" ||
                        label.equals("Event Safety", ignoreCase = true)
                    val onClick: () -> Unit = when {
                        isLineupSchedule -> showComingSoon
                        isEventSafety -> showComingSoon
                        tile.key == "faq" -> onNavigateToFAQ
                        else -> { {} }
                    }
                    ExploreTileCard(
                        label = label,
                        description = resolveTileDescription(tile),
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExploreTileCard(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.party),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(20.dp)
            )
        }
    }
}

// Sponsors & Promotions sections are rendered via the reusable
// com.faster.festival.ui.components.SponsorsPromotionsSection widget,
// which is shared with MapScreen.

// ═══════════════════════════════════════════════════════════════════════════════
// 6. FAQ — expandable accordion items
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FaqExpandableItem(
    faqItem: FaqItem,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "faq_arrow_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faqItem.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = faqItem.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODULE EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModuleEmptyState(
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No $label found",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF999999)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UPCOMING EVENTS
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun UpcomingEventCard(
    event: UpcomingEvent,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date/time badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoralRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CoralRed
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                event.venue?.let { venue ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CoralRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = venue.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = CoralRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatEventDateTime(event.startsAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666)
                )
            }

            // Forward chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color(0xFF999999)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatEventDateTime(isoTime: String): String {
    return try {
        val instant = Instant.parse(isoTime)
        val zdt = instant.atZone(ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
        zdt.format(formatter)
    } catch (e: Exception) {
        isoTime.substringBefore("T")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PERKS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PerkCard(
    perk: PerkItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Perk icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFFF8F00)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = perk.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                perk.description?.let { desc ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            perk.ctaLabel?.let { label ->
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF999999)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALERTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AlertCard(
    alert: AlertItem,
    modifier: Modifier = Modifier
) {
    val bgColor = when (alert.severity) {
        "critical" -> Color(0xFFFFEBEE)
        "warning" -> Color(0xFFFFF3E0)
        else -> Color(0xFFE3F2FD)
    }
    val iconTint = when (alert.severity) {
        "critical" -> Color(0xFFD32F2F)
        "warning" -> Color(0xFFFF8F00)
        else -> Color(0xFF1976D2)
    }
    val textColor = when (alert.severity) {
        "critical" -> Color(0xFFC62828)
        "warning" -> Color(0xFFE65100)
        else -> Color(0xFF1565C0)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                alert.body?.let { body ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. FOOTER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FooterSection() {
    Text(
        text = "\u00A9 ${humanizeModuleKey("faster_events")}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP APP BAR (PRESERVED — do not change)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FasterTopAppBar(
    logoUrl: String? = null,
    isConnected: Boolean = false,
    onSearchClick: () -> Unit = {},
    onWristbandClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .statusBarsPadding()
            .height(44.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            if (!logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "F",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(50))
                .clickable(onClick = onWristbandClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF333333)
            )
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = "Connection status",
                modifier = Modifier.size(12.dp),
                tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
            )
            Text(
                text = if (isConnected) "Connected" else "Not Connected",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (isConnected) Color(0xFF222222) else Color(0xFF888888)
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E8E8))
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF555555)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CINEMATIC KEN BURNS BANNER SLIDER (PRESERVED — do not change)
// ═══════════════════════════════════════════════════════════════════════════════

private const val BANNER_AUTO_SCROLL_MS = 4500L
private const val KEN_BURNS_DURATION_MS = 8000
private const val BANNER_HEIGHT_DP = 320

private data class KenBurnsParams(
    val startScale: Float,
    val endScale: Float,
    val startTranslateX: Float,
    val endTranslateX: Float,
    val startTranslateY: Float,
    val endTranslateY: Float
)

private val kenBurnsVariants = listOf(
    KenBurnsParams(1.0f, 1.18f, 0f, -30f, 0f, -15f),
    KenBurnsParams(1.15f, 1.0f, -20f, 20f, -10f, 10f),
    KenBurnsParams(1.0f, 1.2f, 15f, -15f, 10f, -10f),
    KenBurnsParams(1.12f, 1.0f, 20f, 0f, -5f, 5f),
)

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun FestivalBannerHeader(
    festival: AppFestivalHeader,
    modifier: Modifier = Modifier
) {
    val bannerImages = festival.bannerUrls.ifEmpty {
        listOfNotNull(festival.bannerUrl)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT_DP.dp)
    ) {
        if (bannerImages.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { bannerImages.size })
            val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

            if (bannerImages.size > 1) {
                LaunchedEffect(pagerState, isDragged) {
                    if (!isDragged) {
                        while (true) {
                            delay(BANNER_AUTO_SCROLL_MS)
                            val next = (pagerState.currentPage + 1) % bannerImages.size
                            pagerState.animateScrollToPage(
                                page = next,
                                animationSpec = tween(
                                    durationMillis = 800,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                }
            }

            var settledPage by remember { mutableIntStateOf(0) }
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect { settledPage = it }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                CinematicBannerSlide(
                    imageUrl = bannerImages[page],
                    contentDescription = "${festival.name} ${page + 1}",
                    kenBurnsParams = kenBurnsVariants[page % kenBurnsVariants.size],
                    isCurrentPage = pagerState.settledPage == page
                )
            }

            // Dark gradient overlay (15% -> 92% black top-to-bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.15f),
                                0.3f to Color.Black.copy(alpha = 0.25f),
                                0.55f to Color.Black.copy(alpha = 0.45f),
                                0.75f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            BannerTextOverlay(
                festivalName = festival.name,
                timezone = festival.timezone,
                startsAt = festival.startsAt,
                dateRange = formatDateRange(festival.startsAt, festival.endsAt, festival.timezone),
                location = festival.location,
                settledPage = settledPage,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 24.dp, end = 80.dp)
            )

            if (bannerImages.size > 1) {
                BannerProgressIndicators(
                    pagerState = pagerState,
                    pageCount = bannerImages.size,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A0033),
                                Color(0xFF0D1B2A),
                                Color(0xFF1B2838)
                            )
                        )
                    )
            )
            // Dark gradient overlay (15% -> 92%)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.15f),
                                0.3f to Color.Black.copy(alpha = 0.25f),
                                0.55f to Color.Black.copy(alpha = 0.45f),
                                0.75f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 24.dp, end = 20.dp)
            ) {
                Text(
                    text = festival.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateRange(festival.startsAt, festival.endsAt, festival.timezone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                festival.location?.let { loc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CinematicBannerSlide(
    imageUrl: String,
    contentDescription: String,
    kenBurnsParams: KenBurnsParams,
    isCurrentPage: Boolean
) {
    val scale = remember { Animatable(kenBurnsParams.startScale) }
    val translateX = remember { Animatable(kenBurnsParams.startTranslateX) }
    val translateY = remember { Animatable(kenBurnsParams.startTranslateY) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            scale.snapTo(kenBurnsParams.startScale)
            translateX.snapTo(kenBurnsParams.startTranslateX)
            translateY.snapTo(kenBurnsParams.startTranslateY)

            val spec = tween<Float>(
                durationMillis = KEN_BURNS_DURATION_MS,
                easing = EaseInOut
            )
            kotlinx.coroutines.coroutineScope {
                launch { scale.animateTo(kenBurnsParams.endScale, spec) }
                launch { translateX.animateTo(kenBurnsParams.endTranslateX, spec) }
                launch { translateY.animateTo(kenBurnsParams.endTranslateY, spec) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = translateX.value
                    translationY = translateY.value
                },
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun BannerTextOverlay(
    festivalName: String,
    timezone: String = "",
    startsAt: String = "",
    dateRange: String,
    location: String? = null,
    settledPage: Int,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(settledPage) {
        visible = false
        delay(100)
        visible = true
    }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(200))
        ) {
            Column {
                Text(
                    text = festivalName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 36.sp
                )
                if (timezone.isNotEmpty() || startsAt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val compactDate = if (startsAt.isNotEmpty())
                        com.faster.festival.utils.DateFormatter.formatDateCompact(startsAt)
                    else ""
                    val timezoneDate = when {
                        timezone.isNotEmpty() && compactDate.isNotEmpty() -> "$timezone  ·  $compactDate"
                        timezone.isNotEmpty() -> timezone
                        else -> compactDate
                    }
                    Text(
                        text = timezoneDate,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f),
                    letterSpacing = 0.3.sp
                )
                location?.let { loc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerProgressIndicators(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    val currentPage by remember { derivedStateOf { pagerState.settledPage } }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = currentPage == index

            if (isActive) {
                val progress = remember { Animatable(0f) }
                LaunchedEffect(currentPage) {
                    progress.snapTo(0f)
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BANNER_AUTO_SCROLL_MS.toInt(),
                            easing = LinearEasing
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateRange(startsAt: String, endsAt: String, timezone: String): String {
    return try {
        val zoneId = ZoneId.of(timezone)
        val startZoned = Instant.parse(startsAt).atZone(zoneId)
        val endZoned = Instant.parse(endsAt).atZone(zoneId)
        val startDay = startZoned.dayOfMonth
        val endDay = endZoned.dayOfMonth
        val year = endZoned.year
        if (startZoned.month == endZoned.month) {
            "${startZoned.month} $startDay - $endDay, $year"
        } else {
            "${startZoned.month.toString().take(3)} $startDay - ${endZoned.month.toString().take(3)} $endDay, $year"
        }
    } catch (e: Exception) {
        ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
private fun formatEventTime(startsAt: String): String {
    return try {
        val instant = Instant.parse(startsAt)
        val zoned = instant.atZone(ZoneId.systemDefault())
        val hour = zoned.hour
        val minute = zoned.minute
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        if (minute == 0) "$displayHour $amPm" else "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
    } catch (e: Exception) {
        ""
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun FasterTopAppBarPreview() {
    MaterialTheme { FasterTopAppBar() }
}

@Preview(showBackground = true)
@Composable
private fun AnnouncementsSectionPreview() {
    MaterialTheme {
        AnnouncementsSection(
            title = "Announcements",
            announcements = listOf(
                Announcement(
                    id = "1",
                    title = "Free Popcorn",
                    content = "Come and grab the Popcorn",
                    publishedAt = null,
                    order = 0
                )
            ),
            onAnnouncementClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeroGridCardPreview() {
    MaterialTheme {
        HeroGridCard(
            item = HeroCarouselItem(
                id = "1",
                kind = "artist",
                title = "Larkin Poe",
                subtitle = "Saturday headliner at Main Stage",
                ctaLabel = "View artist"
            ),
            onClick = {},
            modifier = Modifier
                .width(180.dp)
                .height(220.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionCardPreview() {
    MaterialTheme {
        com.faster.festival.ui.components.PromotionCard(
            promotion = PromotionItem(
                id = "1",
                title = "Exclusive Promotion",
                offerText = "25% Off Draft Beer",
                description = "Details about the deal",
                subtitle = "Restaurant"
            ),
            modifier = Modifier.width(300.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SponsorOfferCardPreview() {
    MaterialTheme {
        com.faster.festival.ui.components.SponsorCard(
            sponsor = SponsorOffer(
                id = "1",
                sponsorName = "Culinary Commons",
                offerText = "15% Draft Beer",
                subtitle = "15% Draft Beer 2:30 pm",
                ctaLabel = "Directions"
            ),
            modifier = Modifier.width(280.dp)
        )
    }
}
