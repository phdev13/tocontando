package com.phdev.quantofalta.feature.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AdaptiveContent
import com.phdev.quantofalta.core.designsystem.components.AdaptiveIcon
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.pressScale
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.cards.NextSalaryCard
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun IntroScreen(
    onNavigate: (String) -> Unit,
    viewModel: IntroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var currentStep by rememberSaveable { mutableStateOf(1) }
    val haptic = LocalHapticFeedback.current
    var lastClickTime by remember { mutableStateOf(0L) }

    // Share State of user selections
    var title by rememberSaveable { mutableStateOf("Viagem para Paris") }
    var iconName by rememberSaveable { mutableStateOf("Airplane") }
    var colorHex by rememberSaveable { mutableStateOf("#4F46E5") } // Indigo
    var daysLeft by rememberSaveable { mutableStateOf(45) }
    var progressFactor by rememberSaveable { mutableStateOf(0.65f) }
    var customUnit by rememberSaveable { mutableStateOf("Dias") }

    val primaryColorString = colorHex
    val mainColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(primaryColorString))
        } catch (e: Exception) {
            Color(0xFF6200EE)
        }
    }

    val scaffoldBg = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = scaffoldBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discrete Step progress indicators (Now 4 steps)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val isSelected = currentStep == i
                        val indicatorWidth by animateFloatAsState(
                            targetValue = if (isSelected) 24f else 8f,
                            animationSpec = tween(durationMillis = 200), label = ""
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(indicatorWidth.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // Skip (Pular)
                val skipBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > 450L) {
                            lastClickTime = now
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                            viewModel.completeIntro()
                            onNavigate(Screen.Home.route)
                        }
                    },
                    modifier = Modifier.pressScale(skipBtnInteractionSource).testTag("onboarding_skip_button"),
                    interactionSource = skipBtnInteractionSource,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Pular",
                        style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            maxWidth = 560.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page selector content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                    }, label = ""
                ) { step ->
                    when (step) {
                        1 -> StepDiscover(
                            title = title,
                            iconName = iconName,
                            color = mainColor,
                            daysLeft = daysLeft,
                            progress = progressFactor,
                            unit = customUnit,
                            onOptionSelected = { newTitle, newIcon, newColorHex, newDays, newProgress ->
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                                title = newTitle
                                iconName = newIcon
                                colorHex = newColorHex
                                daysLeft = newDays
                                progressFactor = newProgress
                                customUnit = "Dias"
                            }
                        )
                        2 -> StepSalary(color = mainColor)
                        3 -> StepCustomize(
                            title = title,
                            iconName = iconName,
                            color = mainColor,
                            colorHex = colorHex,
                            daysLeft = daysLeft,
                            progress = progressFactor,
                            unit = customUnit,
                            onChoicesUpdated = { newTitle, newDays, newColor, newIcon ->
                                try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                                title = newTitle
                                iconName = newIcon
                                colorHex = newColor
                                daysLeft = newDays
                                progressFactor = when {
                                    newDays <= 3 -> 0.95f
                                    newDays <= 7 -> 0.85f
                                    newDays <= 30 -> 0.60f
                                    else -> 0.35f
                                }
                            }
                        )
                        4 -> StepFinal(
                            onFinish = { route ->
                                viewModel.completeIntro()
                                onNavigate(route)
                            },
                            title = title,
                            colorHex = colorHex,
                            iconName = iconName,
                            daysLeft = daysLeft
                        )
                    }
                }
            }

            // Bottom Navigation Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = AppSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                if (currentStep < 4) {
                    val nextBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > 450L) {
                                lastClickTime = now
                                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                                currentStep++
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        interactionSource = nextBtnInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(nextBtnInteractionSource)
                            .testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Continuar",
                                style = AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    // Actions are handled directly inside StepFinal for step 4
                }
            }
        }
        }
    }
}

