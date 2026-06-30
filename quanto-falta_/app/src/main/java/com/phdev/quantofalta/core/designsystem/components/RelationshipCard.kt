package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.phdev.quantofalta.core.config.AppConfigManager
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.core.relationship.RelationshipCalculator

import java.time.Instant
import java.time.ZoneId

private val FeaturedEventCardHeight = 220.dp

/**
 * Compact list card for Relationship Mode events.
 * Uses exactly the same visual structure as CompactEventCard —
 * left icon box | text column | right chip.
 * No new colors, shapes, or design elements.
 */
@Composable
fun RelationshipCompactCard(
    event: EventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rel = event.relationshipUiState ?: return
    val ticker = com.phdev.quantofalta.core.time.LocalScreenTicker.current
    val nowMillis = ticker.tickHour.value
    
    val today = remember(nowMillis) {
        Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val liveStats = remember(rel.startEpochDay, today) {
        RelationshipCalculator.calculate(rel.startEpochDay, today)
    }
    
    val liveSecondaryText = remember(liveStats, rel.monthlyEnabled, rel.annualEnabled, rel.milestonesEnabled) {
        RelationshipCalculator.formatSecondaryText(
            liveStats,
            rel.monthlyEnabled,
            rel.annualEnabled,
            rel.milestonesEnabled,
        )
    }
    val showCover = AppConfigManager.isPremiumCardsEnabled(LocalContext.current) &&
        event.coverImageUri != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Icon box (same as CompactEventCard) ──────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShapes.small)
                    .background(event.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (showCover) {
                    val context = LocalContext.current
                    val compactImageRequest = remember(event.coverImageUri) {
                        ImageRequest.Builder(context)
                            .data(event.coverImageUri)
                            .size(160, 160)
                            .memoryCacheKey("relationship-cover-compact:${event.coverImageUri}")
                            .diskCacheKey("relationship-cover-compact:${event.coverImageUri}")
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = compactImageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = getIconByName(event.iconName),
                        contentDescription = null,
                        tint = event.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.medium))

            // ── Text column ──────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = RelationshipCalculator.formatPrimaryText(liveStats, rel.relationshipType),
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = event.color
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (liveSecondaryText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = liveSecondaryText,
                        style = AppTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.small))

            // ── Type badge ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(AppShapes.small)
                    .background(event.color.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = relationshipTypeLabel(rel.relationshipType),
                    style = AppTypography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = event.color,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun RelationshipFeaturedCard(
    event: EventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rel = event.relationshipUiState ?: return
    val ticker = com.phdev.quantofalta.core.time.LocalScreenTicker.current
    val nowMillis = ticker.tickHour.value
    
    val today = remember(nowMillis) {
        Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val liveStats = remember(rel.startEpochDay, today) {
        RelationshipCalculator.calculate(rel.startEpochDay, today)
    }
    val context = LocalContext.current
    val hasCover = event.coverImageUri != null
    val baseTextColor = com.phdev.quantofalta.core.designsystem.theme.ContrastUtils
        .getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor

    val backgroundBrush = remember(event.color) {
        Brush.linearGradient(
            listOf(
                event.color,
                event.color.copy(
                    red = (event.color.red * 0.75f).coerceAtMost(1f),
                    green = (event.color.green * 0.75f).coerceAtMost(1f),
                    blue = (event.color.blue * 1.25f).coerceAtMost(1f),
                )
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().bounceClick(useHaptic = true, onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FeaturedEventCardHeight)
                .background(brush = backgroundBrush)
        ) {
            if (hasCover) {
                val featuredImageRequest = remember(event.coverImageUri) {
                    ImageRequest.Builder(context)
                        .data(event.coverImageUri)
                        .size(480, 320)
                        .memoryCacheKey("relationship-cover-featured:${event.coverImageUri}")
                        .diskCacheKey("relationship-cover-featured:${event.coverImageUri}")
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = featuredImageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(AppShapes.large),
                )
                val bottomOverlayColor = remember(event.color) {
                    Color(
                        red = event.color.red * 0.25f,
                        green = event.color.green * 0.25f,
                        blue = event.color.blue * 0.25f,
                        alpha = 0.90f
                    )
                }
                val overlayBrush = remember(bottomOverlayColor) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.15f),
                            bottomOverlayColor
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapes.large)
                        .background(brush = overlayBrush)
                )
            } else {
                Icon(
                    imageVector = getIconByName("Favorite"),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 40.dp, y = 40.dp)
                        .size(260.dp)
                        .graphicsLayer { rotationZ = -15f }
                )
            }

            // Badge top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppSpacing.medium, end = AppSpacing.medium)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName("Favorite"),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.large, vertical = AppSpacing.mediumLarge)) {
                Text(
                    event.title,
                    style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    relationshipTypeLabel(rel.relationshipType),
                    style = AppTypography.labelMedium,
                    color = textColor.copy(alpha = 0.85f),
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "JUNTOS HÁ",
                    style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 10.sp),
                    color = textColor.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveStats.years > 0) {
                        TimeBlockRel(liveStats.years, if (liveStats.years == 1) "ano" else "anos", textColor, 1.2f)
                        TimeBlockRel(liveStats.months, if (liveStats.months == 1) "mês" else "meses", textColor, 1f)
                        TimeBlockRel(liveStats.remainingDays, if (liveStats.remainingDays == 1) "dia" else "dias", textColor, 1f)
                    } else if (liveStats.months > 0) {
                        TimeBlockRel(liveStats.months, if (liveStats.months == 1) "mês" else "meses", textColor, 1.2f)
                        TimeBlockRel(liveStats.remainingDays, if (liveStats.remainingDays == 1) "dia" else "dias", textColor, 1f)
                    } else {
                        TimeBlockRel(liveStats.remainingDays, if (liveStats.remainingDays == 1) "dia" else "dias", textColor, 1.2f)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val totalDays = liveStats.totalDays
                val targetMilestone = listOf(100L, 500L, 1000L, 5000L, 10000L).firstOrNull { it > totalDays } ?: 10000L
                val progress = (totalDays.toFloat() / targetMilestone.toFloat()).coerceIn(0f, 1f)

                Text(
                    text = "Próximo marco: $targetMilestone dias",
                    style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = textColor.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(textColor)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TimeBlockRel(value: Int, label: String, textColor: Color = Color.White, weight: Float) {
    Column(
        modifier = Modifier
            .weight(weight)
            .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value.toString(),
            style = AppTypography.displayLarge.copy(
                fontSize = if (value.toString().length >= 3) 28.sp else 34.sp,
                lineHeight = if (value.toString().length >= 3) 28.sp else 34.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum"
            ),
            color = textColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = AppTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = textColor.copy(alpha = 0.85f)
        )
    }
}

fun relationshipTypeLabel(type: String): String = when (type) {
    "dating" -> "Namoro"
    "married" -> "Casados"
    "engaged" -> "Noivado"
    "friendship" -> "Amizade"
    else -> "Juntos"
}

@Composable
fun RelationshipCompactCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonIcon(size = 44.dp, shape = AppShapes.small)
            Spacer(modifier = Modifier.width(AppSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 120.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonText(width = 80.dp, height = 16.dp)
            }
            Spacer(modifier = Modifier.width(AppSpacing.small))
            SkeletonBox(modifier = Modifier.size(width = 60.dp, height = 24.dp), shape = AppShapes.small)
        }
    }
}

@Composable
fun RelationshipFeaturedCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().height(FeaturedEventCardHeight),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(AppSpacing.mediumLarge)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonText(width = 100.dp, height = 24.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonText(width = 60.dp, height = 14.dp)
                    }
                    SkeletonIcon(size = 44.dp)
                }
                Spacer(Modifier.height(AppSpacing.medium))
                SkeletonText(width = 64.dp, height = 46.dp)
                Spacer(Modifier.height(4.dp))
                SkeletonText(width = 80.dp, height = 14.dp)
                Spacer(Modifier.height(16.dp))
                SkeletonText(width = 140.dp, height = 16.dp)
                Spacer(Modifier.height(16.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(6.dp), shape = CircleShape)
            }
        }
    }
}
