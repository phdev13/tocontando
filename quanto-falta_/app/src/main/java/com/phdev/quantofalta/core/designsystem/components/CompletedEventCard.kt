package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@Composable
fun CompletedEventCard(
    event: com.phdev.quantofalta.domain.model.EventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPremiumCardsEnabled = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(useHaptic = true, onClick = onClick),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat as per design usually
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.medium, horizontal = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppShapes.pill)
                    .background(event.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPremiumCardsEnabled && event.coverImageUri != null) {
                    val completedImageRequest = androidx.compose.runtime.remember(event.coverImageUri) {
                        coil.request.ImageRequest.Builder(context)
                            .data(event.coverImageUri)
                            .size(160, 160)
                            .memoryCacheKey("event-cover-completed:${event.coverImageUri}")
                            .diskCacheKey("event-cover-completed:${event.coverImageUri}")
                            .crossfade(false)
                            .build()
                    }
                    coil.compose.AsyncImage(
                        model = completedImageRequest,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = getIconByName(event.iconName),
                        contentDescription = null,
                        tint = event.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(AppSpacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.secondaryText,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun CompletedEventCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.medium, horizontal = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonIcon(size = 40.dp)
            Spacer(modifier = Modifier.width(AppSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 140.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonText(width = 90.dp, height = 16.dp)
            }
        }
    }
}
