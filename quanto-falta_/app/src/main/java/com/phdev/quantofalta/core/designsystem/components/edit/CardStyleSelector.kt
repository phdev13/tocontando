package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.domain.model.mode.CardStyleItem
import com.phdev.quantofalta.domain.model.mode.StandardCardStyle

// ─────────────────────────────────────────────────────────────────────────────
// Unified generic selector – works for Standard, Relationship and Salary cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun <T : CardStyleItem> UnifiedCardStyleSelector(
    styles: List<T>,
    selectedStyle: T,
    isPremium: Boolean,
    onStyleSelected: (T) -> Unit,
    onPremiumLocked: (T) -> Unit,
    modifier: Modifier = Modifier,
    stylePreview: @Composable ((T, Color) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Estilo do card",
            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.small)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(styles, key = { it.styleId }) { style ->
                val isSelected = style.styleId == selectedStyle.styleId
                val isLocked = style.isPremium && !isPremium

                var pressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.93f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "card_style_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .scale(scale)
                            .clip(AppShapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = AppShapes.medium
                            )
                            .pointerInput(style) {
                                detectTapGestures(
                                    onPress = {
                                        pressed = true
                                        tryAwaitRelease()
                                        pressed = false
                                    },
                                    onTap = {
                                        if (isLocked) onPremiumLocked(style)
                                        else onStyleSelected(style)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(AppSpacing.small)
                        ) {
                            if (isLocked) {
                                Icon(
                                    Icons.Filled.Lock,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                            }

                            val abstractColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline

                            if (stylePreview != null) {
                                stylePreview(style, abstractColor)
                            } else {
                                DefaultStylePreview(style, abstractColor)
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = style.displayName,
                        style = AppTypography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultStylePreview(style: CardStyleItem, color: Color) {
    // Generic abstract preview shapes – works for any card type
    when (style.styleId) {
        // Classic / Heart
        "classic", "heart" -> {
            Box(Modifier.size(24.dp).background(color.copy(alpha = 0.2f), CircleShape))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(16.dp).width(40.dp).background(color.copy(alpha = 0.6f), AppShapes.small))
        }
        // Blocks / Salary blocks
        "blocks", "salary_blocks" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(20.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
                Box(Modifier.size(20.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
                Box(Modifier.size(20.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
            }
        }
        // Linear / Month progress
        "linear_progress", "month_progress", "receiving_cycle" -> {
            Box(Modifier.height(12.dp).width(60.dp).background(color.copy(alpha = 0.2f), CircleShape)) {
                Box(Modifier.height(12.dp).fillMaxWidth(0.6f).background(color.copy(alpha = 0.8f), CircleShape))
            }
        }
        // Circular / Circle
        "circular_progress", "circle" -> {
            Box(Modifier.size(36.dp).border(4.dp, color.copy(alpha = 0.8f), CircleShape))
        }
        // Timeline
        "timeline" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color.copy(alpha = 0.8f), CircleShape))
                Box(Modifier.height(2.dp).width(40.dp).background(color.copy(alpha = 0.8f)))
                Box(Modifier.size(8.dp).border(2.dp, color.copy(alpha = 0.8f), CircleShape))
            }
        }
        // Minimal / Minimalist finance
        "minimal", "minimalist_finance" -> {
            Box(Modifier.height(24.dp).width(36.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
        }
        // Poster / Cartaz
        "poster" -> {
            Box(Modifier.fillMaxWidth().height(40.dp).background(color.copy(alpha = 0.2f), AppShapes.small))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.height(24.dp).width(50.dp).background(color.copy(alpha = 0.6f), AppShapes.small))
        }
        // Date focus
        "date_focus" -> {
            Box(Modifier.height(32.dp).width(40.dp).background(color.copy(alpha = 0.6f), AppShapes.small))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.height(8.dp).width(60.dp).background(color.copy(alpha = 0.2f), AppShapes.small))
        }
        // Detailed
        "detailed", "finance_dashboard" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.height(12.dp).width(70.dp).background(color.copy(alpha = 0.5f), AppShapes.small))
                Box(Modifier.height(12.dp).width(50.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
                Box(Modifier.height(12.dp).width(60.dp).background(color.copy(alpha = 0.3f), AppShapes.small))
            }
        }
        // Emotional / Survival
        "emotional", "survival_mode" -> {
            Box(Modifier.height(8.dp).width(50.dp).background(color.copy(alpha = 0.3f), AppShapes.small))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(20.dp).width(40.dp).background(color.copy(alpha = 0.6f), AppShapes.small))
        }
        // Chronology
        "chronology" -> {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(7.dp).background(color.copy(alpha = 0.8f), CircleShape))
                    Box(Modifier.height(7.dp).width(45.dp).background(color.copy(alpha = 0.4f), AppShapes.small))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(7.dp).background(color.copy(alpha = 0.5f), CircleShape))
                    Box(Modifier.height(7.dp).width(35.dp).background(color.copy(alpha = 0.25f), AppShapes.small))
                }
            }
        }
        // Progress
        "progress", "business_days" -> {
            Box(Modifier.height(12.dp).width(60.dp).background(color.copy(alpha = 0.2f), CircleShape)) {
                Box(Modifier.height(12.dp).fillMaxWidth(0.4f).background(color.copy(alpha = 0.8f), CircleShape))
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(20.dp).background(color.copy(alpha = 0.3f), CircleShape))
        }
        // Goal / Value
        "salary_goal", "value_and_time" -> {
            Box(Modifier.height(28.dp).width(52.dp).background(color.copy(alpha = 0.3f), AppShapes.small))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.height(8.dp).width(40.dp).background(color.copy(alpha = 0.5f), AppShapes.small))
        }
        // Next salary (default fallback)
        else -> {
            Box(Modifier.size(24.dp).background(color.copy(alpha = 0.2f), CircleShape))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(16.dp).width(40.dp).background(color.copy(alpha = 0.6f), AppShapes.small))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Backwards-compatible typed overloads — kept for callers that still use them
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardStyleSelector(
    selectedStyle: StandardCardStyle,
    isPremium: Boolean,
    onStyleSelected: (StandardCardStyle) -> Unit,
    onPremiumLocked: (StandardCardStyle) -> Unit
) {
    UnifiedCardStyleSelector(
        styles = StandardCardStyle.entries,
        selectedStyle = selectedStyle,
        isPremium = isPremium,
        onStyleSelected = onStyleSelected,
        onPremiumLocked = onPremiumLocked
    )
}