// ==========================================
// STEP 1 — DISCOVER
// ==========================================
@Composable
fun StepDiscover(
    title: String,
    iconName: String,
    color: Color,
    daysLeft: Int,
    progress: Float,
    unit: String,
    onOptionSelected: (String, String, String, Int, Float) -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 350), label = "")

    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.mediumLarge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Saiba quanto falta para\no que realmente importa.",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp, fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Acompanhe prazos, viagens e momentos especiais com precisão.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.small)) {
            val dummyEvent = createMockEventUiModel(
                title = title,
                iconName = iconName,
                color = color,
                daysLeft = daysLeft,
                progress = animatedProgress,
                unit = unit,
                type = if (title.contains("Namoro")) com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP else com.phdev.quantofalta.domain.model.EventType.STANDARD
            )
            if (dummyEvent.type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
                com.phdev.quantofalta.core.designsystem.components.RelationshipFeaturedCard(
                    event = dummyEvent,
                    onClick = {}
                )
            } else {
                MainEventCard(
                    title = dummyEvent.title,
                    date = dummyEvent.date,
                    number = dummyEvent.number,
                    units = dummyEvent.units,
                    color = dummyEvent.color,
                    iconName = dummyEvent.iconName,
                    progress = dummyEvent.progress,
                    contextMessage = dummyEvent.contextMessage,
                    targetDateMillis = dummyEvent.dateMillis,
                    coverImageUri = dummyEvent.coverImageUri,
                    onClick = {}
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Toque em uma proposta para testar:", style = AppTypography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                val suggestions = listOf(
                    Triple("Viagem", "Airplane", "#4F46E5" to 45),
                    Triple("Namoro", "Favorite", "#EC4899" to 812),
                    Triple("Férias", "BeachAccess", "#0EA5E9" to 8)
                )
                suggestions.forEach { (label, icon, colorData) ->
                    val (hexValue, defaultDays) = colorData
                    val isSelected = title.contains(label)
                    val chipBg = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(chipBg)
                            .border(width = 1.dp, color = if (isSelected) color else Color.Transparent, shape = RoundedCornerShape(16.dp))
                            .bounceClick(useHaptic = true) {
                                val progressVal = when (label) { "Viagem" -> 0.65f; "Namoro" -> 0.72f; else -> 0.78f }
                                onOptionSelected(if (label == "Viagem") "Viagem para Paris" else if (label == "Namoro") "Nosso Namoro" else "Meu $label", icon, hexValue, defaultDays, progressVal)
                            }.padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, style = AppTypography.labelLarge, fontWeight = FontWeight.Bold, color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 2 — SALARY MODE (NEW)
// ==========================================
@Composable
fun StepSalary(color: Color) {
    var animateFillProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        animateFillProgress = 0.73f // Simulando dia 22 do mês
    }
    val smoothProgress by animateFloatAsState(targetValue = animateFillProgress, animationSpec = tween(durationMillis = 1800), label = "")

    val salaryColor = Color(0xFF10B981) // Green for money

    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.mediumLarge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Descubra o real valor\ndo seu tempo.",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp, fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Com o Modo Salário, você acompanha seus ganhos e o seu suor rendendo em tempo real.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.small)) {
            NextSalaryCard(
                event = createMockEventUiModel(
                    title = "Salário do Mês",
                    iconName = "AttachMoney",
                    color = salaryColor,
                    daysLeft = 8,
                    progress = smoothProgress,
                    unit = "Dias",
                    mode = com.phdev.quantofalta.domain.model.mode.CardMode.Salary
                ),
                onClick = {}
            )
        }
    }
}

