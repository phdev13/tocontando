package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    // Subtle breathing animation for the halo rings
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_breathing")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    // Dedicated sparkle floating animations for an extremely premium celestial vibe
    val sparkleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_1"
    )
    val sparkleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_2"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.extraLarge)
            .fadeSlideIn(durationMillis = 350),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celestial Illustrated Container
        Box(
            modifier = Modifier
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient background radial glow
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scaleFactor * 0.95f
                        scaleY = scaleFactor * 0.95f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tintColor.copy(alpha = 0.08f),
                                tintColor.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Outer delicate halo border ring
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    .size(116.dp)
                    .border(
                        width = 1.dp,
                        color = tintColor.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
            )

            // Middle translucent glow bubble
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleFactor * 0.98f
                        scaleY = scaleFactor * 0.98f
                    }
                    .size(92.dp)
                    .background(
                        color = tintColor.copy(alpha = 0.04f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = tintColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
            )

            // Innermost thick glassmorphic circle container
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tintColor.copy(alpha = 0.12f),
                                tintColor.copy(alpha = 0.06f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = tintColor.copy(alpha = 0.25f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Dual layered vector shadow for premium 3D lift effect
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = 2.dp)
                            .graphicsLayer { alpha = 0.22f },
                        tint = Color.Black
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = tintColor
                    )
                }
            }

            // --- Floating Premium Sparkles ---
            // Sparkle 1 (Top Left)
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 18.dp, y = 14.dp)
                    .graphicsLayer {
                        alpha = sparkleAlpha1
                        scaleX = sparkleScale * 0.9f
                        scaleY = sparkleScale * 0.9f
                    }
                    .size(14.dp)
            )

            // Sparkle 2 (Bottom Right)
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-20).dp)
                    .graphicsLayer {
                        alpha = sparkleAlpha2
                        scaleX = sparkleScale * 1.1f
                        scaleY = sparkleScale * 1.1f
                    }
                    .size(16.dp)
            )

            // Sparkle 3 (Top Right, micro)
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-26).dp, y = 22.dp)
                    .graphicsLayer {
                        alpha = sparkleAlpha1 * 0.7f
                        scaleX = sparkleScale * 0.7f
                        scaleY = sparkleScale * 0.7f
                    }
                    .size(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        // Large title text
        Text(
            text = title,
            style = AppTypography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                lineHeight = 26.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.small)
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))

        // Description / instruction text
        Text(
            text = description,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.extraLarge)
        )

        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

        // Premium action button with custom press compression and haptics
        Button(
            onClick = onButtonClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tintColor,
                contentColor = if (tintColor == MaterialTheme.colorScheme.primary) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    Color.White
            ),
            modifier = Modifier
                .testTag(testTag)
        ) {
            Text(
                text = buttonText,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
