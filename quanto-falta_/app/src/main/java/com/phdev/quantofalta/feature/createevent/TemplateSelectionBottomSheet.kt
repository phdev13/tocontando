package com.phdev.quantofalta.feature.createevent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

data class EventTemplate(
    val title: String,
    val iconName: String,
    val colorHex: String,
    val displayName: String
)

val defaultTemplates = listOf(
    EventTemplate("Viagem para ", "Airplane", "#1E88E5", "Viagem"),
    EventTemplate("Aniversário de ", "Cake", "#E91E63", "Aniversário"),
    EventTemplate("Show de ", "Headphones", "#F44336", "Show"),
    EventTemplate("Formatura", "School", "#9C27B0", "Formatura"),
    EventTemplate("Prova de ", "School", "#FF9800", "Prova"),
    EventTemplate("Férias", "Beach", "#00BCD4", "Férias"),
    EventTemplate("Campeonato", "Trophy", "#FFC107", "Campeonato"),
    EventTemplate("Reunião", "Coffee", "#4CAF50", "Reunião"),
    EventTemplate("Lançamento", "Star", "#673AB7", "Lançamento")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionBottomSheet(
    isPremium: Boolean,
    standardLimitReached: Boolean,
    relationshipLimitReached: Boolean,
    salaryLimitReached: Boolean,
    onLimitReached: () -> Unit,
    onDismiss: () -> Unit,
    onTemplateSelected: (String, String, String) -> Unit,
    onCustomSelected: () -> Unit,
    onRelationshipSelected: () -> Unit,
    onSalarySelected: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        com.phdev.quantofalta.core.designsystem.components.AdaptiveContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(horizontal = AppSpacing.large)
                .padding(bottom = AppSpacing.huge)
        ) {
            Text(
                text = "Novo Evento",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Text(
                text = "Escolha um template ou crie do zero.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            // Custom Event Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (!isPremium && standardLimitReached) {
                            onLimitReached()
                        } else {
                            onCustomSelected() 
                        }
                    },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName("Add"),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.medium))
                    Text(
                        text = "Evento Personalizado",
                        style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!isPremium && standardLimitReached) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Limite atingido",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (!isPremium && relationshipLimitReached) {
                            onLimitReached()
                        } else {
                            onRelationshipSelected() 
                        }
                    },
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName("Favorite"),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.medium))
                    Column {
                        Text(
                            text = "Relacionamento",
                            style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Namoro, casamento, noivado ou amizade",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                    if (!isPremium && relationshipLimitReached) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Limite atingido",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            // Modo Salário
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (!isPremium && salaryLimitReached) {
                            onLimitReached()
                        } else {
                            onDismiss()
                            onSalarySelected() 
                        }
                    },
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName("AttachMoney"),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.medium))
                    Column {
                        Text(
                            text = "Modo Salário",
                            style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Acompanhe quanto falta para o pagamento",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                        )
                    }
                    if (!isPremium && salaryLimitReached) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Limite atingido",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))
            Text(
                text = "Sugestões Rápidas",
                style = AppTypography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(AppSpacing.medium))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
            ) {
                items(defaultTemplates) { template ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(template.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                if (!isPremium && standardLimitReached) {
                                    onLimitReached()
                                } else {
                                    onTemplateSelected(template.title, template.colorHex, template.iconName)
                                }
                            }
                            .padding(AppSpacing.small),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconByName(template.iconName),
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            if (!isPremium && standardLimitReached) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        Text(
                            text = template.displayName,
                            style = AppTypography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
            }
        }
    }
}
