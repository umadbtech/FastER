package com.faster.festival.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.faster.festival.data.models.Announcement
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val CoralRed = Color(0xFFE53935)
private val AnnouncementAmber = Color(0xFFFF8F00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    announcement: Announcement,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Announcement",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero image
            if (!announcement.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = announcement.imageUrl,
                    contentDescription = announcement.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Priority badge + timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (announcement.priority > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (announcement.priority >= 2) CoralRed else AnnouncementAmber
                            )
                            Text(
                                text = if (announcement.priority >= 2) "Urgent" else "Important",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (announcement.priority >= 2) CoralRed else AnnouncementAmber
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    val displayTime = formatAnnouncementDate(announcement.publishedAt)
                    if (displayTime != null) {
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Date range (starts_at / ends_at)
                val dateRange = formatDateRange(announcement.startsAt, announcement.endsAt)
                if (dateRange != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Content body
                if (!announcement.content.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = announcement.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatAnnouncementDate(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoDate) ?: return isoDate
        SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US).format(date)
    } catch (_: Exception) {
        isoDate
    }
}

private fun formatDateRange(startsAt: String?, endsAt: String?): String? {
    if (startsAt.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val displayFmt = SimpleDateFormat("MMM dd, h:mm a", Locale.US)

        val start = parser.parse(startsAt)?.let { displayFmt.format(it) } ?: return null
        val end = endsAt?.let { parser.parse(it) }?.let { displayFmt.format(it) }

        if (end != null) "$start - $end" else "Starts $start"
    } catch (_: Exception) {
        null
    }
}
