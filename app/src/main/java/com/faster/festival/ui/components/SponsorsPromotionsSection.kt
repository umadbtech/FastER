package com.faster.festival.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.HomeModule
import com.faster.festival.data.models.PromotionItem
import com.faster.festival.data.models.SponsorOffer
import com.faster.festival.utils.rememberComingSoonToast

private val PromotionGradientStart = Color(0xFF0088FF)
private val PromotionGradientEnd = Color(0xFF0055CC)
private val PromotionAccentLight = Color(0xFFB0D4F1)
private val SponsorAccentRed = Color(0xFFE53935)

/**
 * Reusable home-bundle section that renders the Sponsors and Promotions
 * carousels in one place. Used on the Home screen and the bottom of the Map
 * screen so both stay visually in sync.
 *
 * Each sub-section is rendered only when its module is enabled in the bundle
 * and its item list is non-empty. Pass explicit titles to match the caller's
 * title-resolution rules, or omit them to use the bundle's display titles.
 */
@Composable
fun SponsorsPromotionsSection(
    bundle: AppHomeBundleResponse,
    onSponsorClick: (String) -> Unit,
    onPromotionClick: (String) -> Unit,
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    sponsorsTitle: String = resolveTitle(bundle, "sponsors", "Sponsors"),
    promotionsTitle: String = resolveTitle(bundle, "promotions", "Promotions"),
    sectionSpacing: androidx.compose.ui.unit.Dp = 24.dp
) {
    val showSponsors = bundle.isModuleEnabled("sponsors") && bundle.sponsorOffers.isNotEmpty()
    val showPromotions = bundle.isModuleEnabled("promotions") && bundle.promotions.isNotEmpty()

    if (!showSponsors && !showPromotions) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        if (showSponsors) {
            SponsorCarousel(
                title = sponsorsTitle,
                sponsors = bundle.sponsorOffers,
                onSponsorClick = onSponsorClick,
                onCtaClick = onCtaClick
            )
        }
        if (showPromotions) {
            PromotionCarousel(
                title = promotionsTitle,
                promotions = bundle.promotions,
                onPromotionClick = onPromotionClick,
                onCtaClick = onCtaClick
            )
        }
    }
}

/**
 * Reusable horizontal carousel of sponsor offer cards with a section header.
 * Renders nothing if [sponsors] is empty.
 */
@Composable
fun SponsorCarousel(
    title: String,
    sponsors: List<SponsorOffer>,
    onSponsorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    if (sponsors.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(sponsors, key = { it.id }) { sponsor ->
                SponsorCard(
                    sponsor = sponsor,
                    onClick = { onSponsorClick(sponsor.id) },
                    onCtaClick = onCtaClick,
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}

/**
 * Reusable horizontal carousel of promotion cards with a section header.
 * Renders nothing if [promotions] is empty.
 */
@Composable
fun PromotionCarousel(
    title: String,
    promotions: List<PromotionItem>,
    onPromotionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    if (promotions.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(promotions, key = { it.id }) { promo ->
                PromotionCard(
                    promotion = promo,
                    onClick = { onPromotionClick(promo.id) },
                    onCtaClick = onCtaClick,
                    modifier = Modifier.width(300.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun PromotionCard(
    promotion: PromotionItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PromotionGradientStart, PromotionGradientEnd)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                PromotionImageOrFallback(
                    imageUrl = promotion.thumbnailUrl,
                    contentDescription = promotion.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = promotion.offerText ?: promotion.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                val desc = promotion.description ?: promotion.subtitle
                if (!desc.isNullOrBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val locationLine = listOfNotNull(
                    promotion.vendorName,
                    promotion.locationText
                ).joinToString(", ")
                if (locationLine.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PromotionAccentLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = locationLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Supabase-driven CTA: label from `cta_label`, click opens
                // InAppWebViewScreen with `cta_url`. Hidden when no URL is provided.
                val ctaUrl = promotion.ctaUrl?.takeIf { it.isNotBlank() }
                if (ctaUrl != null) {
                    val ctaLabel = promotion.ctaLabel?.takeIf { it.isNotBlank() } ?: "Open"
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onCtaClick(ctaUrl, promotion.title) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PromotionGradientEnd
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ctaLabel,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SponsorCard(
    sponsor: SponsorOffer,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCtaClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val imageUrl = sponsor.primaryMediaUrl ?: sponsor.sponsorLogoUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = sponsor.sponsorName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = sponsor.sponsorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val offerLine = sponsor.offerText ?: sponsor.title
                if (!offerLine.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = offerLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SponsorAccentRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                sponsor.subtitle?.let { sub ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Supabase-driven CTA: label from `cta_label`. If `cta_url` is
                // present we navigate to the in-app web view; otherwise fall
                // back to the existing "coming soon" toast so the label still
                // communicates intent to the user.
                val ctaLabel = sponsor.ctaLabel?.takeIf { it.isNotBlank() }
                if (ctaLabel != null) {
                    val ctaUrl = sponsor.ctaUrl?.takeIf { it.isNotBlank() }
                    val ctaTitle = sponsor.title ?: sponsor.sponsorName
                    val showComingSoonSponsorCta = rememberComingSoonToast()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SponsorAccentRed,
                        modifier = Modifier.clickable {
                            if (ctaUrl != null) {
                                onCtaClick(ctaUrl, ctaTitle)
                            } else {
                                showComingSoonSponsorCta()
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun resolveTitle(
    bundle: AppHomeBundleResponse,
    moduleKey: String,
    fallback: String
): String {
    val module: HomeModule? = bundle.moduleByKey(moduleKey)
    return module?.displayTitle
        ?: module?.title
        ?: fallback
}
