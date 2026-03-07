package com.faster.festival.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Automatic banner carousel slider with dot indicators
 * Auto-rotates through multiple banner images
 *
 * @param bannerUrls List of banner image URLs to display
 * @param modifier Modifier for the component
 * @param autoScrollInterval Time in milliseconds between slides (default: 1000ms = 1 second)
 * @param fallbackColor Color to use if no banners available
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSlider(
    bannerUrls: List<String>,
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = 1000L,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    // If no banners, show placeholder
    if (bannerUrls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(fallbackColor)
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { bannerUrls.size })

    // Auto-scroll effect with proper dependencies
    LaunchedEffect(pagerState.currentPage, bannerUrls.size, autoScrollInterval) {
        if (bannerUrls.size > 1) {
            delay(autoScrollInterval)
            val nextPage = (pagerState.currentPage + 1) % bannerUrls.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    // Main container with fixed height
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Pager for banner images
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { pageIndex ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Banner image
                AsyncImage(
                    model = bannerUrls[pageIndex],
                    contentDescription = "Festival banner ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
            }
        }

        // Dot indicators at bottom (only show if more than 1 banner)
        if (bannerUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(bannerUrls.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index == pagerState.currentPage)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
