package com.phdev.quantofalta.feature.eventdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.RelationshipFeaturedCard
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.domain.model.EventUiModel

@Composable
fun ShareImageCard(
    event: EventUiModel,
    hideExactDate: Boolean, // Note: We keep this param but the Home cards might not support it without modification. The user explicitly asked for the literal Home card.
    hideWatermark: Boolean,
    aspectRatio: ShareAspectRatio,
) {
    val isRelationship = event.relationshipUiState != null
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(event.color)
    ) {
        val compactPreview = maxWidth < 300.dp
        
        if (event.coverImageUri != null) {
            AsyncImage(
                model = event.coverImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.26f),
                                event.color.copy(alpha = 0.22f),
                                Color.Black.copy(alpha = if (aspectRatio == ShareAspectRatio.STORY) 0.82f else 0.74f)
                            )
                        )
                    )
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                event.color,
                                event.color.copy(
                                    red = (event.color.red * 0.72f).coerceIn(0f, 1f),
                                    green = (event.color.green * 0.78f).coerceIn(0f, 1f),
                                    blue = (event.color.blue * 1.08f).coerceIn(0f, 1f)
                                )
                            )
                        )
                    )
            )
            Icon(
                imageVector = getIconByName(event.iconName),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(if (aspectRatio == ShareAspectRatio.SQUARE) 260.dp else 380.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compactPreview) 16.dp else 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRelationship) {
                RelationshipFeaturedCard(
                    event = event,
                    onClick = {}
                )
            } else {
                MainEventCard(
                    title = event.title,
                    date = event.date,
                    number = event.number,
                    units = event.units,
                    color = event.color,
                    iconName = event.iconName,
                    progress = event.progress,
                    contextMessage = event.contextMessage,
                    targetDateMillis = event.dateMillis,
                    coverImageUri = event.coverImageUri,
                    onClick = {}
                )
            }
        }
        
        if (!hideWatermark) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compactPreview) 16.dp else 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compactPreview) 22.dp else 30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByName("CalendarMonth"),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(if (compactPreview) 14.dp else 18.dp)
                    )
                }
                Text(
                    "Tô Contando",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

enum class ShareAspectRatio(val width: Int, val height: Int) {
    SQUARE(1080, 1080),
    STORY(1080, 1920),
    WALLPAPER(1440, 3200),
}
