package com.phdev.quantofalta.feature.eventdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.domain.model.EventUiModel

@Composable
fun ShareImageCard(
    event: EventUiModel,
    hideExactDate: Boolean,
    hideWatermark: Boolean,
    aspectRatio: ShareAspectRatio
) {
    // Determine colors
    val bgColor = event.color
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            bgColor.copy(alpha = 0.8f),
            bgColor.copy(alpha = 1.0f)
        )
    )

    // Determine layout scale based on aspect ratio
    val isStory = aspectRatio == ShareAspectRatio.STORY || aspectRatio == ShareAspectRatio.WALLPAPER
    val contentPadding = if (isStory) 64.dp else 48.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(contentPadding)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(if (isStory) 120.dp else 90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(event.iconName),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isStory) 60.dp else 45.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = event.title,
                color = Color.White,
                fontSize = if (isStory) 36.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!hideExactDate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${event.date} às ${event.time}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = if (isStory) 18.sp else 14.sp
                )
            }

            Spacer(modifier = Modifier.height(if (isStory) 48.dp else 32.dp))

            // Box for Countdown
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = event.number,
                        color = bgColor,
                        style = TextStyle(
                            fontSize = if (isStory) 84.sp else 64.sp,
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        )
                    )
                    Text(
                        text = event.units.uppercase(),
                        color = bgColor.copy(alpha = 0.8f),
                        fontSize = if (isStory) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isStory) 32.dp else 24.dp))

            // Custom Message/Badge
            Text(
                text = event.badgeText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = if (isStory) 24.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        if (!hideWatermark) {
            // Watermark Footer
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isStory) 48.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳ Tô Contando",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = if (isStory) 20.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

enum class ShareAspectRatio(val width: Int, val height: Int) {
    SQUARE(1080, 1080),
    STORY(1080, 1920),
    WALLPAPER(1440, 3200)
}
