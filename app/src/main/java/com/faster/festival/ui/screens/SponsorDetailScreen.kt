package com.faster.festival.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.faster.festival.data.models.SponsorOffer

// Light theme palette (consistent with HomeScreen)
private val SponsorBg = Color(0xFFF7F7F7)
private val SponsorWhite = Color.White
private val SponsorCoralRed = Color(0xFFE53935)
private val SponsorTextDark = Color(0xFF222222)
private val SponsorTextMedium = Color(0xFF333333)
private val SponsorTextLight = Color(0xFF666666)
private val SponsorBorderLight = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorDetailScreen(
    sponsor: SponsorOffer,
    onBackClick: () -> Unit = {},
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = sponsor.sponsorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SponsorTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${sponsor.sponsorName} - ${sponsor.offerText ?: sponsor.title ?: ""}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = SponsorTextDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SponsorTextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SponsorWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero image
            val heroImage = sponsor.primaryMediaUrl ?: sponsor.sponsorLogoUrl
            if (heroImage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        AsyncImage(
                            model = heroImage,
                            contentDescription = sponsor.sponsorName,
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
                                            0.6f to Color.Black.copy(alpha = 0.15f),
                                            1.0f to Color.Black.copy(alpha = 0.55f)
                                        )
                                    )
                                )
                        )
                        // Sponsor logo overlay (bottom-left)
                        sponsor.sponsorLogoUrl?.let { logo ->
                            if (sponsor.primaryMediaUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    AsyncImage(
                                        model = logo,
                                        contentDescription = "${sponsor.sponsorName} logo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Header: Name, subtitle, tags
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SponsorWhite)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = sponsor.sponsorName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SponsorTextDark
                    )

                    sponsor.subtitle?.let { sub ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SponsorTextLight
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sponsor.isExclusive) {
                            SponsorTagChip(text = "EXCLUSIVE", isHighlighted = true)
                        }
                        SponsorTagChip(text = "SPONSOR", isHighlighted = false)
                    }
                }
            }

            // Image gallery (if multiple media)
            if (sponsor.mediaUrls.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        items(sponsor.mediaUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Sponsor image",
                                modifier = Modifier
                                    .size(width = 160.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Supabase-driven CTA: text from `cta_label`, click opens
            // InAppWebViewScreen with `cta_url`. Hidden when no URL is provided.
            val ctaUrl = sponsor.ctaUrl?.takeIf { it.isNotBlank() }
            if (ctaUrl != null) {
                val ctaLabel = sponsor.ctaLabel?.takeIf { it.isNotBlank() } ?: "Open"
                val ctaTitle = sponsor.title ?: sponsor.sponsorName
                item {
                    Button(
                        onClick = { onCtaClick(ctaUrl, ctaTitle) },
                        colors = ButtonDefaults.buttonColors(containerColor = SponsorCoralRed),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(ctaLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Offer Card
            val hasOffer = !sponsor.offerText.isNullOrBlank()
            if (hasOffer) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (sponsor.isExclusive) "Exclusive Offer" else "Sponsor Offer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SponsorTextDark,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0088FF)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF0088FF),
                                                Color(0xFF0055CC)
                                            )
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = sponsor.offerText ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                sponsor.title?.let { title ->
                                    if (title != sponsor.offerText) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                val locationLine = listOfNotNull(
                                    sponsor.vendorName,
                                    sponsor.locationText
                                ).joinToString(", ")
                                if (locationLine.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = locationLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            val aboutText = sponsor.description ?: sponsor.subtitle
            if (!aboutText.isNullOrBlank()) {
                item {
                    SponsorDetailSection(title = "About") {
                        Text(
                            text = aboutText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SponsorTextMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Hours Section
            sponsor.hoursText?.let { hours ->
                item {
                    SponsorDetailSection(title = "Hours") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Every Day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SponsorTextMedium
                            )
                            Text(
                                text = hours,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = SponsorTextDark
                            )
                        }
                    }
                }
            }

            // Details Section
            val hasDetails = !sponsor.phone.isNullOrBlank() ||
                    !sponsor.website.isNullOrBlank() ||
                    !sponsor.address.isNullOrBlank()

            if (hasDetails) {
                item {
                    SponsorDetailSection(title = "Details") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            sponsor.phone?.let { phone ->
                                SponsorDetailRow(
                                    label = "Phone",
                                    value = phone,
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        )
                                    }
                                )
                            }

                            sponsor.website?.let { website ->
                                SponsorDetailRow(
                                    label = "Website",
                                    value = website,
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                        )
                                    }
                                )
                            }

                            sponsor.address?.let { address ->
                                SponsorDetailRow(
                                    label = "Address",
                                    value = address,
                                    onClick = {
                                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsorTagChip(
    text: String,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isHighlighted) SponsorCoralRed else SponsorBorderLight,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (isHighlighted) SponsorCoralRed.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) SponsorCoralRed else SponsorTextMedium,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SponsorDetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SponsorTextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SponsorWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SponsorDetailRow(
    label: String,
    value: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SponsorTextLight,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = SponsorTextDark,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
