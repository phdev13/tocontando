package com.phdev.quantofalta.feature.finance


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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.allows
import com.phdev.quantofalta.core.designsystem.components.AppTextField
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.edit.CoverPhotoEditorScreen
import com.phdev.quantofalta.core.designsystem.components.edit.EditAppearanceSection
import com.phdev.quantofalta.core.designsystem.components.edit.EditCardLayout
import com.phdev.quantofalta.core.designsystem.components.edit.EditFieldRow
import com.phdev.quantofalta.core.designsystem.components.edit.EditToggleRow
import com.phdev.quantofalta.core.designsystem.components.edit.SalaryCardStyleSelector
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.utils.ImageStorageHelper
import com.phdev.quantofalta.core.utils.rememberCoverImagePicker
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.feature.createevent.CustomColorPickerSheet
import com.phdev.quantofalta.feature.standard.IconPickerDialog

@Composable
fun CreateSalaryScreen(
    eventId: String? = null,
    viewModel: SalaryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showFullIconPicker by remember { mutableStateOf(false) }
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }
    var coverImageToEdit by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val pickCoverImage = rememberCoverImagePicker(
        isPremium = isPremium,
        onImageReady = { coverImageToEdit = it }
    )

    LaunchedEffect(eventId) {
        if (eventId != null) {
            viewModel.loadForEdit(eventId)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    if (showCustomColorPicker) {
        CustomColorPickerSheet(
            initialColor = Color(uiState.colorArgb),
            onColorSelected = { color ->
                viewModel.updateColor(color.toArgb())
                showCustomColorPicker = false
            },
            onDismiss = { showCustomColorPicker = false }
        )
    }

    if (showFullIconPicker) {
        IconPickerDialog(
            currentSelection = uiState.iconName,
            isPremium = isPremium,
            onDismiss = { showFullIconPicker = false },
            onIconSelected = { iconName ->
                viewModel.updateIcon(iconName)
                showFullIconPicker = false
            },
            onFeatureLocked = { 
                showFullIconPicker = false
                premiumFeatureToExplain = PremiumFeatureInfo(
                    title = "Todos os Ícones",
                    description = "Escolha entre centenas de ícones para representar qualquer momento especial.",
                    icon = Icons.Filled.Star
                )
            }
        )
    }

    coverImageToEdit?.let { sourceUri ->
        androidx.activity.compose.BackHandler {
            coverImageToEdit = null
        }
        val editorPreview = Event(
            id = "",
            title = uiState.title.ifBlank { "Recebimento" },
            iconName = uiState.iconName,
            colorArgb = uiState.colorArgb,
            targetDate = java.time.LocalDate.now().plusDays(10),
            targetTime = java.time.LocalTime.of(9, 0),
            zoneId = java.time.ZoneId.systemDefault().id,
            referenceDate = null,
            format = com.phdev.quantofalta.domain.model.CountdownFormat.DAYS,
            direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
            createdAtMillis = System.currentTimeMillis(),
            coverImageUri = sourceUri,
            type = com.phdev.quantofalta.domain.model.EventType.SALARY,
            salaryFrequency = uiState.frequency,
            salaryPaymentDay = uiState.paymentDay.toIntOrNull() ?: 5,
            salaryPaymentDateEpochDay = uiState.paymentDateEpochDay,
            salaryCustomIntervalDays = uiState.customIntervalDays.toIntOrNull() ?: 30,
            salaryWeekendRule = uiState.weekendRule,
            salaryShowBusinessDays = uiState.showBusinessDays,
            salaryValue = uiState.salaryValue.toDoubleOrNull(),
            salaryModeStyle = uiState.salaryCardStyle,
            salaryGoalTarget = uiState.salaryGoalTarget.replace(",", ".").toDoubleOrNull(),
            salaryCustomPhrase = uiState.salaryCustomPhrase.takeIf { it.isNotBlank() }
        ).toUiModel(context = context)

        CoverPhotoEditorScreen(
            sourceUri = sourceUri,
            cardTitle = editorPreview.title,
            cardSubtitle = "Pagamento",
            cardIconName = editorPreview.iconName,
            cardPrimaryText = editorPreview.primaryText,
            cardSecondaryText = editorPreview.secondaryText,
            accentColor = Color(uiState.colorArgb),
            onPickPhoto = { pickCoverImage() },
            onRemovePhoto = {
                viewModel.updateCoverImage(null)
            },
            onDismiss = { coverImageToEdit = null },
            onSave = { editedUri ->
                viewModel.updateCoverImage(editedUri)
                coverImageToEdit = null
            }
        )
        return
    }

    EditCardLayout(
        title = if (eventId == null) "Modo Finanças" else "Editar Finanças",
        onBack = onNavigateBack,
        isSaving = uiState.isLoading,
        isValid = uiState.title.isNotBlank() && uiState.paymentDay.isNotBlank(),
        onSave = {
            if (uiState.title.isBlank()) {
                showError = true
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
            } else if (!uiState.isLoading) {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                viewModel.saveSalaryEvent(
                    eventId = eventId,
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
            val dummyEvent = Event(
                id = "",
                title = uiState.title.ifBlank { "Recebimento" },
                iconName = uiState.iconName,
                colorArgb = uiState.colorArgb,
                targetDate = java.time.LocalDate.now().plusDays(10), // Dummy para o preview
                targetTime = java.time.LocalTime.of(9, 0),
                zoneId = java.time.ZoneId.systemDefault().id,
                referenceDate = null,
                format = com.phdev.quantofalta.domain.model.CountdownFormat.DAYS,
                direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
                createdAtMillis = System.currentTimeMillis(),
                coverImageUri = uiState.coverImageUri,
                type = com.phdev.quantofalta.domain.model.EventType.SALARY,
                salaryFrequency = uiState.frequency,
                salaryPaymentDay = uiState.paymentDay.toIntOrNull() ?: 5,
                salaryPaymentDateEpochDay = uiState.paymentDateEpochDay,
                salaryCustomIntervalDays = uiState.customIntervalDays.toIntOrNull() ?: 30,
                salaryWeekendRule = uiState.weekendRule,
                salaryShowBusinessDays = uiState.showBusinessDays,
                salaryValue = uiState.salaryValue.toDoubleOrNull(),
                salaryModeStyle = uiState.salaryCardStyle,
                salaryGoalTarget = uiState.salaryGoalTarget.replace(",", ".").toDoubleOrNull(),
                salaryCustomPhrase = uiState.salaryCustomPhrase.takeIf { it.isNotBlank() }
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val dummyUiModel = dummyEvent.toUiModel(context = context)
                MainEventCard(
                    title = dummyUiModel.title,
                    date = dummyUiModel.date,
                    number = dummyUiModel.number,
                    units = dummyUiModel.units,
                    color = dummyUiModel.color,
                    iconName = dummyUiModel.iconName,
                    progress = dummyUiModel.progress,
                    contextMessage = dummyUiModel.contextMessage,
                    targetDateMillis = dummyUiModel.dateMillis,
                    coverImageUri = dummyUiModel.coverImageUri,
                    onClick = { /* preview */ }
                )
            }
        },
        basicInfoContent = {
            EditFieldRow(
                label = "Nome da Renda",
                value = uiState.title,
                onValueChange = { 
                    viewModel.updateTitle(it)
                    showError = false 
                },
                placeholder = "Ex: salário, vale, freela, aluguel",
                iconName = "Create",
                isError = showError,
                errorMessage = "O nome é obrigatório"
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Frequência de Recebimento",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    listOf("monthly" to "Mensal", "biweekly" to "Quinzenal", "weekly" to "Semanal", "custom" to "Personalizado").forEach { (key, label) ->
                        val isSelected = uiState.frequency == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppShapes.medium)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, AppShapes.medium)
                                .clickable { viewModel.updateFrequency(key) }
                                .padding(vertical = AppSpacing.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (uiState.frequency == "custom") {
                    EditFieldRow(
                        label = "Recebo a cada quantos dias?",
                        value = uiState.customIntervalDays,
                        onValueChange = { viewModel.updateCustomInterval(it) },
                        placeholder = "30",
                        iconName = "Event"
                    )
                }
            }
        },
        temporalContent = {
            if (uiState.frequency == "monthly") {
                EditFieldRow(
                    label = "DIA DO MÊS (ex: 5)",
                    value = uiState.paymentDay,
                    onValueChange = { viewModel.updatePaymentDay(it) },
                    placeholder = "5",
                    iconName = "Event"
                )
            } else {
                val currentMillis = java.time.LocalDate.ofEpochDay(uiState.paymentDateEpochDay)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                EditFieldRow(
                    label = "DATA DO PRÓXIMO PAGAMENTO",
                    value = com.phdev.quantofalta.core.time.TimeUtils.formatDate(currentMillis),
                    onValueChange = {},
                    placeholder = "Selecione a data",
                    iconName = "Event",
                    onClick = {
                        com.phdev.quantofalta.core.designsystem.components.showDatePicker(context) { newMillis ->
                            val epochDay = java.time.Instant.ofEpochMilli(newMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate().toEpochDay()
                            viewModel.updatePaymentDate(epochDay)
                        }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Regra para fim de semana",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "O que fazer se o pagamento cair no sábado ou domingo?",
                    style = AppTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    listOf("friday" to "Antecipa Sexta", "monday" to "Adia Segunda", "keep" to "Mantém").forEach { (key, label) ->
                        val isSelected = uiState.weekendRule == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(AppShapes.medium)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, AppShapes.medium)
                                .clickable { viewModel.updateWeekendRule(key) }
                                .padding(vertical = AppSpacing.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        appearanceContent = {
            EditAppearanceSection(
                coverImageUri = uiState.coverImageUri,
                onCoverImageClick = { pickCoverImage() },
                onCoverImageRemove = { viewModel.updateCoverImage(null) },
                onCoverImageEdit = { uiState.coverImageUri?.let { coverImageToEdit = it } ?: pickCoverImage() },
                selectedColorArgb = uiState.colorArgb,
                onColorSelected = { viewModel.updateColor(it) },
                onCustomColorClick = { showCustomColorPicker = true },
                selectedIconName = uiState.iconName,
                onIconSelected = { viewModel.updateIcon(it) },
                onSearchIconClick = { showFullIconPicker = true },
                isPremium = isPremium,
                onPremiumCoverLocked = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Foto de Capa",
                        description = "Personalize seus cards de evento usando suas próprias fotos de fundo para deixá-los ainda mais memoráveis.",
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
        additionalOptionsContent = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Estilo do card",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                SalaryCardStyleSelector(
                    selectedStyle = uiState.salaryCardStyle,
                    isPremium = isPremium,
                    onStyleSelected = { viewModel.updateSalaryCardStyle(it) },
                    onPremiumLocked = { style ->
                        premiumFeatureToExplain = PremiumFeatureInfo(
                            title = style.displayName,
                            description = "Este estilo adapta o modo Finanças ao jeito que você acompanha seus ciclos.",
                            icon = Icons.Filled.Star
                        )
                    }
                )
            }

            EditToggleRow(
                title = "Contar dias úteis",
                description = "Exibe o tempo restante sem contabilizar finais de semana.",
                checked = uiState.showBusinessDays,
                onCheckedChange = { viewModel.updateShowBusinessDays(it) }
            )
            EditToggleRow(
                title = "Destacar na Home",
                description = "Exibe esta contagem em tamanho grande no topo da Tela Inicial.",
                checked = uiState.isPinned,
                onCheckedChange = { viewModel.updateIsPinned(it) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Valor do recebimento (opcional)",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Privacidade total. Seus valores ficam apenas neste celular.",
                    style = AppTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppTextField(
                    value = uiState.salaryValue,
                    onValueChange = { viewModel.updateSalaryValue(it) },
                    placeholder = "Ex: 5000,00",
                    icon = getIconByName("Money")
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Meta simples (opcional)",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Uma estimativa leve por ciclo, sem virar controle financeiro.",
                    style = AppTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppTextField(
                    value = uiState.salaryGoalTarget,
                    onValueChange = { viewModel.updateSalaryGoalTarget(it) },
                    placeholder = "Ex: 1500,00",
                    icon = getIconByName("Goal")
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Frase personalizada (opcional)",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                AppTextField(
                    value = uiState.salaryCustomPhrase,
                    onValueChange = { viewModel.updateSalaryCustomPhrase(it) },
                    placeholder = "Ex: Um ciclo de cada vez.",
                    icon = getIconByName("Edit")
                )
            }
        },
        auxiliaryMessage = "As informações do seu salário serão atualizadas imediatamente após salvar."
    )

    premiumFeatureToExplain?.let { info ->
        PremiumFeatureModal(
            feature = info,
            onDismiss = { premiumFeatureToExplain = null },
            onNavigatePremium = {
                premiumFeatureToExplain = null
                onNavigateToPremium()
            }
        )
    }

    uiState.error?.let {
        LaunchedEffect(it) {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}
