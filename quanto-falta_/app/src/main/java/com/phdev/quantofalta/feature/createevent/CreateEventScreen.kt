package com.phdev.quantofalta.feature.createevent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.domain.model.toUiModel
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.utils.ImageStorageHelper
import com.phdev.quantofalta.core.designsystem.components.AppTextField
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.PremiumLockedWrapper
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.allows
import com.phdev.quantofalta.billing.blocks
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.getIconDisplayName
import com.phdev.quantofalta.core.designsystem.components.iconNamesList
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.phdev.quantofalta.core.designsystem.theme.Colors

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.time.TimeUtils
import androidx.compose.ui.graphics.toArgb
import com.phdev.quantofalta.core.designsystem.components.showDatePicker
import com.phdev.quantofalta.core.designsystem.components.showTimePicker

// Localized Color Names MAP (Issue 8)
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
fun CreateEventScreen(
    eventId: String? = null,
    prefillTitle: String? = null,
    prefillColorHex: String? = null,
    prefillIconName: String? = null,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: CreateEventViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val initialTitle = prefillTitle ?: ""
    val initialColor = remember(prefillColorHex) {
        prefillColorHex?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                null
            }
        } ?: Colors[0]
    }
    val initialIcon = prefillIconName ?: iconNamesList[0]
    val initialTimestamp = remember {
        System.currentTimeMillis() + 24L * 60L * 60L * 1000L
    }

    var name by remember { mutableStateOf(initialTitle) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var timestamp by remember { mutableStateOf(initialTimestamp) }
    var unit by remember { mutableStateOf("Automático") }
    var coverImageUri by remember { mutableStateOf<String?>(null) }
    
    // Dialog state controllers
    var showFullIconPicker by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var showUnitPicker by remember { mutableStateOf(false) }
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }
    var isPrivate by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
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
                selectedColor = androidx.compose.ui.graphics.Color(event.colorArgb)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localUri = ImageStorageHelper.copyImageToInternalStorage(context, uri)
            if (localUri != null) {
                coverImageUri = localUri
            }
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (eventId == null) "Novo evento" else "Editar evento",
                centerTitle = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                },
                actions = {
                    val isValid = name.isNotBlank()
                    Button(
                        onClick = {
                            if (isValid && !isSaving) {
                                isSaving = true
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (e: Exception) {}
                                viewModel.saveEvent(
                                    id = eventId,
                                    title = name,
                                    dateMillis = timestamp,
                                    unit = unit,
                                    colorArgb = selectedColor.toArgb(),
                                    iconName = selectedIcon,
                                    isPrivate = isPrivate,
                                    isPinned = isPinned,
                                    coverImageUri = coverImageUri
                                )
                                onBack()
                            }
                        },
                        enabled = isValid,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = AppSpacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Salvar",
                            style = AppTypography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .fadeSlideIn()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.medium)
                    .padding(top = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // SEÇÃO 1: INFORMAÇÕES BÁSICAS
                FormSection(title = "Informações básicas") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "NOME DO EVENTO",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Viagem para Salvador",
                            icon = getIconByName("Create")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "UNIDADE DE CONTAGEM",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        AppTextField(
                            value = unit,
                            onValueChange = {},
                            placeholder = "Dias",
                            icon = getIconByName("List"),
                            onClick = { showUnitPicker = true }
                        )
                    }
                }

                // SEÇÃO 2: DATA E HORÁRIO
                FormSection(title = "Data e horário") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DATA",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        AppTextField(
                            value = TimeUtils.formatDate(timestamp),
                            onValueChange = {},
                            placeholder = "24 de maio de 2024",
                            icon = getIconByName("Event"),
                            onClick = {
                                showDatePicker(context) { newDate -> timestamp = newDate }
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "HORÁRIO",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        AppTextField(
                            value = TimeUtils.formatTime(timestamp),
                            onValueChange = {},
                            placeholder = "09:00",
                            icon = getIconByName("Alarm"),
                            onClick = {
                                showTimePicker(context, timestamp) { newTime -> timestamp = newTime }
                            }
                        )
                    }
                }

                // SEÇÃO 3: APARÊNCIA
                FormSection(title = "Aparência") {
                    
                    // Foto de Capa
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "FOTO DE CAPA (Opcional)",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        if (coverImageUri != null || name.isNotBlank()) {
                            val dummyUiModel = com.phdev.quantofalta.domain.model.Event(
                                id = "",
                                title = name.ifBlank { "Meu Evento" },
                                iconName = selectedIcon,
                                colorArgb = selectedColor.toArgb(),
                                targetDate = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                                targetTime = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime(),
                                zoneId = java.time.ZoneId.systemDefault().id,
                                referenceDate = null,
                                format = runCatching { com.phdev.quantofalta.domain.model.CountdownFormat.valueOf(unit) }.getOrDefault(com.phdev.quantofalta.domain.model.CountdownFormat.DAYS),
                                direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
                                createdAtMillis = System.currentTimeMillis(),
                                coverImageUri = coverImageUri
                            ).toUiModel(context = androidx.compose.ui.platform.LocalContext.current)

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.fillMaxWidth(0.9f)) {
                                    com.phdev.quantofalta.core.designsystem.components.MainEventCard(
                                        title = dummyUiModel.title,
                                        date = dummyUiModel.date,
                                        number = dummyUiModel.number,
                                        units = dummyUiModel.units,
                                        color = dummyUiModel.color,
                                        iconName = dummyUiModel.iconName,
                                        progress = dummyUiModel.progress,
                                        contextMessage = dummyUiModel.contextMessage,
                                        isToday = dummyUiModel.isToday,
                                        targetDateMillis = dummyUiModel.dateMillis,
                                        coverImageUri = dummyUiModel.coverImageUri,
                                        onClick = { /* Preview */ }
                                    )
                                    if (coverImageUri != null) {
                                        androidx.compose.material3.IconButton(
                                            onClick = { coverImageUri = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                                                .size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (coverImageUri == null) {
                            if (name.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isPremium.allows(PremiumFeature.COVER_PHOTO)) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                                            photoPickerLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        } else {
                                            premiumFeatureToExplain = PremiumFeatureInfo(
                                                title = "Foto de Capa",
                                                description = "Personalize seus cards de evento usando suas próprias fotos de fundo para deixá-los ainda mais memoráveis.",
                                                icon = Icons.Filled.Image
                                            )
                                        }
                                    }
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = AppShapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        shape = AppShapes.medium
                                    )
                                    .padding(AppSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adicionar foto da galeria",
                                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!isPremium) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Filled.Lock, contentDescription = "Premium", tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    // Preview do Ícone e Nome Ativo (Hierarquia Real)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = AppShapes.medium
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                shape = AppShapes.medium
                            )
                            .padding(AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(AppShapes.medium)
                                .background(selectedColor.copy(alpha = 0.15f))
                                .border(2.dp, selectedColor, AppShapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(selectedIcon),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = selectedColor
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "ÍCONE SELECIONADO",
                                style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = getIconDisplayName(selectedIcon),
                                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Seletor de cores
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COR DO EVENTO",
                                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = getColorLabel(selectedColor),
                                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = selectedColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                                val isLocked = isPremium.blocks(PremiumFeature.ALL_COLORS) && Colors.indexOf(color) >= PremiumFeature.FREE_COLOR_COUNT

                                PremiumLockedWrapper(
                                    isLocked = isLocked,
                                    onClick = {
                                        if (isLocked) {
                                            premiumFeatureToExplain = PremiumFeatureInfo(
                                                title = "Todas as Cores",
                                                description = "Desbloqueie a paleta completa de cores para personalizar seus eventos.",
                                                icon = Icons.Filled.Palette
                                            )
                                        } else {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                                            selectedColor = color
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
                            val isColorPickerLocked = isPremium.blocks(PremiumFeature.ALL_COLORS)
                            PremiumLockedWrapper(
                                isLocked = isColorPickerLocked,
                                onClick = {
                                    if (isColorPickerLocked) {
                                        premiumFeatureToExplain = PremiumFeatureInfo(
                                            title = "Cores Personalizadas",
                                            description = "Crie cores únicas e ilimitadas para combinar exatamente com o estilo do seu evento.",
                                            icon = Icons.Filled.Palette
                                        )
                                    } else {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                                        showCustomColorPicker = true
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

                    // Seletor de ícones recomendados
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "RECOMENDADOS",
                            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        val popularIcons = listOf("Airplane", "Cake", "Beach", "Music", "Notifications", "Favorite", "Work", "School")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            popularIcons.forEach { iconName ->
                                val isSelected = selectedIcon == iconName
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

                                val isLocked = isPremium.blocks(PremiumFeature.ALL_ICONS) && popularIcons.indexOf(iconName) > 3

                                PremiumLockedWrapper(
                                    isLocked = isLocked,
                                    onClick = {
                                        if (isLocked) {
                                            premiumFeatureToExplain = PremiumFeatureInfo(
                                                title = "Todos os Ícones",
                                                description = "Escolha entre centenas de ícones para representar qualquer momento especial.",
                                                icon = Icons.Filled.Star
                                            )
                                        } else {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                                            selectedIcon = iconName
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

                        // Pesquisa global de ícones acionadora
                        TextButton(
                            onClick = { showFullIconPicker = true },
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

                // SEÇÃO 4: OPÇÕES ADICIONAIS
                FormSection(title = "Opções Adicionais") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Proteger com biometria",
                                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Exigir digital/face ID para visualizar os detalhes deste evento.",
                                style = AppTypography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isPremium) {
                                Text("⭐ Exclusivo Premium", color = Color(0xFFD4AF37), style = AppTypography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        androidx.compose.material3.Switch(
                            checked = isPrivate,
                            onCheckedChange = {
                                if (isPremium.allows(PremiumFeature.BIOMETRIC_PROTECTION)) {
                                    isPrivate = it
                                } else {
                                    premiumFeatureToExplain = PremiumFeatureInfo(
                                        title = "Proteção por Biometria",
                                        description = "Exija digital ou Face ID para visualizar os detalhes deste evento particular.",
                                        icon = Icons.Filled.Lock
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isPinnedLocked = isPremium.blocks(PremiumFeature.PREMIUM_CARDS)

                    PremiumLockedWrapper(
                        isLocked = isPinnedLocked,
                        onClick = {
                            if (isPinnedLocked) {
                                premiumFeatureToExplain = PremiumFeatureInfo(
                                    title = "Destaque de Evento",
                                    description = "Mantenha seu evento mais importante no topo da Home com o Premium.",
                                    icon = Icons.Filled.Star
                                )
                            } else {
                                isPinned = !isPinned
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isPinnedLocked) { if (!isPinnedLocked) isPinned = !isPinned },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Destacar este evento",
                                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Exibe este evento em tamanho grande no topo da Tela Inicial.",
                                    style = AppTypography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = isPinned,
                                onCheckedChange = { if (!isPinnedLocked) isPinned = it },
                                enabled = !isPinnedLocked
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info box at any bottom layout with proper system bar padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.medium)
                    .clip(AppShapes.medium)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(AppSpacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A contagem de tempo do seu evento iniciará imediatamente após salvar.",
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
        }
    }

    // Modal dialogs
    if (showCustomColorPicker) {
        CustomColorPickerSheet(
            initialColor = selectedColor,
            onColorSelected = { selectedColor = it },
            onDismiss = { showCustomColorPicker = false }
        )
    }

    if (showFullIconPicker) {
        IconPickerDialog(
            currentSelection = selectedIcon,
            isPremium = isPremium,
            onIconSelected = { selectedIcon = it },
            onDismiss = { showFullIconPicker = false },
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

    if (showUnitPicker) {
        UnitPickerDialog(
            currentUnit = unit,
            onUnitSelected = { unit = it },
            onDismiss = { showUnitPicker = false }
        )
    }

    premiumFeatureToExplain?.let { feature ->
        PremiumFeatureModal(
            feature = feature,
            onNavigatePremium = {
                premiumFeatureToExplain = null
                onNavigate(com.phdev.quantofalta.core.navigation.Screen.Premium.route)
            },
            onDismiss = { premiumFeatureToExplain = null }
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = AppShapes.large
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                shape = AppShapes.large
            )
            .padding(AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Text(
            text = title.uppercase(),
            style = AppTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
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
                    placeholder = "Pesquisar (ex: avião, bolo, música)...",
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
                                val isLocked = !isPremium && !(com.phdev.quantofalta.core.designsystem.components.iconCategories["Básicos"]?.contains(name) == true)
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

// UNIT PICKER DIALOG (Resolves "Automático" being confusing and improves selection UX)
@Composable
fun UnitPickerDialog(
    currentUnit: String,
    onUnitSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val units = listOf(
        "Automático" to "Detecta e agrupa unidades (Anos, Meses, Dias...)",
        "Anos" to "Exibe o tempo restante em anos",
        "Meses" to "Exibe o tempo restante em meses",
        "Semanas" to "Exibe o tempo restante em semanas",
        "Dias" to "Exibe o tempo de forma estrita em dias",
        "Horas" to "Exibe o tempo de forma estrita em horas",
        "Minutos" to "Exibe o tempo de forma estrita em minutos",
        "Segundos" to "Exibe o tempo de forma estrita em segundos"
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
                    units.forEach { (unitKey, description) ->
                        val isSelected = currentUnit.lowercase() == unitKey.lowercase()
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
                                    onUnitSelected(unitKey)
                                    onDismiss()
                                }
                                .padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = unitKey,
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
                            }
                        }
                    }
                }
            }
        }
    }
}
