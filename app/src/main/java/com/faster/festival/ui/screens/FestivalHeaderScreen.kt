package com.faster.festival.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.faster.festival.data.models.FestivalHeader
import com.faster.festival.data.repository.AppHomeRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.viewmodel.AppHomeViewModel
import com.faster.festival.ui.viewmodel.FestivalHeaderUiState
import com.faster.festival.utils.DateFormatter

/** Festival Header Screen - Main entry point */
@Composable
fun FestivalHeaderScreen(
        modifier: Modifier = Modifier,
        festivalSlug: String,
        accessToken: String? = null,
        viewModel: AppHomeViewModel =
                viewModel(
                        factory =
                                AppHomeViewModel.createFactory(
                                        appHomeRepository =
                                                AppHomeRepository(
                                                        NetworkModule.appHomeApi,
                                                        NetworkModule.festivalApiService
                                                ),
                                        festivalSlug = festivalSlug
                                )
                )
) {
    val uiState by viewModel.festivalHeaderState.collectAsState()

    // Load data on first composition or when slug changes
    LaunchedEffect(festivalSlug) { viewModel.loadFestivalHeader(accessToken) }

    FestivalHeaderContent(
            uiState = uiState,
            onRetry = { viewModel.retryFestivalHeader() },
            modifier = modifier
    )
}

/** Main content composable that handles all UI states */
@Composable
fun FestivalHeaderContent(
        uiState: FestivalHeaderUiState,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
) {
    when (uiState) {
        is FestivalHeaderUiState.Loading -> {
            FestivalHeaderShimmer(modifier = modifier)
        }
        is FestivalHeaderUiState.Success -> {
            FestivalHeaderBanner(festival = uiState.header, modifier = modifier)
        }
        is FestivalHeaderUiState.Error -> {
            FestivalHeaderError(message = uiState.message, onRetry = onRetry, modifier = modifier)
        }
    }
}

/** Success state: Full festival header banner with image, gradient, and text overlay */
@Composable
fun FestivalHeaderBanner(festival: FestivalHeader, modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp))
    ) {
        // Banner background image
        AsyncImage(
                model = festival.bannerUrl,
                contentDescription = "Festival banner - ${festival.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    // Fallback to solid color on image load error
                }
        )

        // Dark gradient overlay to ensure text readability
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.Black.copy(
                                                                                alpha = 0.3f
                                                                        ),
                                                                        Color.Black.copy(
                                                                                alpha = 0.6f
                                                                        )
                                                                ),
                                                        startY = 0f,
                                                        endY = Float.POSITIVE_INFINITY
                                                )
                                )
        )

        // Content overlay with text and logo
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
        ) {
            // Top section with circular logo avatar
            if (festival.logoUrl.isNotEmpty()) {
                AsyncImage(
                        model = festival.logoUrl,
                        contentDescription = "Festival logo - ${festival.name}",
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                )
            }

            // Bottom section with festival details
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                // Festival name
                Text(
                        text = festival.name,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Timezone + starts_at date (e.g., "America/New_York · 03-MAR-2026")
                val formattedStart = DateFormatter.formatDateCompact(festival.startsAt)
                Text(
                        text = if (formattedStart.isNotEmpty())
                            "${festival.timezone}  ·  $formattedStart"
                        else
                            festival.timezone,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date range formatted
                Text(
                        text =
                                DateFormatter.formatDateRange(
                                        festival.startsAt,
                                        festival.endsAt,
                                        festival.timezone
                                ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

/** Loading state: Shimmer skeleton placeholder */
@Composable
fun FestivalHeaderShimmer(modifier: Modifier = Modifier) {
    val shimmerAlpha = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        shimmerAlpha.animateTo(
                targetValue = 1f,
                animationSpec =
                        infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                        )
        )
    }

    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp))
                            .background(Color.LightGray.copy(alpha = shimmerAlpha.value))
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo placeholder (circle)
            Box(
                    modifier =
                            Modifier.size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = shimmerAlpha.value))
            )

            // Text placeholders
            Column {
                Box(
                        modifier =
                                Modifier.fillMaxWidth(0.7f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = shimmerAlpha.value))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                        modifier =
                                Modifier.fillMaxWidth(0.5f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Gray.copy(alpha = shimmerAlpha.value))
                )
            }
        }
    }
}

/** Error state: Error message with retry button */
@Composable
fun FestivalHeaderError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(bottomEnd = 16.dp, bottomStart = 16.dp))
                            .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
    ) {
        Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = "Oops! Could not load festival",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = message,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                    onClick = onRetry,
                    modifier = Modifier.height(44.dp).fillMaxWidth(),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                            )
            ) {
                Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", color = Color.White)
            }
        }
    }
}
