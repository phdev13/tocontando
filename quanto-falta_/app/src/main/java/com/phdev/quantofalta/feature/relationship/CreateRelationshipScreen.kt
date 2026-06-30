@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.phdev.quantofalta.feature.relationship



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.allows
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.edit.CoverPhotoEditorScreen
import com.phdev.quantofalta.core.designsystem.components.edit.EditAppearanceSection
import com.phdev.quantofalta.core.designsystem.components.edit.EditCardLayout
import com.phdev.quantofalta.core.designsystem.components.edit.EditFieldRow
import com.phdev.quantofalta.core.designsystem.components.edit.EditToggleRow
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import com.phdev.quantofalta.core.utils.ImageStorageHelper
import com.phdev.quantofalta.core.utils.rememberCoverImagePicker
import com.phdev.quantofalta.feature.createevent.CustomColorPickerSheet
import com.phdev.quantofalta.feature.standard.IconPickerDialog
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.EventType
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RELATIONSHIP_TYPES = listOf(
    "dating" to "Namoro",
    "married" to "Casamento",
    "engaged" to "Noivado",
    "friendship" to "Amizade",
    "other" to "Outro",
)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

@Composable
fun CreateRelationshipScreen(
    eventId: String? = null,
    viewModel: RelationshipViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var coverImageToEdit by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val pickCoverImage = rememberCoverImagePicker(
        isPremium = isPremium,
        onImageReady = { coverImageToEdit = it }
    )
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }

    LaunchedEffect(eventId) {
        if (eventId != null) viewModel.loadForEdit(eventId)
    }

    if (showDatePicker) {
        RelationshipDatePickerDialog(
            initialEpochDay = state.startEpochDay,
            onConfirm = { epochDay ->
                viewModel.onStartDateChange(epochDay)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showCustomColorPicker) {
        CustomColorPickerSheet(
            initialColor = Color(state.colorArgb),
            onColorSelected = { 
                viewModel.onColorChange(it.toArgb())
                showCustomColorPicker = false 
            },
            onDismiss = { showCustomColorPicker = false },
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentSelection = state.iconName,
            isPremium = isPremium,
            onIconSelected = {
                viewModel.onIconChange(it)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
            onFeatureLocked = {
                showIconPicker = false
                premiumFeatureToExplain = PremiumFeatureInfo(
                    title = "Todos os Ícones",
                    description = "Escolha entre centenas de ícones para representar qualquer momento especial.",
                    icon = Icons.Filled.Star
                )
            },
        )
    }

    coverImageToEdit?.let { sourceUri ->
        androidx.activity.compose.BackHandler {
            coverImageToEdit = null
        }
        val startDate = LocalDate.ofEpochDay(state.startEpochDay)
        val editorPreview = Event(
            id = "",
            title = state.name.ifBlank { "Relacionamento" },
            iconName = state.iconName,
            colorArgb = state.colorArgb,
            targetDate = startDate,
            targetTime = java.time.LocalTime.now(),
            zoneId = java.time.ZoneId.systemDefault().id,
            referenceDate = null,
            format = CountdownFormat.DAYS,
            direction = CountdownDirection.ELAPSED,
            createdAtMillis = System.currentTimeMillis(),
            coverImageUri = sourceUri,
            type = EventType.RELATIONSHIP,
            relationshipType = state.type,
            relationshipStartEpochDay = state.startEpochDay,
            relationshipMonthlyEnabled = state.monthlyEnabled,
            relationshipAnnualEnabled = state.annualEnabled,
            relationshipMilestonesEnabled = state.milestonesEnabled,
            relationshipModeStyle = state.relationshipCardStyle
        ).toUiModel(context = context)

        CoverPhotoEditorScreen(
            sourceUri = sourceUri,
            cardTitle = editorPreview.title,
            cardSubtitle = RELATIONSHIP_TYPES.firstOrNull { it.first == state.type }?.second,
            cardIconName = editorPreview.iconName,
            cardPrimaryText = editorPreview.primaryText,
            cardSecondaryText = editorPreview.secondaryText,
            accentColor = Color(state.colorArgb),
            onPickPhoto = { pickCoverImage() },
            onRemovePhoto = {
                viewModel.onCoverImageChange(null)
            },
            onDismiss = { coverImageToEdit = null },
            onSave = { editedUri ->
                viewModel.onCoverImageChange(editedUri)
                coverImageToEdit = null
            }
        )
        return
    }

    EditCardLayout(
        title = if (eventId == null) "Novo Relacionamento" else "Editar Relacionamento",
        onBack = onBack,
        isSaving = state.isSaving,
        isValid = state.name.isNotBlank(),
        onSave = {
            if (state.name.isBlank()) {
                showError = true
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
            } else if (!state.isSaving) {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                viewModel.save(
                    existingEventId = eventId,
                    onSuccess = onBack,
                    onLimitReached = {
                        premiumFeatureToExplain = PremiumFeatureInfo(
                            title = "Eventos ilimitados",
                            description = "O plano gratuito permite até 5 eventos ativos para esta categoria.",
                            icon = androidx.compose.material.icons.Icons.Filled.Star
                        )
                    }
                )
            }
        },
        previewContent = {
            val startDate = LocalDate.ofEpochDay(state.startEpochDay)
            val dummyEvent = Event(
                id = "",
                title = state.name.ifBlank { "Relacionamento" },
                iconName = state.iconName,
                colorArgb = state.colorArgb,
                targetDate = startDate,
                targetTime = java.time.LocalTime.now(),
                zoneId = java.time.ZoneId.systemDefault().id,
                referenceDate = null,
                format = CountdownFormat.DAYS,
                direction = CountdownDirection.ELAPSED,
                createdAtMillis = System.currentTimeMillis(),
                coverImageUri = state.coverImageUri,
                type = EventType.RELATIONSHIP,
                relationshipType = state.type,
                relationshipStartEpochDay = state.startEpochDay,
                relationshipMonthlyEnabled = state.monthlyEnabled,
                relationshipAnnualEnabled = state.annualEnabled,
                relationshipMilestonesEnabled = state.milestonesEnabled,
                relationshipModeStyle = state.relationshipCardStyle
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val dummyUiModel = dummyEvent.toUiModel(context = context)
                com.phdev.quantofalta.core.designsystem.components.RelationshipFeaturedCard(
                    event = dummyUiModel,
                    onClick = { /* preview */ }
                )
            }
        },
        basicInfoContent = {
            EditFieldRow(
                label = "Nome",
                value = state.name,
                onValueChange = {
                    viewModel.onNameChange(it)
                    showError = false
                },
                placeholder = "Ex: Eu e Ana",
                iconName = "Create",
                isError = showError,
                errorMessage = "O nome é obrigatório"
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tipo",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RELATIONSHIP_TYPES.forEach { (key, label) ->
                        val selected = state.type == key
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onTypeChange(key) },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary.copy(alpha = 0.15f),
                                selectedLabelColor = PurplePrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selected) PurplePrimary else MaterialTheme.colorScheme.outline,
                                selectedBorderColor = PurplePrimary,
                                enabled = true,
                                selected = selected
                            )
                        )
                    }
                }
            }
        },
        temporalContent = {
            EditFieldRow(
                label = "Data de início",
                value = LocalDate.ofEpochDay(state.startEpochDay).format(DATE_FORMATTER),
                onValueChange = {},
                placeholder = "Selecione a data",
                iconName = "CalendarMonth",
                onClick = { showDatePicker = true }
            )
        },
        appearanceContent = {
            EditAppearanceSection(
                coverImageUri = state.coverImageUri,
                onCoverImageClick = { pickCoverImage() },
                onCoverImageRemove = { viewModel.onCoverImageChange(null) },
                onCoverImageEdit = { state.coverImageUri?.let { coverImageToEdit = it } ?: pickCoverImage() },
                selectedColorArgb = state.colorArgb,
                onColorSelected = { viewModel.onColorChange(it) },
                onCustomColorClick = { showCustomColorPicker = true },
                selectedIconName = state.iconName,
                onIconSelected = { viewModel.onIconChange(it) },
                onSearchIconClick = { showIconPicker = true },
                isPremium = isPremium,
                onPremiumCoverLocked = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Foto de Capa",
                        description = "Personalize o card do seu relacionamento usando suas próprias fotos de fundo para deixá-lo ainda mais memorável.",
                        icon = Icons.Filled.Image
                    )
                },
                onPremiumColorLocked = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Cores Premium",
                        description = "Desbloqueie nossa paleta completa de cores ou crie a sua própria.",
                        icon = Icons.Filled.ColorLens
                    )
                },
                onPremiumIconLocked = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Todos os Ícones",
                        description = "Escolha entre centenas de ícones para representar qualquer momento especial.",
                        icon = Icons.Filled.Star
                    )
                }
            )
        },
        remindersContent = {
            EditToggleRow(
                title = "Mensário",
                description = "Lembrete no mesmo dia de cada mês",
                checked = state.monthlyEnabled,
                onCheckedChange = viewModel::onMonthlyToggle
            )
            EditToggleRow(
                title = "Aniversário anual",
                description = "Lembrete a cada ano completo",
                checked = state.annualEnabled,
                onCheckedChange = viewModel::onAnnualToggle
            )
            EditToggleRow(
                title = "Marcos automáticos",
                description = "Notificar nos marcos (30, 100, 365 dias...)",
                checked = state.milestonesEnabled,
                onCheckedChange = viewModel::onMilestonesToggle
            )
        },
        additionalOptionsContent = {
            EditToggleRow(
                title = "Mostrar na Home",
                description = "Exibir este relacionamento na tela inicial",
                checked = state.showOnHome,
                onCheckedChange = viewModel::onShowOnHomeToggle
            )
            EditToggleRow(
                title = "Destacar na Home",
                description = "Inclui este relacionamento no carrossel principal",
                checked = state.isHighlighted,
                onCheckedChange = viewModel::onHighlightedToggle
            )
        }
    )

    premiumFeatureToExplain?.let { feature ->
        PremiumFeatureModal(
            feature = feature,
            onNavigatePremium = {
                premiumFeatureToExplain = null
            },
            onDismiss = { premiumFeatureToExplain = null }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelationshipDatePickerDialog(
    initialEpochDay: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay * 86_400_000L,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onConfirm(millis / 86_400_000L)
                }
            ) { Text("OK", color = PurplePrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(
            state = state,
            headline = { Text("Data de início", modifier = Modifier.padding(start = 24.dp)) },
            title = null,
        )
    }
}
