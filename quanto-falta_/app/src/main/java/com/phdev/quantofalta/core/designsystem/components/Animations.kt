package com.phdev.quantofalta.core.designsystem.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Applies a subtle scale compaction on press.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    scaleOnPress: Float = 0.97f
): Modifier = composed {
    val isReduced = isReducedMotionEnabled()
    if (isReduced || !enabled) return@composed this

    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleOnPress else 1.0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

/**
 * A click modifier that automatically applies press-scale compression and (optional) discrete haptic feedback.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleOnPress: Float = 0.97f,
    useHaptic: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    
    this
        .pressScale(interactionSource, enabled, scaleOnPress)
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = {
                if (useHaptic) {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (e: Exception) {
                        // Safe catch
                    }
                }
                onClick()
            }
        )
}

/**
 * Fade-in and subtle slide-in translation modifier for entry transitions.
 */
fun Modifier.fadeSlideIn(
    delayMillis: Int = 0,
    durationMillis: Int = 180,
    offsetYDp: Float = 10f
): Modifier = composed {
    val isReduced = isReducedMotionEnabled()
    if (isReduced) return@composed this

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(offsetYDp) }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        launch {
            alpha.animateTo(
                1f,
                animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            offsetY.animateTo(
                0f,
                animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing)
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = offsetY.value * density
    }
}
