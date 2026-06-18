package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.toUiModel

@Composable
fun BeachPalmGraphic(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(width = 64.dp, height = 64.dp)
    ) {
        val w = size.width
        val h = size.height

        // 1. Warm Golden Island Sand
        val sandPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            quadraticTo(w * 0.4f, h * 0.65f, w, h * 0.85f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = sandPath,
            color = Color(0xFFFEF08A) // warm yellow orange-100 sand island
        )

        // 2. Cyan Water Wave
        val waterPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.35f, h)
            quadraticTo(w * 0.65f, h * 0.8f, w, h * 0.92f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = waterPath,
            color = Color(0xFF38BDF8) // sky-400 blue water
        )

        // 3. Main Palm Tree (Right)
        // Trunk
        val trunkPath1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.68f, h * 0.88f)
            quadraticTo(w * 0.62f, h * 0.52f, w * 0.76f, h * 0.34f)
            quadraticTo(w * 0.82f, h * 0.52f, w * 0.74f, h * 0.88f)
            close()
        }
        drawPath(trunkPath1, color = Color(0xFFB45309)) // Amber-700 brown

        // Leaves of Main Tree
        drawCircle(Color(0xFF16A34A), radius = w * 0.12f, center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.34f))
        drawCircle(Color(0xFF22C55E), radius = w * 0.10f, center = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.30f))
        drawCircle(Color(0xFF4ADE80), radius = w * 0.10f, center = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.38f))
        drawCircle(Color(0xFF15803D), radius = w * 0.08f, center = androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.22f))

        // 4. Secondary Palm Tree (Left)
        // Trunk
        val trunkPath2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.44f, h * 0.92f)
            quadraticTo(w * 0.38f, h * 0.74f, w * 0.46f, h * 0.60f)
            quadraticTo(w * 0.50f, h * 0.74f, w * 0.48f, h * 0.92f)
            close()
        }
        drawPath(trunkPath2, color = Color(0xFF92400E)) // Amber-800 brown

        // Leaves of Secondary Tree
        drawCircle(Color(0xFF15803D), radius = w * 0.09f, center = androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.60f))
        drawCircle(Color(0xFF16A34A), radius = w * 0.07f, center = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.56f))
        drawCircle(Color(0xFF86EFAC), radius = w * 0.07f, center = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.64f))
    }
}

@Composable
fun BlueAndroidWidget(
    title: String,
    number: String,
    units: String,
    date: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = 220.dp)
            .fillMaxWidth()
            .height(136.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4E91F9), Color(0xFF3575E2))
                    )
                )
                .padding(14.dp)
        ) {
            // Refresh icon top right
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .testTag("widget_preview_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Atualizar",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Left: Diagonal airplane icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Filled.Flight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-45f)
                )
            }

            // Right side column containing event information
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = number,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 42.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = units,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val displayTitle = if (title.startsWith("Viagem", ignoreCase = true)) {
                    "para a viagem \uD83C\uDFDD️"
                } else {
                    "para ${title.lowercase()} \uD83C\uDF89"
                }
                
                Text(
                    text = displayTitle,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.testTag("blue_widget_title")
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = date,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        fontWeight = FontWeight.Light
                    )
                )
            }
        }
    }
}