// ==========================================
// STEP 3 — CUSTOMIZE
// ==========================================
@Composable
fun StepCustomize(
    title: String, iconName: String, color: Color, colorHex: String, daysLeft: Int, progress: Float, unit: String,
    onChoicesUpdated: (String, Int, String, String) -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "")
    val displayDays = when (daysLeft) { 7 -> 1; 30 -> 1; 365 -> 1; else -> daysLeft }
    val displayUnit = when (daysLeft) { 3 -> "Dias"; 7 -> "Semana"; 30 -> "Mês"; 365 -> "Ano"; else -> "Dias" }

    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Personalize cada momento", style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Text(text = "Controle o visual para deixar o app com a sua cara.", style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.small)) {
            val dummyEvent = createMockEventUiModel(
                title = title,
                iconName = iconName,
                color = color,
                daysLeft = daysLeft,
                progress = animatedProgress,
                unit = unit,
                type = if (title.contains("Namoro")) com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP else com.phdev.quantofalta.domain.model.EventType.STANDARD
            )
            if (dummyEvent.type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
                com.phdev.quantofalta.core.designsystem.components.RelationshipFeaturedCard(
                    event = dummyEvent,
                    onClick = {}
                )
            } else {
                MainEventCard(
                    title = dummyEvent.title,
                    date = dummyEvent.date,
                    number = dummyEvent.number,
                    units = dummyEvent.units,
                    color = dummyEvent.color,
                    iconName = dummyEvent.iconName,
                    progress = dummyEvent.progress,
                    contextMessage = dummyEvent.contextMessage,
                    targetDateMillis = dummyEvent.dateMillis,
                    coverImageUri = dummyEvent.coverImageUri,
                    onClick = {}
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Colors
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Escolha uma Cor:", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val colors = listOf("#8B5CF6", "#3B82F6", "#F43F5E", "#10B981")
                    colors.forEach { hex ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier.size(48.dp).bounceClick(useHaptic = true) { onChoicesUpdated(title, daysLeft, hex, iconName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(parsed)
                                    .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Icons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Escolha um Ícone:", style = AppTypography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icons = listOf("Star", "MusicNote", "Favorite", "Work")
                    icons.forEach { name ->
                        val isSelected = iconName == name
                        Box(
                            modifier = Modifier.size(48.dp).bounceClick(useHaptic = true) { onChoicesUpdated(title, daysLeft, colorHex, name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(width = if (isSelected) 1.5.dp else 0.dp, color = if (isSelected) color else Color.Transparent, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = getIconByName(name), contentDescription = null, tint = if (isSelected) color else MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 4 — FINAL & PREMIUM
// ==========================================
@Composable
fun StepFinal(
    onFinish: (String) -> Unit,
    title: String,
    colorHex: String,
    iconName: String,
    daysLeft: Int
) {
    val haptic = LocalHapticFeedback.current
    var lastClickTime by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.phdev.quantofalta.R.drawable.ic_padrao),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tudo pronto!",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Você está a um passo de transformar a forma como visualiza seus prazos e rendimentos.",
                style = AppTypography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        // Main Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime > 450L) {
                        lastClickTime = now
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                        val prefillRoute = Screen.CreateEvent.createPrefillRoute(
                            prefillTitle = title, prefillColorHex = colorHex, prefillIconName = iconName, prefillDaysLeft = daysLeft
                        )
                        onFinish(prefillRoute)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Criar meu primeiro evento",
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            OutlinedButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime > 450L) {
                        lastClickTime = now
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                        onFinish(Screen.Home.route)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = "Explorar o app",
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime > 450L) {
                        lastClickTime = now
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                        onFinish(Screen.RedeemCode.route)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Já possuo uma chave Premium",
                    style = AppTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ==========================================
// MOCK DATA GENERATOR
// ==========================================
private fun createMockEventUiModel(
    title: String,
    iconName: String,
    color: Color,
    daysLeft: Int,
    progress: Float,
    unit: String,
    mode: com.phdev.quantofalta.domain.model.mode.CardMode = com.phdev.quantofalta.domain.model.mode.CardMode.Standard,
    type: com.phdev.quantofalta.domain.model.EventType = com.phdev.quantofalta.domain.model.EventType.STANDARD
): com.phdev.quantofalta.domain.model.EventUiModel {
    val totalHours = daysLeft * 24
    
    val isRelationship = type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP
    val relStats = if (isRelationship) {
        com.phdev.quantofalta.core.relationship.RelationshipCalculator.RelationshipStats(
            totalDays = daysLeft,
            years = daysLeft / 365,
            months = (daysLeft % 365) / 30,
            remainingDays = (daysLeft % 365) % 30,
            nextMilestoneDays = null,
            daysToNextMilestone = null,
            daysToNextMonthly = null,
            daysToNextAnnual = null,
            nextSpecialEventDays = null,
            nextSpecialEventLabel = null
        )
    } else null
    
    val relUiState = if (isRelationship && relStats != null) {
        com.phdev.quantofalta.domain.model.RelationshipUiState(
            primaryText = "$daysLeft dias juntos",
            secondaryText = "",
            stats = relStats,
            relationshipType = "dating",
            monthlyEnabled = true,
            annualEnabled = true,
            milestonesEnabled = true,
            startEpochDay = java.time.LocalDate.now().toEpochDay() - daysLeft
        )
    } else null

    return com.phdev.quantofalta.domain.model.EventUiModel(
        id = "mock_${System.currentTimeMillis()}",
        title = title,
        date = "01 Jan",
        time = "12:00",
        units = unit,
        number = daysLeft.toString(),
        progress = progress,
        contextMessage = "Faltam $daysLeft $unit",
        isToday = daysLeft == 0,
        isSoon = daysLeft <= 3,
        eventState = com.phdev.quantofalta.domain.model.EventState.ACTIVE,
        primaryText = "$daysLeft",
        secondaryText = unit,
        color = color,
        iconName = iconName,
        badgeText = "",
        totalHoursRemaining = totalHours.toString(),
        isCompleted = false,
        isArchived = false,
        isPrivate = false,
        dateMillis = System.currentTimeMillis() + (daysLeft * 86400000L),
        isPinned = false,
        coverImageUri = null,
        mode = mode,
        type = type,
        relationshipUiState = relUiState,
        salaryUiState = if (mode == com.phdev.quantofalta.domain.model.mode.CardMode.Salary || type == com.phdev.quantofalta.domain.model.EventType.SALARY) 
            com.phdev.quantofalta.domain.model.SalaryUiState(
                frequency = "monthly",
                paymentDay = 5,
                customIntervalDays = null,
                nextPaymentEpochDay = 0L,
                daysRemaining = daysLeft,
                businessDaysRemaining = null,
                cycleProgressPercentage = (progress * 100).toInt(),
                weekendRule = "none",
                showBusinessDays = false,
                salaryValue = 5250.00,
                primaryText = "R$ 3.845,90",
                secondaryText = "Trabalhado até agora",
                salaryCardStyle = com.phdev.quantofalta.domain.model.mode.SalaryCardStyle.NEXT_SALARY,
                salaryGoalTarget = null,
                salaryCustomPhrase = null
            ) else null
    )
}
