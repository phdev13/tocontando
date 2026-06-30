package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.allows
import com.phdev.quantofalta.core.designsystem.components.PremiumLockedWrapper
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.getIconDisplayName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.Colors

// Localized Color Names MAP
private val ColorLabelMap = mapOf(
    Color(0xFF7B61FF) to "Lilás Real",
    Color(0xFF4A90FF) to "Azul Celeste",
    Color(0xFF34C759) to "Verde Esmeralda",
    Color(0xFFFF9F0A) to "Abóbora / Laranja",
    Color(0xFFFF453A) to "Cereja / Vermelho",
    Color(0xFFFF375F) to "Rosa Choque",
    Color(0xFF32ADE6) to "Ciano Neon"
)

private fun getColorLabel(color: Color): String {
    return ColorLabelMap[color] ?: "Cor customizada"
}

@Composable
fun EditAppearanceSection(
    coverImageUri: String?,
    onCoverImageClick: () -> Unit,
    onCoverImageRemove: () -> Unit,
    onCoverImageEdit: (() -> Unit)? = null,
    
    selectedColorArgb: Int,
    onColorSelected: (Int) -> Unit,
    onCustomColorClick: () -> Unit,
    
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    onSearchIconClick: () -> Unit,
    
    isPremium: Boolean,
    onPremiumCoverLocked: () -> Unit,
    onPremiumColorLocked: () -> Unit,
    onPremiumIconLocked: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val selectedColor = Color(selectedColorArgb)
    val canUseCoverPhoto = isPremium.allows(PremiumFeature.COVER_PHOTO)

    // Foto de Capa – compact single row regardless of whether a photo is selected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), AppShapes.medium)
            .clickable {
                if (canUseCoverPhoto) {
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                    if (coverImageUri != null) {
                        (onCoverImageEdit ?: onCoverImageClick)()
                    } else {
                        onCoverImageClick()
                    }
                } else {
                    onPremiumCoverLocked()
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (coverImageUri != null) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppShapes.small)
            ) {
                AsyncImage(
                    model = coverImageUri,
                    contentDescription = "Foto de capa",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Foto de capa",
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toque para ajustar recorte, zoom e formato",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onCoverImageClick) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Trocar",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            IconButton(
                onClick = onCoverImageRemove,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remover foto",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Icon(
                imageVector = if (canUseCoverPhoto) Icons.Filled.Add else Icons.Filled.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Adicionar foto de capa",
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (canUseCoverPhoto) "Escolha uma foto e ajuste zoom, corte e rotação" else "Recurso Premium",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!canUseCoverPhoto) {
                Icon(Icons.Filled.Lock, contentDescription = "Premium", tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Seletor de cores
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Cor Principal",
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Colors.forEach { color ->
                val isSelected = selectedColor == color
                val scaleFactor by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1.0f,
                    animationSpec = tween(120),
                    label = "scale"
                )
                val borderAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0.6f else 0.0f,
                    animationSpec = tween(120),
                    label = "border"
                )

                val isLocked = false // Cores liberadas no Free

                PremiumLockedWrapper(
                    isLocked = isLocked,
                    showBadge = false,
                    onClick = {
                        if (isLocked) {
                            onPremiumColorLocked()
                        } else {
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                            onColorSelected(color.toArgb())
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = scaleFactor
                                scaleY = scaleFactor
                            }
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = borderAlpha),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = com.phdev.quantofalta.core.designsystem.theme.ContrastUtils.getContrastColor(color),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Botão '+' para cores personalizadas
            PremiumLockedWrapper(
                isLocked = !isPremium,
                showBadge = false,
                onClick = {
                    if (!isPremium) {
                        onPremiumColorLocked()
                    } else {
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                        onCustomColorClick()
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Cor personalizada",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Seletor de ícones recomendados
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Ícone do Evento",
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val popularIcons = listOf("Airplane", "Favorite", "TrendingUp", "Music", "Cake", "ShoppingCart", "School", "Home")
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            popularIcons.forEach { iconName ->
                val isSelected = selectedIconName == iconName
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(120),
                    label = "pIconScale"
                )
                val iconBg by animateColorAsState(
                    targetValue = if (isSelected) selectedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    animationSpec = tween(120),
                    label = "pIconBg"
                )
                val iconBorderColor by animateColorAsState(
                    targetValue = if (isSelected) selectedColor else Color.Transparent,
                    animationSpec = tween(120),
                    label = "pIconBorder"
                )

                val isLocked = !isPremium && iconName !in com.phdev.quantofalta.billing.PremiumPolicy.freeIcons

                PremiumLockedWrapper(
                    isLocked = isLocked,
                    showBadge = false,
                    onClick = {
                        if (isLocked) {
                            onPremiumIconLocked()
                        } else {
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                            onIconSelected(iconName)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                            .clip(AppShapes.small)
                            .background(iconBg)
                            .border(1.5.dp, iconBorderColor, AppShapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = getIconDisplayName(iconName),
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Pesquisa global de ícones
        TextButton(
            onClick = onSearchIconClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Pesquisar mais ícones...",
                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
