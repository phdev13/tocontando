package com.phdev.quantofalta.feature.createevent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorPickerSheet(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentColor by remember { mutableStateOf(initialColor) }
    var hexInput by remember { mutableStateOf(colorToHex(initialColor)) }

    // HSV representation for sliders
    var hsvArray by remember { mutableStateOf(colorToHSV(initialColor)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium)
                .padding(bottom = AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Text(
                text = "Cor Personalizada",
                style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preview and Hex Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                )

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        hexInput = newValue.uppercase()
                        val parsedColor = hexToColorOrNull(hexInput)
                        if (parsedColor != null) {
                            currentColor = parsedColor
                            hsvArray = colorToHSV(parsedColor)
                        }
                    },
                    label = { Text("Hexadecimal") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    prefix = { Text("#") }
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))

            // Hue Slider
            val hueColors = remember {
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(60f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(120f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(180f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(240f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(300f, 1f, 1f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(360f, 1f, 1f)))
                )
            }
            Text("Matiz (Cor)", style = AppTypography.labelMedium)
            ColorSlider(
                value = hsvArray[0],
                onValueChange = {
                    hsvArray = floatArrayOf(it, hsvArray[1], hsvArray[2])
                    currentColor = Color(android.graphics.Color.HSVToColor(hsvArray))
                    hexInput = colorToHex(currentColor)
                },
                valueRange = 0f..360f,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(hueColors)
            )

            // Saturation Slider
            val satColors = remember(hsvArray[0], hsvArray[2]) {
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hsvArray[0], 0f, hsvArray[2]))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hsvArray[0], 1f, hsvArray[2])))
                )
            }
            Text("Saturação", style = AppTypography.labelMedium)
            ColorSlider(
                value = hsvArray[1],
                onValueChange = {
                    hsvArray = floatArrayOf(hsvArray[0], it, hsvArray[2])
                    currentColor = Color(android.graphics.Color.HSVToColor(hsvArray))
                    hexInput = colorToHex(currentColor)
                },
                valueRange = 0f..1f,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(satColors)
            )

            // Value Slider
            val valColors = remember(hsvArray[0], hsvArray[1]) {
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hsvArray[0], hsvArray[1], 0f))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hsvArray[0], hsvArray[1], 1f)))
                )
            }
            Text("Luminosidade", style = AppTypography.labelMedium)
            ColorSlider(
                value = hsvArray[2],
                onValueChange = {
                    hsvArray = floatArrayOf(hsvArray[0], hsvArray[1], it)
                    currentColor = Color(android.graphics.Color.HSVToColor(hsvArray))
                    hexInput = colorToHex(currentColor)
                },
                valueRange = 0f..1f,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(valColors)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Button(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirmar Cor", style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("%06X", 0xFFFFFF and argb)
}

private fun colorToHSV(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

private fun hexToColorOrNull(hex: String): Color? {
    return try {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    brush: androidx.compose.ui.graphics.Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(brush)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                thumbColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
