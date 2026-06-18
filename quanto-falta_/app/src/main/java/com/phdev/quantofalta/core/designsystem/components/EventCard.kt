package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.config.AppConfigManager
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

/**
 * O card principal da Home — exibe o próximo evento em destaque.
 * Layout: gradiente de cor do evento, número grande à esquerda,
 * ícone decorativo grande flutuando no canto superior direito.
 */
@Composable
fun MainEventCard(
    title: String,
    date: String,
    number: String,
    units: String,
    color: Color,
    iconName: String,
    progress: Float = 0f,
    contextMessage: String = "",
    isToday: Boolean = false,
    targetDateMillis: Long,
    coverImageUri: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPremiumCardsEnabled = AppConfigManager.isPremiumCardsEnabled(context)
    val hasPhoto = isPremiumCardsEnabled && coverImageUri != null
    // Floating animation for the decorative icon
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val baseTextColor = com.phdev.quantofalta.core.designsystem.theme.ContrastUtils.getContrastColor(color)
    val textColor = if (hasPhoto) Color.White else baseTextColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color,
                            color.copy(
                                red = (color.red * 0.75f).coerceAtMost(1f),
                                green = (color.green * 0.75f).coerceAtMost(1f),
                                blue = (color.blue * 1.25f).coerceAtMost(1f)
                            )
                        )
                    )
                )
        ) {
            var currentMillis by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1000L)
                    currentMillis = System.currentTimeMillis()
                }
            }
            
            val diff = (targetDateMillis - currentMillis).coerceAtLeast(0)
            val days = diff / (1000 * 60 * 60 * 24)
            val hours = (diff / (1000 * 60 * 60)) % 24
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / 1000) % 60
            
            // Render the cover photo or the color gradient
            if (isPremiumCardsEnabled && coverImageUri != null) {
                AsyncImage(
                    model = coverImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapes.large)
                )
                // Color gradient overlay using the selected color for a beautiful and readable blend
                val darkOverlayColor = Color(
                    red = color.red * 0.35f,
                    green = color.green * 0.35f,
                    blue = color.blue * 0.35f,
                    alpha = 0.95f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapes.large)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    darkOverlayColor
                                )
                            )
                        )
                )
            } else {
                // Huge background watermark with elegant breathing animation
                val bgScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bgScale"
                )
                
                Icon(
                    imageVector = getIconByName(iconName),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 40.dp, y = 40.dp)
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = -15f // Fixed, elegant angle
                            scaleX = bgScale
                            scaleY = bgScale
                        }
                )
            }
            // Decorative large icon — top-right, floating
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppSpacing.large, end = AppSpacing.large)
                    .graphicsLayer { translationY = offsetY }
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(iconName),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
            }

            // Text content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.large)
            ) {
                Text(
                    text = title,
                    style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = date,
                    style = AppTypography.labelMedium,
                    color = textColor.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                if (isToday) {
                    // Estado Especial: O dia chegou
                    val shimmerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shimmerAlpha"
                    )
                    Text(
                        text = "É Hoje!",
                        style = AppTypography.displayLarge.copy(
                            fontSize = 64.sp,
                            lineHeight = 64.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor.copy(alpha = shimmerAlpha)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chegou o grande dia.",
                        style = AppTypography.titleMedium,
                        color = textColor.copy(alpha = 0.9f)
                    )
                } else {
                    // Contagem Regressiva Completa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        CountdownBlock(value = days.toString(), label = "dias", textColor = textColor)
                        CountdownBlock(value = hours.toString().padStart(2, '0'), label = "horas", textColor = textColor)
                        CountdownBlock(value = minutes.toString().padStart(2, '0'), label = "min", textColor = textColor)
                        CountdownBlock(value = seconds.toString().padStart(2, '0'), label = "seg", textColor = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                if (contextMessage.isNotEmpty()) {
                    Text(
                        text = contextMessage,
                        style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Barra de Progresso Arredondada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(textColor)
                    )
                }
            }
        }
    }
}

/**
 * Card compacto para a lista de próximos eventos na Home.
 * Linha horizontal: ícone colorido arredondado | nome + número+unidade | data.
 */
@Composable
fun CompactEventCard(
    title: String,
    date: String,
    number: String,
    units: String,
    color: Color,
    iconName: String,
    targetDateMillis: Long,
    coverImageUri: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPremiumCardsEnabled = AppConfigManager.isPremiumCardsEnabled(context)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        var currentMillis by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                currentMillis = System.currentTimeMillis()
            }
        }
        
        val diff = (targetDateMillis - currentMillis).coerceAtLeast(0)
        val isDone = diff == 0L
        val summaryText = if (isDone) {
            "Concluído"
        } else {
            val d = diff / (1000 * 60 * 60 * 24)
            val h = (diff / (1000 * 60 * 60)) % 24
            val m = (diff / (1000 * 60)) % 60
            val s = (diff / 1000) % 60
            if (d > 0) "${d} dias, ${h}h" else "${h}h ${m}min ${s}s"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored icon box or Cover photo
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShapes.small)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPremiumCardsEnabled && coverImageUri != null) {
                    AsyncImage(
                        model = coverImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = getIconByName(iconName),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.medium))

            // Title + countdown + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = summaryText,
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = date,
                    style = AppTypography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun CountdownBlock(value: String, label: String, textColor: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.displayLarge.copy(
                fontSize = 48.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = textColor
        )
        Text(
            text = label,
            style = AppTypography.labelMedium,
            color = textColor.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun CompactCountdownBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = AppTypography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
