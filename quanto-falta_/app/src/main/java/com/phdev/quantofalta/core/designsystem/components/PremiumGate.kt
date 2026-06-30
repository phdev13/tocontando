package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

// ─────────────────────────────────────────────────────────────────────────────
// PremiumBadge — Small "Premium" chip used inline next to locked items
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A small, discrete "Premium" badge chip.
 * Use it as a trailing element inside rows, buttons, or cards.
 */
@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = AppShapes.pill
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "Premium",
                style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumLockIcon — Small lock icon overlay for grids/images
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A minimal lock icon, typically positioned at the top-end of a locked item
 * in a grid (icon picker, color picker, etc.).
 */
@Composable
fun PremiumLockIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Recurso Premium",
            modifier = Modifier.size(10.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumLockedWrapper — Generic wrapper that dims and overlays locked content
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps any composable [content] with a locked visual state when [isLocked] is true.
 * Applies reduced alpha and attaches an [onClick] handler.
 *
 * @param isLocked    Whether to apply the locked visual treatment.
 * @param onClick     Action when the user taps the locked content.
 * @param showBadge   Whether to show the "Premium" badge overlay.
 * @param content     The actual UI to render.
 */
@Composable
fun PremiumLockedWrapper(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .then(if (isLocked) Modifier.alpha(0.65f) else Modifier)
            .clickable(onClick = onClick)
    ) {
        content()
        if (isLocked && showBadge) {
            PremiumLockIcon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumLockedRow — Settings-style row with premium state
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A settings-style list row that shows a Premium badge and reduced alpha
 * when [isLocked] is true. Still clickable — opens [onLockedClick].
 */
@Composable
fun PremiumLockedRow(
    title: String,
    description: String,
    icon: ImageVector,
    isLocked: Boolean,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFreeClick: (() -> Unit)? = null
) {
    val contentAlpha = if (isLocked) 0.65f else 1f
    val iconTint by animateColorAsState(
        targetValue = if (isLocked)
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(200),
        label = "premiumRowIconTint"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = if (isLocked) onLockedClick else (onFreeClick ?: {}))
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(AppSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Text(
                text = description,
                style = AppTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
        if (isLocked) {
            Spacer(modifier = Modifier.width(AppSpacing.small))
            PremiumBadge()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PremiumFeatureModal — Contextual bottom sheet explaining a locked feature
// ─────────────────────────────────────────────────────────────────────────────

data class PremiumFeatureInfo(
    val title: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Star
)

/**
 * A contextual bottom sheet that appears when a Free user taps a Premium feature.
 * Explains the specific benefit and offers to navigate to the paywall.
 *
 * @param feature           Metadata about the specific locked feature.
 * @param onNavigatePremium Called when the user taps "Desbloquear Premium".
 * @param onDismiss         Called when dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeatureModal(
    feature: PremiumFeatureInfo,
    onNavigatePremium: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.large)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(AppShapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Title + Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PremiumBadge()
                Text(
                    text = feature.title,
                    style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Description
            Text(
                text = feature.description,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.small)
            )

            Spacer(modifier = Modifier.height(AppSpacing.small))

            // CTA
            Button(
                onClick = {
                    onDismiss()
                    onNavigatePremium()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Desbloquear Premium",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Agora não",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EventLimitModal — Specific modal for when the free event limit is reached
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Modal shown when a Free user tries to create more than [FREE_EVENT_LIMIT] events.
 *
 * @param onNavigatePremium Navigate to Premium paywall.
 * @param onManageEvents    Navigate back to Home so user can delete/complete an event.
 * @param onDismiss         Dismiss the modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLimitModal(
    onNavigatePremium: () -> Unit,
    onManageEvents: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.large)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(AppShapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "Limite de eventos atingido",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Você atingiu o limite de 5 eventos ativos para esta categoria no plano gratuito. Conclua ou exclua um evento para criar outro, ou desbloqueie eventos ilimitados com o Premium.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.small))

            Button(
                onClick = {
                    onDismiss()
                    onNavigatePremium()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppShapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ver Premium",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            TextButton(
                onClick = {
                    onDismiss()
                    onManageEvents()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gerenciar eventos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
