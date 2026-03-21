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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.faster.festival.data.models.PromotionItem

// Light theme palette (consistent with HomeScreen)
private val PromoBg = Color(0xFFF7F7F7)
private val PromoWhite = Color.White
private val PromoCoralRed = Color(0xFFE53935)
private val PromoTextDark = Color(0xFF222222)
private val PromoTextMedium = Color(0xFF333333)
private val PromoTextLight = Color(0xFF666666)
private val PromoBorderLight = Color(0xFFE0E0E0)
private val PromoCardBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionDetailScreen(
    promotion: PromotionItem,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = promotion.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PromoTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PromoTextDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PromoTextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PromoWhite
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header section: Title, subtitle, tags
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PromoWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = promotion.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PromoTextDark
                    )

                    promotion.subtitle?.let { sub ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PromoTextLight
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (promotion.isActive) {
                            TagChip(text = "OPEN", isHighlighted = true)
                        }
                        if (promotion.isExclusive) {
                            TagChip(text = "EXCLUSIVE", isHighlighted = false)
                        }
                    }
                }
            }

            // Image gallery
            val allImages = buildList {
                promotion.thumbnailUrl?.let { add(it) }
                addAll(promotion.mediaUrls)
            }.distinct()

            if (allImages.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        items(allImages) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Promotion image",
                                modifier = Modifier
                                    .size(width = 160.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Action buttons (Directions + Menu)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Directions button
                    Button(
                        onClick = {
                            val address = promotion.address ?: promotion.locationText ?: ""
                            if (address.isNotBlank()) {
                                try {
                                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) { }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PromoCoralRed
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Directions", fontWeight = FontWeight.SemiBold)
                    }

                    // Menu button
                    OutlinedButton(
                        onClick = {
                            promotion.menuUrl?.let { url ->
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) { }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PromoBorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PromoTextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Menu",
                            fontWeight = FontWeight.SemiBold,
                            color = PromoTextDark
                        )
                    }
                }
            }

            // Exclusive Promotion Offer Card
            val hasOffer = !promotion.offerText.isNullOrBlank()
            if (hasOffer) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (promotion.isExclusive) "Exclusive Promotion" else "Promotion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PromoTextDark,
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
                                    text = promotion.offerText ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                promotion.description?.let { desc ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                val locationLine = listOfNotNull(
                                    promotion.vendorName,
                                    promotion.locationText
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
            val aboutText = promotion.description
            if (!aboutText.isNullOrBlank() && !hasOffer) {
                item {
                    DetailSection(title = "About") {
                        Text(
                            text = aboutText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PromoTextMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else if (!aboutText.isNullOrBlank() && hasOffer) {
                // Show about even when offer is present
                item {
                    DetailSection(title = "About") {
                        Text(
                            text = "Description about the vendor, what they are known for, and what people can purchase",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PromoTextMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Hours Section
            promotion.hoursText?.let { hours ->
                item {
                    DetailSection(title = "Hours") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Every Day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PromoTextMedium
                            )
                            Text(
                                text = hours,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = PromoTextDark
                            )
                        }
                    }
                }
            }

            // Details Section (Phone, Website, Address)
            val hasDetails = !promotion.phone.isNullOrBlank() ||
                    !promotion.website.isNullOrBlank() ||
                    !promotion.address.isNullOrBlank()

            if (hasDetails) {
                item {
                    DetailSection(title = "Details") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            promotion.phone?.let { phone ->
                                DetailRow(
                                    label = "Phone",
                                    value = phone,
                                    icon = Icons.Default.Phone,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    }
                                )
                            }

                            promotion.website?.let { website ->
                                DetailRow(
                                    label = "Website",
                                    value = website,
                                    icon = Icons.Default.Language,
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website)))
                                    }
                                )
                            }

                            promotion.address?.let { address ->
                                DetailRow(
                                    label = "Address",
                                    value = address,
                                    icon = Icons.Default.LocationOn,
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
private fun TagChip(
    text: String,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isHighlighted) PromoCoralRed else PromoBorderLight,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (isHighlighted) PromoCoralRed.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) PromoCoralRed else PromoTextMedium,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun DetailSection(
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
            color = PromoTextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PromoWhite),
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
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
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
            color = PromoTextLight,
            modifier = Modifier.width(80.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = PromoTextDark
            )
        }
    }
}
