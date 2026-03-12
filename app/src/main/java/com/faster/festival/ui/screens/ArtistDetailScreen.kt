package com.faster.festival.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

// Artist detail data
data class ArtistDetailData(
    val id: String,
    val name: String,
    val bio: String?,
    val imageUrl: String?,
    val coverImageUrl: String?,
    val genres: List<String>,
    val origin: String?,
    val performances: List<ArtistPerformance>
)

data class ArtistPerformance(
    val id: String,
    val stageName: String,
    val startTime: String,
    val endTime: String,
    val day: Int,
    val description: String?
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
                    val performances = body.performances.map { perf ->
                        ArtistPerformance(
                            id = perf.id,
                            stageName = perf.stage_name,
                            startTime = perf.start_time,
                            endTime = perf.end_time,
                            day = perf.day,
                            description = perf.description
                        )
                    }
                    _uiState.value = ArtistDetailUiState.Success(
                        ArtistDetailData(
                            id = artist.id,
                            name = artist.name,
                            bio = artist.bio,
                            imageUrl = artist.image_url,
                            coverImageUrl = artist.cover_image_url,
                            genres = artist.genres ?: emptyList(),
                            origin = artist.origin,
                            performances = performances
                        )
                    )
                    return@launch
                }

                // Fallback: try lineup API to find artist
                val lineupResponse = contentLineupApi.getContentLineup(festivalSlug)
                if (lineupResponse.isSuccessful && lineupResponse.body() != null) {
                    val lineupBody = lineupResponse.body()!!
                    val foundArtist = lineupBody.featured_artists.find {
                        it.id == artistId || it.name.equals(artistId, ignoreCase = true)
                    }
                    if (foundArtist != null) {
                        // Get schedule info
                        val schedulePerformances = mutableListOf<ArtistPerformance>()
                        val scheduleResponse = contentStageScheduleApi.getStageSchedule(festivalSlug)
                        if (scheduleResponse.isSuccessful && scheduleResponse.body() != null) {
                            scheduleResponse.body()!!.stages.forEach { stage ->
                                stage.schedule
                                    .filter { it.artist_id == foundArtist.id }
                                    .forEach { slot ->
                                        schedulePerformances.add(
                                            ArtistPerformance(
                                                id = slot.id,
                                                stageName = stage.name,
                                                startTime = slot.start_time,
                                                endTime = slot.end_time,
                                                day = slot.day,
                                                description = null
                                            )
                                        )
                                    }
                            }
                        }

                        _uiState.value = ArtistDetailUiState.Success(
                            ArtistDetailData(
                                id = foundArtist.id,
                                name = foundArtist.name,
                                bio = foundArtist.bio,
                                imageUrl = foundArtist.image_url,
                                coverImageUrl = null,
                                genres = foundArtist.genres ?: emptyList(),
                                origin = null,
                                performances = schedulePerformances
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        val artistName = when (val state = uiState) {
                            is ArtistDetailUiState.Success -> state.artist.name
                            else -> "this artist"
                        }
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Check out $artistName at the festival!"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share artist"))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
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

@Composable
private fun ArtistDetailShimmer() {
    val transition = rememberInfiniteTransition(label = "artist_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artist_shimmer_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = alpha))
        )
    }
}

@Composable
private fun ArtistDetailError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to load artist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun ArtistDetailContent(artist: ArtistDetailData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
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
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Artist name overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (artist.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist.genres.joinToString(" / "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    if (artist.origin != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artist.origin,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Bio section
        if (artist.bio != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Performances section
        if (artist.performances.isNotEmpty()) {
            item {
                Text(
                    text = "Performances",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
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

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun PerformanceCard(
    performance: ArtistPerformance,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${performance.day}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = performance.stageName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${performance.startTime} - ${performance.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (performance.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = performance.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
