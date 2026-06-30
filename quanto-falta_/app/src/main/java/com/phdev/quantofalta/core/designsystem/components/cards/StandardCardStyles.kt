package com.phdev.quantofalta.core.designsystem.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.ContrastUtils
import com.phdev.quantofalta.domain.model.EventUiModel

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import androidx.compose.material3.Icon

// --- MAIN CARDS ---

@Composable
fun MainBlocksCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(textColor.copy(alpha = 0.15f), AppShapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = textColor)
                }
                Spacer(modifier = Modifier.width(AppSpacing.medium))
                Text(
                    text = event.title, 
                    style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                    color = textColor,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            
            com.phdev.quantofalta.core.designsystem.components.SmartBlocksCountdown(targetMillis = event.dateMillis, textColor = textColor)
        }
    }}



@Composable
fun MainLinearProgressCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.large)) {
            Text(text = event.title, style = AppTypography.titleLarge, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(textColor.copy(0.2f), AppShapes.small)) {
                Box(modifier = Modifier.fillMaxWidth(event.progress).height(12.dp).background(textColor, AppShapes.small))
            }
            Text(text = "${(event.progress * 100).toInt()}% concluído", color = textColor)
        }
    }}

@Composable
fun MainCircularProgressCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(56.dp))
                        .background(textColor.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(46.dp))
                        .background(event.color)
                )
                Text(
                    text = "${(event.progress * 100).toInt()}%",
                    style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.large))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor, maxLines = 2)
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(text = event.primaryText, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                Text(text = event.secondaryText, style = AppTypography.bodySmall, color = textColor.copy(alpha = 0.82f))
            }
        }
    }}

@Composable
fun MainTimelineCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Text(text = event.title, style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor, maxLines = 2)
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimelineDot(textColor)
                TimelineLine(textColor, 0.35f)
                TimelineDot(textColor.copy(alpha = 0.75f))
                TimelineLine(textColor, 0.2f)
                TimelineDot(textColor.copy(alpha = 0.45f))
            }
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            Text(text = event.primaryText, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Black), color = textColor)
            Text(text = event.secondaryText, style = AppTypography.bodySmall, color = textColor.copy(alpha = 0.82f))
        }
    }}

@Composable
fun MainMinimalCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.large),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = event.title, style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = textColor.copy(alpha = 0.86f), maxLines = 2)
            Text(text = event.primaryText, style = AppTypography.displayLarge.copy(fontSize = 52.sp, lineHeight = 54.sp, fontWeight = FontWeight.Black), color = textColor)
            Text(text = event.secondaryText, style = AppTypography.labelLarge, color = textColor.copy(alpha = 0.72f))
        }
    }}

@Composable
fun MainPosterCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = getIconByName(event.iconName),
                contentDescription = null,
                tint = textColor.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.CenterEnd).size(180.dp)
            )
            Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
                Text(text = event.secondaryText.uppercase(), style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textColor.copy(alpha = 0.78f))
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(text = event.title, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Black), color = textColor, maxLines = 3)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = event.primaryText, style = AppTypography.displayLarge.copy(fontWeight = FontWeight.Black), color = textColor)
            }
        }
    }}

@Composable
fun MainDateFocusCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(104.dp)
                    .clip(AppShapes.medium)
                    .background(textColor.copy(alpha = 0.14f))
                    .padding(AppSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = event.secondaryText.take(3).uppercase(), style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold), color = textColor.copy(alpha = 0.72f))
                Text(text = event.primaryText.filter { it.isDigit() }.ifBlank { event.primaryText }, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Black), color = textColor)
            }
            Spacer(modifier = Modifier.width(AppSpacing.large))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = textColor, maxLines = 3)
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(text = event.contextMessage.ifBlank { event.secondaryText }, style = AppTypography.bodyMedium, color = textColor.copy(alpha = 0.82f))
            }
        }
    }}

@Composable
fun MainDetailedCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(AppSpacing.small))
                Text(text = event.title, style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor, maxLines = 2)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = event.primaryText, style = AppTypography.displayLarge.copy(fontSize = 46.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black), color = textColor)
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(text = event.secondaryText, style = AppTypography.bodyMedium, color = textColor.copy(alpha = 0.86f))
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(AppShapes.small).background(textColor.copy(alpha = 0.22f))) {
                Box(modifier = Modifier.fillMaxWidth(event.progress).height(5.dp).background(textColor))
            }
        }
    }}

