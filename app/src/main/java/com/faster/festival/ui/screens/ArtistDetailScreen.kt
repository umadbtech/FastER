package com.faster.festival.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.AppConfig
import com.faster.festival.data.remote.ContentArtistDetailApi
import com.faster.festival.data.remote.ContentLineupApi
import com.faster.festival.data.remote.ContentStageScheduleApi
import com.faster.festival.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Light theme palette (consistent with HomeScreen)
private val DetailBg = Color(0xFFF7F7F7)
private val DetailWhite = Color.White
private val DetailCoralRed = Color(0xFFE53935)
private val DetailDarkNavy = Color(0xFF0D1B2A)
private val DetailTextDark = Color(0xFF222222)
private val DetailTextMedium = Color(0xFF333333)
private val DetailTextLight = Color(0xFF666666)
private val DetailBorderLight = Color(0xFFE0E0E0)
private val DetailCardBg = Color(0xFFF5F5F5)

// ─────────────────────────────────────────────────────────────────────
// Data & ViewModel
// ─────────────────────────────────────────────────────────────────────

data class ArtistDetailData(
    val id: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val coverImageUrl: String?,
    val genres: List<String>,
    val origin: String?,
    val foundedYear: Int?,
    val memberCount: Int?,
    val performances: List<ArtistPerformance>,
    val media: ArtistMediaData?
)

data class ArtistPerformance(
    val id: String,
    val stageName: String,
    val startTime: String,
    val endTime: String,
    val day: Int,
    val description: String?
)

data class ArtistMediaData(
    val website: String?,
    val spotify: String?,
    val instagram: String?,
    val twitter: String?,
    val youtube: String?
)

sealed class ArtistDetailUiState {
    object Loading : ArtistDetailUiState()
    data class Success(val artist: ArtistDetailData) : ArtistDetailUiState()
    data class Error(val message: String) : ArtistDetailUiState()
}

