package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global Skeleton Modifier.
 * Provides a lightweight animated alpha pulse (from 0.6f to 1.0f) or static background 
 * for loading states to avoid FPS drops associated with heavy gradient shimmers.
 */
fun Modifier.skeleton(
    visible: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    color: Color? = null,
    pulse: Boolean = true
): Modifier = composed {
    if (!visible) return@composed this

    val skeletonColor = color ?: MaterialTheme.colorScheme.surfaceVariant
    val shouldPulse = pulse && !isReducedMotionEnabled()
    
    val alpha = if (shouldPulse) {
        val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
        val alphaAnim by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeleton_alpha"
        )
        alphaAnim
    } else {
        1.0f
    }

    this
        .background(color = skeletonColor.copy(alpha = skeletonColor.alpha * alpha), shape = shape)
        .clip(shape)
}

@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 16.dp,
    pulse: Boolean = true
) {
    Spacer(
        modifier = modifier
            .width(width)
            .height(height)
            .skeleton(visible = true, shape = RoundedCornerShape(4.dp), pulse = pulse)
    )
}

@Composable
fun SkeletonIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    shape: Shape = CircleShape,
    pulse: Boolean = true
) {
    Spacer(
        modifier = modifier
            .size(size)
            .skeleton(visible = true, shape = shape, pulse = pulse)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    pulse: Boolean = true
) {
    Box(
        modifier = modifier.skeleton(visible = true, shape = shape, pulse = pulse)
    )
}
