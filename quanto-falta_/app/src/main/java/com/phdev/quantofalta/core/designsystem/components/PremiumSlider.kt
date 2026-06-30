package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@Composable
fun PremiumSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    
    // Animações para dar vida ao componente
    val thumbRadius by animateFloatAsState(
        targetValue = if (isDragging) 12f else 8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumbRadius"
    )
    val trackHeight by animateFloatAsState(
        targetValue = if (isDragging) 16f else 8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "trackHeight"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val disabledColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabeçalho (Label + Valor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else disabledColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) primaryColor else disabledColor
            )
        }

        // Slider Customizado com Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                            onValueChange(newValue)
                            tryAwaitRelease()
                            isDragging = false
                        }
                    )
                }
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                val fraction = if (valueRange.endInclusive > valueRange.start) {
                    ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                } else 0f

                val trackY = center.y - (trackHeight.dp.toPx() / 2f)
                val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

                // Track de Fundo (Inativo)
                drawRoundRect(
                    color = if (enabled) trackColor else disabledColor.copy(alpha = 0.1f),
                    topLeft = Offset(0f, trackY),
                    size = Size(size.width, trackHeight.dp.toPx()),
                    cornerRadius = cornerRadius
                )

                // Track Preenchido (Ativo)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = if (enabled) primaryColor else disabledColor,
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width * fraction, trackHeight.dp.toPx()),
                        cornerRadius = cornerRadius
                    )
                }

                // Thumb (Bolinha interativa)
                val thumbX = (size.width * fraction).coerceIn(thumbRadius.dp.toPx(), size.width - thumbRadius.dp.toPx())
                if (enabled) {
                    // Sombra do thumb quando dragging
                    if (isDragging) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.2f),
                            radius = thumbRadius.dp.toPx() * 2.5f,
                            center = Offset(thumbX, center.y)
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius.dp.toPx(),
                        center = Offset(thumbX, center.y)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = thumbRadius.dp.toPx(),
                        center = Offset(thumbX, center.y),
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = disabledColor,
                        radius = thumbRadius.dp.toPx(),
                        center = Offset(thumbX, center.y)
                    )
                }
            }
        }
    }
}