class ArtistDetailScreenViewModel(
    private val contentArtistDetailApi: ContentArtistDetailApi,
    private val contentLineupApi: ContentLineupApi,
    private val contentStageScheduleApi: ContentStageScheduleApi,
    private val festivalSlug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    fun loadArtist(artistId: String) {
        viewModelScope.launch {
            _uiState.value = ArtistDetailUiState.Loading
            try {
                // Try artist detail API first
                val detailResponse = contentArtistDetailApi.getArtistDetail(
                    festivalSlug = festivalSlug,
                    artistSlug = artistId
                )
                if (detailResponse.isSuccessful && detailResponse.body() != null) {
                    val body = detailResponse.body()!!
                    val artist = body.artist
                    val performances = body.events.map { event ->
                        ArtistPerformance(
                            id = event.id,
                            stageName = event.stageName,
                            startTime = event.startTime,
                            endTime = event.endTime,
                            day = event.day,
                            description = event.description
                        )
                    }
                    val mediaData = body.media?.let { m ->
                        ArtistMediaData(
                            website = m.website,
                            spotify = m.spotify,
                            instagram = m.instagram,
                            twitter = m.twitter,
                            youtube = m.youtube
                        )
                    }
                    _uiState.value = ArtistDetailUiState.Success(
                        ArtistDetailData(
                            id = artist.id,
                            name = artist.name,
                            bio = artist.bio,
                            imageUrl = artist.imageUrl,
                            coverImageUrl = artist.coverImageUrl,
                            genres = artist.genres ?: emptyList(),
                            origin = artist.origin,
                            foundedYear = artist.foundedYear,
                            memberCount = artist.memberCount,
                            performances = performances,
                            media = mediaData
                        )
                    )
                    return@launch
                }

                // Fallback: try lineup API to find artist
                val lineupResponse = contentLineupApi.getContentLineup(festivalSlug)
                if (lineupResponse.isSuccessful && lineupResponse.body() != null) {
                    val lineupBody = lineupResponse.body()!!
                    val foundArtist = lineupBody.artists.find {
                        it.id == artistId || it.slug == artistId ||
                                it.name.equals(artistId, ignoreCase = true)
                    }
                    if (foundArtist != null) {
                        val schedulePerformances = mutableListOf<ArtistPerformance>()
                        val scheduleResponse = contentStageScheduleApi.getStageSchedule(festivalSlug)
                        if (scheduleResponse.isSuccessful && scheduleResponse.body() != null) {
                            scheduleResponse.body()!!.events
                                .filter { it.artistId == foundArtist.id }
                                .forEach { event ->
                                    schedulePerformances.add(
                                        ArtistPerformance(
                                            id = event.id,
                                            stageName = event.stageName,
                                            startTime = event.startTime,
                                            endTime = event.endTime,
                                            day = event.day,
                                            description = null
                                        )
                                    )
                                }
                        }

                        _uiState.value = ArtistDetailUiState.Success(
                            ArtistDetailData(
                                id = foundArtist.id,
                                name = foundArtist.name,
                                bio = foundArtist.bio,
                                imageUrl = foundArtist.imageUrl,
                                coverImageUrl = null,
                                genres = foundArtist.genres ?: emptyList(),
                                origin = null,
                                foundedYear = null,
                                memberCount = null,
                                performances = schedulePerformances,
                                media = null
                            )
                        )
                        return@launch
                    }
                }

                _uiState.value = ArtistDetailUiState.Error("Artist not found")
            } catch (e: Exception) {
                _uiState.value = ArtistDetailUiState.Error(
                    e.localizedMessage ?: "Failed to load artist"
                )
            }
        }
    }

    class Factory(
        private val contentArtistDetailApi: ContentArtistDetailApi,
        private val contentLineupApi: ContentLineupApi,
        private val contentStageScheduleApi: ContentStageScheduleApi,
        private val festivalSlug: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ArtistDetailScreenViewModel(
                contentArtistDetailApi,
                contentLineupApi,
                contentStageScheduleApi,
                festivalSlug
            ) as T
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Screen Composable
// ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBackClick: () -> Unit,
    festivalSlug: String = AppConfig.DEFAULT_FESTIVAL_SLUG
) {
    val viewModel: ArtistDetailScreenViewModel = viewModel(
        factory = ArtistDetailScreenViewModel.Factory(
            contentArtistDetailApi = NetworkModule.contentArtistDetailApi,
            contentLineupApi = NetworkModule.contentLineupApi,
            contentStageScheduleApi = NetworkModule.contentStageScheduleApi,
            festivalSlug = festivalSlug
        )
    )

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBg)
    ) {
        // Top bar
        TopAppBar(
            title = {
                val title = when (val state = uiState) {
                    is ArtistDetailUiState.Success -> state.artist.name
                    else -> "Artist"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DetailTextDark
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DetailTextDark
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        val artistName = when (val state = uiState) {
                            is ArtistDetailUiState.Success -> state.artist.name
                            else -> "this artist"
                        }
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out $artistName at the festival!"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share artist"))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = DetailTextDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DetailBg
            )
        )

        when (val state = uiState) {
            is ArtistDetailUiState.Loading -> {
                ArtistDetailShimmer()
            }
            is ArtistDetailUiState.Error -> {
                ArtistDetailError(
                    message = state.message,
                    onRetry = { viewModel.loadArtist(artistId) }
                )
            }
            is ArtistDetailUiState.Success -> {
                ArtistDetailContent(artist = state.artist)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Loading Shimmer
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistDetailShimmer() {
    val transition = rememberInfiniteTransition(label = "artist_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artist_shimmer_alpha"
    )

    val shimmerColor = Color.Gray.copy(alpha = alpha)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(shimmerColor)
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(shimmerColor)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Error State
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistDetailError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Error",
                modifier = Modifier.size(40.dp),
                tint = DetailCoralRed
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Failed to load artist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DetailTextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = DetailTextLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = DetailCoralRed),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Success Content
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistDetailContent(artist: ArtistDetailData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Hero Image ──────────────────────────────────────────────
        item {
            ArtistHeroHeader(artist = artist)
        }

        // ── Quick Info Pills ────────────────────────────────────────
        item {
            ArtistQuickInfo(artist = artist)
        }

        // ── Bio Section ─────────────────────────────────────────────
        if (!artist.bio.isNullOrBlank()) {
            item {
                ArtistBioSection(bio = artist.bio)
            }
        }

        // ── Social / Media Links ────────────────────────────────────
        if (artist.media != null) {
            item {
                ArtistSocialLinks(media = artist.media)
            }
        }

        // ── Performances Section ────────────────────────────────────
        if (artist.performances.isNotEmpty()) {
            item {
                Text(
                    text = "Performances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DetailTextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(
                items = artist.performances,
                key = { it.id }
            ) { performance ->
                PerformanceCard(
                    performance = performance,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Hero Header
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistHeroHeader(artist: ArtistDetailData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        val heroUrl = artist.coverImageUrl ?: artist.imageUrl
        if (heroUrl != null) {
            AsyncImage(
                model = heroUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A0033),
                                DetailDarkNavy
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = DetailTextLight,
                    modifier = Modifier.size(96.dp)
                )
            }
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

        // Artist name + genres overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 36.sp
            )
            if (artist.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    artist.genres.take(3).forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            if (artist.origin != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = artist.origin,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Quick Info (founded year, member count)
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistQuickInfo(artist: ArtistDetailData) {
    val infoItems = mutableListOf<Pair<ImageVector, String>>()

    if (artist.foundedYear != null) {
        infoItems.add(Icons.Default.CalendarMonth to "Est. ${artist.foundedYear}")
    }
    if (artist.memberCount != null && artist.memberCount > 0) {
        val memberText = if (artist.memberCount == 1) "Solo artist" else "${artist.memberCount} members"
        infoItems.add(Icons.Default.Groups to memberText)
    }
    if (artist.performances.isNotEmpty()) {
        val stageCount = artist.performances.map { it.stageName }.distinct().size
        val perfText = "${artist.performances.size} show${if (artist.performances.size > 1) "s" else ""}" +
                if (stageCount > 1) " / $stageCount stages" else ""
        infoItems.add(Icons.Default.MusicNote to perfText)
    }

    if (infoItems.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        infoItems.forEach { (icon, label) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = DetailWhite
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DetailCoralRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = DetailTextMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Bio Section
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtistBioSection(bio: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DetailTextDark
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = DetailWhite)
        ) {
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = DetailTextMedium,
                modifier = Modifier.padding(16.dp),
                lineHeight = 22.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Social Links
// ─────────────────────────────────────────────────────────────────────

private val SpotifyGreen = Color(0xFF1DB954)

@Composable
private fun ArtistSocialLinks(media: ArtistMediaData) {
    val context = LocalContext.current

    data class SocialLink(
        val name: String,
        val url: String,
        val icon: ImageVector,
        val tint: Color,
        val bgColor: Color
    )

    val links = mutableListOf<SocialLink>()

    // Spotify first — prominent placement
    media.spotify?.let {
        links.add(SocialLink("Spotify", it, Icons.Default.PlayArrow, Color.White, SpotifyGreen))
    }
    media.website?.let {
        links.add(SocialLink("Website", it, Icons.Default.Language, DetailCoralRed, DetailWhite))
    }
    media.instagram?.let {
        links.add(SocialLink("Instagram", it, Icons.Default.PhotoCamera, Color(0xFFE1306C), DetailWhite))
    }
    media.twitter?.let {
        links.add(SocialLink("Twitter", it, Icons.Default.Language, Color(0xFF1DA1F2), DetailWhite))
    }
    media.youtube?.let {
        links.add(SocialLink("YouTube", it, Icons.Default.OndemandVideo, Color(0xFFFF0000), DetailWhite))
    }

    if (links.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Links",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DetailTextDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Spotify gets a full-width prominent button when available
        media.spotify?.let { spotifyUrl ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl)))
                    },
                shape = RoundedCornerShape(12.dp),
                color = SpotifyGreen
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listen on Spotify",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Other links as smaller chips
        val otherLinks = links.filter { it.name != "Spotify" }
        if (otherLinks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                otherLinks.forEach { link ->
                    Surface(
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = link.bgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = link.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = link.tint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = link.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = DetailTextDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Performance Card
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun PerformanceCard(
    performance: ArtistPerformance,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = DetailWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DetailCoralRed.copy(alpha = 0.15f),
                                DetailCoralRed.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = DetailCoralRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${performance.day}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DetailCoralRed
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = DetailCoralRed
                    )
                    Text(
                        text = performance.stageName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailTextDark
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = DetailTextLight
                    )
                    Text(
                        text = "${performance.startTime} - ${performance.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DetailTextMedium
                    )
                }
                if (performance.description != null) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = performance.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = DetailTextLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
