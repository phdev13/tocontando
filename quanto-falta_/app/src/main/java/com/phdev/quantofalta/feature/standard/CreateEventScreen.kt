package com.phdev.quantofalta.feature.standard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.allows
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppTextField
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.PremiumBadge
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.PremiumLockedWrapper
import com.phdev.quantofalta.core.designsystem.components.edit.EditAppearanceSection
import com.phdev.quantofalta.core.designsystem.components.edit.EditCardLayout
import com.phdev.quantofalta.core.designsystem.components.edit.EditFieldRow
import com.phdev.quantofalta.core.designsystem.components.edit.EditToggleRow
import com.phdev.quantofalta.core.designsystem.components.edit.CoverPhotoEditorScreen
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.getIconDisplayName
import com.phdev.quantofalta.core.designsystem.components.iconNamesList
import com.phdev.quantofalta.core.designsystem.components.showDatePicker
import com.phdev.quantofalta.core.designsystem.components.showTimePicker
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.Colors
import com.phdev.quantofalta.core.notifications.model.TriggerType
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.core.utils.ImageStorageHelper
import com.phdev.quantofalta.core.utils.rememberCoverImagePicker
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.feature.createevent.CustomColorPickerSheet

@Composable
fun CreateEventScreen(
    eventId: String? = null,
    prefillTitle: String? = null,
    prefillColorHex: String? = null,
    prefillIconName: String? = null,
    prefillDaysLeft: Int? = null,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: CreateEventViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val initialTitle = prefillTitle ?: ""
    val initialColor = remember(prefillColorHex) {
        prefillColorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
        } ?: Colors[0]
    }
    val initialIcon = prefillIconName ?: iconNamesList[0]
    val initialTimestamp = remember(prefillDaysLeft) {
        System.currentTimeMillis() + (prefillDaysLeft ?: 1).coerceAtLeast(0) * 24L * 60L * 60L * 1000L
    }

    var name by remember { mutableStateOf(initialTitle) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var timestamp by remember { mutableStateOf(initialTimestamp) }
    var unit by remember { mutableStateOf("DAYS") }
    var coverImageUri by remember { mutableStateOf<String?>(null) }
    var coverImageToEdit by remember { mutableStateOf<String?>(null) }
    var cardStyle by remember { mutableStateOf(com.phdev.quantofalta.domain.model.mode.StandardCardStyle.CLASSIC) }
    
    // Dialog state controllers
    var showFullIconPicker by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showUnitPicker by remember { mutableStateOf(false) }
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }
    
    var isPrivate by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var remindAtEvent by remember { mutableStateOf(true) }
    var remindOneHourBefore by remember { mutableStateOf(false) }
    var remindOneDayBefore by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionPrefs = remember {
        context.getSharedPreferences("notification_permission", android.content.Context.MODE_PRIVATE)
    }
    var isSaving by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    
    LaunchedEffect(eventId) {
        if (eventId != null) {
            val event = viewModel.getEvent(eventId)
            if (event != null) {
                name = event.title
                selectedIcon = event.iconName
                val zone = java.time.ZoneId.of(event.zoneId)
                val zdt = if (event.targetTime != null) {
                    event.targetDate.atTime(event.targetTime).atZone(zone)
                } else {
                    event.targetDate.atStartOfDay(zone)
                }
                timestamp = zdt.toInstant().toEpochMilli()
                unit = event.format.name
                isPrivate = event.isPrivate
                isPinned = event.isPinned
                coverImageUri = event.coverImageUri
                cardStyle = event.standardModeStyle
                selectedColor = Color(event.colorArgb)
                val reminders = viewModel.getReminders(event.id)
                remindAtEvent = reminders.any { it.triggerType == TriggerType.EXACT }
                remindOneHourBefore = reminders.any { it.triggerType == TriggerType.OFFSET && it.offsetMinutes == 60 }
                remindOneDayBefore = reminders.any { it.triggerType == TriggerType.OFFSET && it.offsetMinutes == 1440 }
            }
        }
    }

    val pickCoverImage = rememberCoverImagePicker(
        isPremium = isPremium.allows(PremiumFeature.COVER_PHOTO),
        onImageReady = { localUri -> coverImageToEdit = localUri }
    )
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    if (showUnitPicker) {
        UnitPickerDialog(
            currentUnit = unit,
            isPremium = isPremium,
            onUnitSelected = { selectedUnit ->
                unit = selectedUnit
                showUnitPicker = false
            },
            onDismiss = { showUnitPicker = false },
            onFeatureLocked = {
                showUnitPicker = false
                premiumFeatureToExplain = PremiumFeatureInfo(
                    title = "Formatos avançados",
                    description = "Exiba a contagem em semanas, meses, dias úteis ou tempo completo.",
                    icon = Icons.Filled.Star
                )
            }
        )
    }

    if (showCustomColorPicker) {
        CustomColorPickerSheet(
            initialColor = selectedColor,
            onColorSelected = { selectedColor = it; showCustomColorPicker = false },
            onDismiss = { showCustomColorPicker = false }
        )
    }

    if (showFullIconPicker) {
        IconPickerDialog(
            currentSelection = selectedIcon,
            isPremium = isPremium,
            onDismiss = { showFullIconPicker = false },
            onIconSelected = { selectedIcon = it; showFullIconPicker = false },
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
            title = name.ifBlank { "Meu Evento" },
            iconName = selectedIcon,
            colorArgb = selectedColor.toArgb(),
            targetDate = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            targetTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime(),
            zoneId = java.time.ZoneId.systemDefault().id,
            referenceDate = null,
            format = runCatching { CountdownFormat.valueOf(unit) }.getOrDefault(CountdownFormat.DAYS),
            direction = CountdownDirection.AUTO,
            createdAtMillis = System.currentTimeMillis(),
            coverImageUri = sourceUri,
            standardModeStyle = cardStyle,
            type = com.phdev.quantofalta.domain.model.EventType.STANDARD
        ).toUiModel(context = context)

        CoverPhotoEditorScreen(
            sourceUri = sourceUri,
            cardTitle = editorPreview.title,
            cardSubtitle = countdownFormatLabel(unit),
            cardIconName = editorPreview.iconName,
            cardPrimaryText = editorPreview.primaryText,
            cardSecondaryText = editorPreview.secondaryText,
            accentColor = selectedColor,
            onPickPhoto = { pickCoverImage() },
            onRemovePhoto = {
                coverImageUri = null
            },
            onDismiss = { coverImageToEdit = null },
            onSave = { editedUri ->
                coverImageUri = editedUri
                coverImageToEdit = null
            }
        )
        return
    }

    EditCardLayout(
        title = if (eventId == null) "Novo evento" else "Editar evento",
        onBack = onBack,
        isSaving = isSaving,
        isValid = name.isNotBlank(),
        onSave = {
            if (name.isBlank()) {
                showError = true
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
            } else if (!isSaving) {
                isSaving = true
                if (
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    !permissionPrefs.getBoolean("asked", false)
                ) {
                    permissionPrefs.edit().putBoolean("asked", true).apply()
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                viewModel.saveEvent(
                    id = eventId,
                    title = name,
                    dateMillis = timestamp,
                    unit = unit,
                    colorArgb = selectedColor.toArgb(),
                    iconName = selectedIcon,
                    isPrivate = isPrivate,
                    isPinned = isPinned,
                    coverImageUri = coverImageUri,
                    standardModeStyle = cardStyle.styleId,
                    remindAtEvent = remindAtEvent,
                    remindOneHourBefore = remindOneHourBefore,
                    remindOneDayBefore = remindOneDayBefore,
                    onSaved = onBack,
                    onLimitReached = {
                        isSaving = false
                        premiumFeatureToExplain = PremiumFeatureInfo(
                            title = "Eventos ilimitados",
                            description = "O plano gratuito permite até cinco eventos ativos.",
                            icon = Icons.Filled.Star
                        )
                    },
                    onError = {
                        isSaving = false
                        android.widget.Toast.makeText(
                            context,
                            "Não foi possível salvar o evento. Tente novamente.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        },
        previewContent = {
            val dummyUiModel = Event(
                id = "",
                title = name.ifBlank { "Meu Evento" },
                iconName = selectedIcon,
                colorArgb = selectedColor.toArgb(),
                targetDate = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                targetTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime(),
                zoneId = java.time.ZoneId.systemDefault().id,
                referenceDate = null,
                format = runCatching { CountdownFormat.valueOf(unit) }.getOrDefault(CountdownFormat.DAYS),
                direction = CountdownDirection.AUTO,
                createdAtMillis = System.currentTimeMillis(),
                coverImageUri = coverImageUri,
                standardModeStyle = cardStyle,
                type = com.phdev.quantofalta.domain.model.EventType.STANDARD
            ).toUiModel(context = context)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                    onClick = { /* Preview only */ }
                )
            }
        },
        basicInfoContent = {
            EditFieldRow(
                label = "Nome do evento",
                value = name,
                onValueChange = {
                    if (it.length <= com.phdev.quantofalta.core.validation.AppDataValidator.MAX_TITLE_LENGTH) {
                        name = it
                        showError = false
                    }
                },
                placeholder = "Viagem para Salvador",
                iconName = "Create",
                isError = showError,
                errorMessage = "O nome é obrigatório"
            )
            EditFieldRow(
                label = "Unidade de contagem",
                value = countdownFormatLabel(unit),
                onValueChange = {},
                placeholder = "Dias",
                iconName = "List",
                onClick = { showUnitPicker = true }
            )
        },
        temporalContent = {
            EditFieldRow(
                label = "Data",
                value = TimeUtils.formatDate(timestamp),
                onValueChange = {},
                placeholder = "24 de maio de 2024",
                iconName = "Event",
                onClick = { showDatePicker(context) { newDate -> timestamp = newDate } }
            )
            EditFieldRow(
                label = "Horário",
                value = TimeUtils.formatTime(timestamp),
                onValueChange = {},
                placeholder = "09:00",
                iconName = "Alarm",
                onClick = { showTimePicker(context, timestamp) { newTime -> timestamp = newTime } }
            )
        },
        appearanceContent = {
            EditAppearanceSection(
                coverImageUri = coverImageUri,
                onCoverImageClick = { pickCoverImage() },
                onCoverImageRemove = { coverImageUri = null },
                onCoverImageEdit = { coverImageUri?.let { coverImageToEdit = it } ?: pickCoverImage() },
                selectedColorArgb = selectedColor.toArgb(),
                onColorSelected = { selectedColor = Color(it) },
            onCustomColorClick = { showCustomColorPicker = true },
                selectedIconName = selectedIcon,
                onIconSelected = { selectedIcon = it },
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
        remindersContent = {
            EditToggleRow(
                title = "No horário do evento",
                description = "Receba um aviso quando a contagem chegar ao fim.",
                checked = remindAtEvent,
                onCheckedChange = { remindAtEvent = it }
            )
            EditToggleRow(
                title = "1 hora antes",
                description = "Aviso antecipado para você se preparar.",
                checked = remindOneHourBefore,
                isPremiumLocked = !isPremium,
                onPremiumLockedClick = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Lembretes antecipados",
                        description = "Receba avisos adicionais antes do horário do evento.",
                        icon = Icons.Filled.Star
                    )
                },
                onCheckedChange = { remindOneHourBefore = it }
            )
            EditToggleRow(
                title = "1 dia antes",
                description = "Lembrete no dia anterior ao evento.",
                checked = remindOneDayBefore,
                isPremiumLocked = !isPremium,
                onPremiumLockedClick = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Lembretes antecipados",
                        description = "Receba avisos adicionais antes do horário do evento.",
                        icon = Icons.Filled.Star
                    )
                },
                onCheckedChange = { remindOneDayBefore = it }
            )
        },
        additionalOptionsContent = {
            EditToggleRow(
                title = "Destacar na Home",
                description = "Exibe esta contagem em tamanho grande no topo da Tela Inicial.",
                checked = isPinned,
                onCheckedChange = { isPinned = it }
            )
            EditToggleRow(
                title = "Proteger com biometria",
                description = "Exigir digital/face ID para visualizar os detalhes deste evento.",
                checked = isPrivate,
                isPremiumLocked = !isPremium.allows(PremiumFeature.BIOMETRIC_PROTECTION),
                onPremiumLockedClick = {
                    premiumFeatureToExplain = PremiumFeatureInfo(
                        title = "Biometria",
                        description = "Proteja seus eventos secretos ou surpresas com a sua digital ou Face ID.",
                        icon = Icons.Filled.Lock
                    )
                },
                onCheckedChange = { isPrivate = it }
            )
        },
        auxiliaryMessage = "A contagem de tempo do seu evento iniciará imediatamente após salvar."
    )

    premiumFeatureToExplain?.let { info ->
        PremiumFeatureModal(
            feature = info,
            onDismiss = { premiumFeatureToExplain = null },
            onNavigatePremium = {
                premiumFeatureToExplain = null
                onNavigate(com.phdev.quantofalta.core.navigation.Screen.Premium.route)
            }
        )
    }
}

private fun countdownFormatLabel(value: String): String = when (value) {
    "FULL_TIME" -> "Tempo completo"
    "WEEKS" -> "Semanas"
    "WEEKS_AND_DAYS" -> "Semanas e dias"
    "MONTHS" -> "Meses"
    "MONTHS_AND_DAYS" -> "Meses e dias"
    "WORKING_DAYS" -> "Dias úteis"
    else -> "Dias"
}

// ICON PICKER DIALOG
@Composable
fun IconPickerDialog(
    currentSelection: String,
    isPremium: Boolean,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onFeatureLocked: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val categories = com.phdev.quantofalta.core.designsystem.components.iconCategories.keys.toList()
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(550.dp),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Escolha um Ícone",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                }
                
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Pesquisar (ex: avião, bolo)...",
                    icon = Icons.Filled.Search
                )

                if (searchQuery.isBlank()) {
                    androidx.compose.material3.ScrollableTabRow(
                        selectedTabIndex = selectedCategoryIndex,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedCategoryIndex < tabPositions.size) {
                                androidx.compose.material3.TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        categories.forEachIndexed { index, title ->
                            val isTabLocked = index > 0 && !isPremium
                            androidx.compose.material3.Tab(
                                selected = selectedCategoryIndex == index,
                                onClick = {
                                    if (isTabLocked) {
                                        onFeatureLocked()
                                    } else {
                                        selectedCategoryIndex = index
                                    }
                                },
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, style = AppTypography.labelMedium, fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal)
                                        if (isTabLocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                val iconsToShow = remember(searchQuery, selectedCategoryIndex) {
                    if (searchQuery.isNotBlank()) {
                        com.phdev.quantofalta.core.designsystem.components.iconNamesList.filter { name ->
                            val displayName = com.phdev.quantofalta.core.designsystem.components.getIconDisplayName(name)
                            displayName.contains(searchQuery, ignoreCase = true) || name.contains(searchQuery, ignoreCase = true)
                        }
                    } else {
                        val catName = categories[selectedCategoryIndex]
                        com.phdev.quantofalta.core.designsystem.components.iconCategories[catName] ?: emptyList()
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    if (iconsToShow.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Nenhum ícone correspondente",
                                style = AppTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 68.dp),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                            contentPadding = PaddingValues(bottom = AppSpacing.medium)
                        ) {
                            items(iconsToShow) { name ->
                                val isLocked = !isPremium && name !in com.phdev.quantofalta.billing.PremiumPolicy.freeIcons
                                val isSelected = currentSelection == name
                                val displayName = com.phdev.quantofalta.core.designsystem.components.getIconDisplayName(name)
                                val bg by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    label = "cellBg"
                                )
                                val borderCol by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    label = "cellBorder"
                                )
                                
                                PremiumLockedWrapper(
                                    isLocked = isLocked,
                                    showBadge = false,
                                    onClick = {
                                        if (isLocked) {
                                            onFeatureLocked()
                                        } else {
                                            onIconSelected(name)
                                            onDismiss()
                                        }
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(AppShapes.medium)
                                            .background(bg)
                                            .border(1.5.dp, borderCol, AppShapes.medium)
                                            .padding(vertical = AppSpacing.small)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = com.phdev.quantofalta.core.designsystem.components.getIconByName(name),
                                                contentDescription = displayName,
                                                modifier = Modifier.size(24.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = displayName.split(" / ").first(),
                                            style = AppTypography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// UNIT PICKER DIALOG
@Composable
fun UnitPickerDialog(
    currentUnit: String,
    isPremium: Boolean,
    onUnitSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onFeatureLocked: () -> Unit
) {
    val units = listOf(
        Triple("DAYS", "Dias", "Exibe a contagem em dias"),
        Triple("FULL_TIME", "Tempo completo", "Exibe dias, horas e minutos"),
        Triple("WEEKS", "Semanas", "Exibe a contagem em semanas"),
        Triple("WEEKS_AND_DAYS", "Semanas e dias", "Combina semanas e dias restantes"),
        Triple("MONTHS", "Meses", "Exibe a contagem em meses"),
        Triple("MONTHS_AND_DAYS", "Meses e dias", "Combina meses e dias restantes"),
        Triple("WORKING_DAYS", "Dias úteis", "Conta apenas de segunda a sexta")
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unidade de contagem",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    units.forEach { (unitKey, label, description) ->
                        val isSelected = currentUnit == unitKey
                        val isLocked = unitKey != "DAYS" && !isPremium
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                            label = "unitCellBg"
                        )
                        val borderCol by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "unitCellBorder"
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.medium)
                                .background(bg)
                                .border(1.dp, borderCol, AppShapes.medium)
                                .clickable {
                                    if (isLocked) {
                                        onFeatureLocked()
                                    } else {
                                        onUnitSelected(unitKey)
                                    }
                                }
                                .padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = AppTypography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    style = AppTypography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selecionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isLocked) {
                                PremiumBadge()
                            }
                        }
                    }
                }
            }
        }
    }
}