@Composable
fun LightThemeWidget(
    title: String,
    number: String,
    units: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(116.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            // Moon crescent icon in light lavender-grey top-right
            Icon(
                imageVector = Icons.Filled.NightsStay,
                contentDescription = null,
                tint = Color(0xFFA5B4FC), // Lavender indigo
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            )

            // Content Column
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)
            ) {
                // Top caption "Viagem para Salvador" in lavender
                Text(
                    text = title,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6CF1) // Custom gorgeous lavender color
                    ),
                    maxLines = 1,
                    modifier = Modifier.testTag("light_widget_title")
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = number,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6F6CF1)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = units,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B88FF)
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = date,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 10.sp,
                        color = Color(0xFF9E9EFC),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Beach island bottom-right vector
            BeachPalmGraphic(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun DarkThemeWidget(
    title: String,
    number: String,
    units: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(116.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141923))
                .padding(12.dp)
        ) {
            // Moon crescent icon in white top-right
            Icon(
                imageVector = Icons.Filled.NightsStay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            )

            // Content Column
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)
            ) {
                // Top caption "Viagem para Salvador" in white
                Text(
                    text = title,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    modifier = Modifier.testTag("dark_widget_title")
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = number,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = units,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = date,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            // Beach island bottom-right vector
            BeachPalmGraphic(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun WidgetsPreviewSelector(
    allEvents: List<Event>,
    modifier: Modifier = Modifier
) {
    val uiEvents = remember(allEvents) { allEvents.map { it.toUiModel() } }
    var selectedIndex by remember { mutableStateOf(-1) }
    
    val fallbackTitle = "Viagem para Salvador"
    val fallbackNumber = "8"
    val fallbackUnits = "dias"
    val fallbackDate = "24 de maio de 2024"
    
    val (title, number, units, date) = remember(selectedIndex, uiEvents) {
        if (selectedIndex >= 0 && selectedIndex < uiEvents.size) {
            val event = uiEvents[selectedIndex]
            val dispTitle = if (event.title.contains("Salvador", ignoreCase = true)) "Viagem para Salvador" else event.title
            val dispUnits = if (event.units.equals("automático", ignoreCase = true)) "dias" else event.units
            Quadruple(dispTitle, event.number, dispUnits, event.date)
        } else {
            Quadruple(fallbackTitle, fallbackNumber, fallbackUnits, fallbackDate)
        }
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Android Widget Section (Left)
                Box(
                    modifier = Modifier.weight(1.1f)
                ) {
                    AndroidWidgetPanel(
                        title = title,
                        number = number,
                        units = units,
                        date = date,
                        onRefresh = {
                            if (uiEvents.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % uiEvents.size
                            }
                        }
                    )
                }

                // Themes Section (Right)
                Box(
                    modifier = Modifier.weight(1.9f)
                ) {
                    ThemesPanel(
                        title = title,
                        number = number,
                        units = units,
                        date = date
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AndroidWidgetPanel(
                    title = title,
                    number = number,
                    units = units,
                    date = date,
                    onRefresh = {
                        if (uiEvents.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1) % uiEvents.size
                        }
                    }
                )

                ThemesPanel(
                    title = title,
                    number = number,
                    units = units,
                    date = date
                )
            }
        }

        // Chip selection row
        if (uiEvents.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Personalize com seus dados ativos:",
                    style = AppTypography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Quick Reset / Mock default option first
                    item {
                        InputChip(
                            selected = selectedIndex == -1,
                            onClick = { selectedIndex = -1 },
                            label = { Text("Padrão (Fiel à Imagem)") },
                            modifier = Modifier.testTag("chip_default_mockup")
                        )
                    }
                    
                    items(uiEvents.size) { index ->
                        val ev = uiEvents[index]
                        InputChip(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            label = { Text(ev.title) },
                            modifier = Modifier.testTag("chip_user_event_$index")
                        )
                    }
                }
            }
        } else {
            Text(
                text = "💡 Dica: Crie novos eventos na tela principal para poder visualizá-los aqui em tempo real!",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AndroidWidgetPanel(
    title: String,
    number: String,
    units: String,
    date: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp) // Leave space for fieldset title overlap
    ) {
        // Enclosing simulation container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(24.dp))
                .padding(vertical = 24.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BlueAndroidWidget(
                title = title,
                number = number,
                units = units,
                date = date,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth().widthIn(max = 240.dp)
            )
        }

        // Title at the top center/start, covering the border
        Text(
            text = "Widget do Android",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun ThemesPanel(
    title: String,
    number: String,
    units: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Temas",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LightThemeWidget(
                    title = title,
                    number = number,
                    units = units,
                    date = date,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DarkThemeWidget(
                    title = title,
                    number = number,
                    units = units,
                    date = date,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Simple Quadruple helper class
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