@Composable
fun MainEmotionalCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    val baseTextColor = remember(event.color) { ContrastUtils.getContrastColor(event.color) }
    val textColor = remember(hasPhoto, baseTextColor) { if (hasPhoto) Color.White else baseTextColor }
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.large),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = event.contextMessage.ifBlank { "Falta pouco para a sua data." }, style = AppTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = textColor.copy(alpha = 0.86f), maxLines = 2)
            Column {
                Text(text = event.title, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Black), color = textColor, maxLines = 2)
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(text = event.primaryText, style = AppTypography.displayLarge.copy(fontWeight = FontWeight.Black), color = textColor)
            }
        }
    }}

// --- COMPACT CARDS ---

@Composable
fun CompactBlocksCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { Box(modifier = Modifier.size(10.dp).background(event.color.copy(alpha = 0.45f), AppShapes.small)) }
        }
    }
}

@Composable
fun CompactLinearProgressCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) {
        Box(modifier = Modifier.width(48.dp).height(6.dp).clip(AppShapes.small).background(event.color.copy(alpha = 0.2f))) {
            Box(modifier = Modifier.fillMaxWidth(event.progress).height(6.dp).background(event.color))
        }
    }
}

@Composable
fun CompactCircularProgressCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(event.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Text(text = "${(event.progress * 100).toInt()}%", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold), color = event.color)
        }
    }
}

@Composable
fun CompactTimelineCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) { TimelineDot(event.color) }
}

@Composable
fun CompactMinimalCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier, showDetail = false) { Text(text = event.primaryText, style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Black), color = event.color) }
}

@Composable
fun CompactPosterCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) { Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = event.color, modifier = Modifier.size(26.dp)) }
}

@Composable
fun CompactDateFocusCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) { Text(text = event.primaryText.filter { it.isDigit() }.ifBlank { event.primaryText }, style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Black), color = event.color) }
}

@Composable
fun CompactDetailedCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier, showProgress = true) { Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = event.color, modifier = Modifier.size(24.dp)) }
}

@Composable
fun CompactEmotionalCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CompactStyledCard(event, onClick, modifier) { Text(text = "!", style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Black), color = event.color) }
}

@Composable
private fun TimelineDot(color: Color) {
    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(color))
}

@Composable
private fun TimelineLine(color: Color, alpha: Float) {
    Box(modifier = Modifier.width(54.dp).height(3.dp).background(color.copy(alpha = alpha)))
}

@Composable
private fun CompactStyledCard(
    event: EventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDetail: Boolean = true,
    showProgress: Boolean = false,
    leading: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().bounceClick(onClick = onClick),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(AppShapes.small).background(event.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                leading()
            }
            Spacer(modifier = Modifier.width(AppSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = AppTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = event.primaryText, style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = event.color)
                if (showDetail) {
                    Text(text = event.secondaryText, style = AppTypography.labelMedium, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                }
                if (showProgress) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(AppShapes.small).background(event.color.copy(alpha = 0.18f))) {
                        Box(modifier = Modifier.fillMaxWidth(event.progress).height(4.dp).background(event.color))
                    }
                }
            }
        }
    }
}

@Composable
fun StandardHeroSurface(
    event: EventUiModel,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasPhoto = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context) && event.coverImageUri != null
    
    Box(modifier = modifier.fillMaxSize()) {
        if (hasPhoto) {
            val featuredImageRequest = androidx.compose.runtime.remember(event.coverImageUri) {
                coil.request.ImageRequest.Builder(context)
                    .data(event.coverImageUri)
                    .size(480, 320)
                    .memoryCacheKey("event-cover-featured:${event.coverImageUri}")
                    .diskCacheKey("event-cover-featured:${event.coverImageUri}")
                    .crossfade(false)
                    .build()
            }
            coil.compose.AsyncImage(
                model = featuredImageRequest,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val bottomOverlayColor = androidx.compose.runtime.remember(event.color) {
                Color(
                    red = event.color.red * 0.25f,
                    green = event.color.green * 0.25f,
                    blue = event.color.blue * 0.25f,
                    alpha = 0.90f
                )
            }
            val overlayBrush = androidx.compose.runtime.remember(bottomOverlayColor) {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.15f),
                        bottomOverlayColor
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = overlayBrush)
            )
        }
        content()
    }
}
