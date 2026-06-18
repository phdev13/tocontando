package com.phdev.quantofalta.feature.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.pressScale
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.AdaptiveIcon
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

@Composable
fun IntroScreen(
    onNavigate: (String) -> Unit,
    viewModel: IntroViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var currentStep by rememberSaveable { mutableStateOf(1) }
    val haptic = LocalHapticFeedback.current
    var lastClickTime by remember { mutableStateOf(0L) }

    // Share State of user selections across step 1, 2, and 3
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
            // Header bar containing progress and SKIP button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discrete Step progress indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
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

                // Skip (Pular) button with press compaction (no empty bounceClick callbacks)
                val skipBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                TextButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > 450L) {
                            lastClickTime = now
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (e: Exception) {}
                            viewModel.completeIntro()
                            onNavigate(Screen.Home.route)
                        }
                    },
                    modifier = Modifier
                        .pressScale(skipBtnInteractionSource)
                        .testTag("onboarding_skip_button"),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page selector content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } catch (e: Exception) {}
                                title = newTitle
                                iconName = newIcon
                                colorHex = newColorHex
                                daysLeft = newDays
                                progressFactor = newProgress
                                customUnit = "Dias"
                            }
                        )
                        2 -> StepCustomize(
                            title = title,
                            iconName = iconName,
                            color = mainColor,
                            colorHex = colorHex,
                            daysLeft = daysLeft,
                            progress = progressFactor,
                            unit = customUnit,
                            onChoicesUpdated = { newTitle, newDays, newColor, newIcon ->
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } catch (e: Exception) {}
                                title = newTitle
                                iconName = newIcon
                                colorHex = newColor
                                daysLeft = newDays
                                // mock progress updates depending on selection value
                                progressFactor = when {
                                    newDays <= 3 -> 0.95f
                                    newDays <= 7 -> 0.85f
                                    newDays <= 30 -> 0.60f
                                    else -> 0.35f
                                }
                            }
                        )
                        3 -> StepResult(
                            title = title,
                            iconName = iconName,
                            color = mainColor,
                            daysLeft = daysLeft,
                            progress = progressFactor,
                            unit = customUnit
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
                if (currentStep < 3) {
                    val nextBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > 450L) {
                                lastClickTime = now
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (e: Exception) {}
                                if (currentStep < 3) {
                                    currentStep++
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        interactionSource = nextBtnInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(nextBtnInteractionSource)
                            .testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
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
                    // Final Actions in Step 3
                    val launchBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > 450L) {
                                lastClickTime = now
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (e: Exception) {}
                                viewModel.completeIntro()
                                val prefillRoute = Screen.CreateEvent.createPrefillRoute(
                                    prefillTitle = title,
                                    prefillColorHex = colorHex,
                                    prefillIconName = iconName,
                                    prefillDaysLeft = daysLeft
                                )
                                onNavigate(prefillRoute)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        interactionSource = launchBtnInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(launchBtnInteractionSource)
                            .testTag("onboarding_launch_create_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Criar meu primeiro evento",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val exploreBtnInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    TextButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > 450L) {
                                lastClickTime = now
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (e: Exception) {}
                                viewModel.completeIntro()
                                onNavigate(Screen.Home.route)
                            }
                        },
                        interactionSource = exploreBtnInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(exploreBtnInteractionSource)
                            .testTag("onboarding_explore_button")
                    ) {
                        Text(
                            text = "Explorar o app",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350), label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.small),
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
                style = AppTypography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Acompanhe prazos, viagens ou momentos especiais.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        // Live Dynamic Interactive Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .height(180.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.15f),
                                color.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = color.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Icon & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AdaptiveIcon(
                                    iconName = iconName,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = title,
                                style = AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Progress Bar filling elegantly
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = color,
                                trackColor = color.copy(alpha = 0.12f),
                            )
                            Text(
                                text = "Progresso Geral • ${(animatedProgress * 100).toInt()}%",
                                style = AppTypography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Remaining Counter Box
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = daysLeft.toString(),
                            style = AppTypography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 38.sp
                            ),
                            color = color
                        )
                        Text(
                            text = unit,
                            style = AppTypography.labelLarge,
                            color = color.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Suggestion Chips list
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Toque em uma proposta para testar:",
                style = AppTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val suggestions = listOf(
                    Triple("Viagem", "Airplane", "#4F46E5" to 45),
                    Triple("Aniversário", "Cake", "#EC4899" to 12),
                    Triple("Férias", "BeachAccess", "#0EA5E9" to 8),
                    Triple("Pagamento", "AttachMoney", "#10B981" to 3)
                )

                suggestions.forEach { (label, icon, colorData) ->
                    val (hexValue, defaultDays) = colorData
                    val isSelected = title.contains(label) || (label == "Pagamento" && title.contains("Pagamento"))
                    val chipBg = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val labelColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) color else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .bounceClick(useHaptic = true) {
                                val progressVal = when (label) {
                                    "Viagem" -> 0.65f
                                    "Aniversário" -> 0.92f
                                    "Férias" -> 0.78f
                                    else -> 0.88f
                                }
                                onOptionSelected(
                                    if (label == "Viagem") "Viagem para Paris" else if (label == "Pagamento") "Dia do Pagamento" else "Meu $label",
                                    icon,
                                    hexValue,
                                    defaultDays,
                                    progressVal
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = labelColor
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// STEP 2 — CUSTOMIZE
// ==========================================
@Composable
fun StepCustomize(
    title: String,
    iconName: String,
    color: Color,
    colorHex: String,
    daysLeft: Int,
    progress: Float,
    unit: String,
    onChoicesUpdated: (String, Int, String, String) -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "")

    val displayDays = when (daysLeft) {
        7 -> 1
        30 -> 1
        365 -> 1
        else -> daysLeft
    }
    val displayUnit = when (daysLeft) {
        3 -> "Dias"
        7 -> "Semana"
        30 -> "Mês"
        365 -> "Ano"
        else -> "Dias"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Simule e monte o seu evento",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Customize o título, o prazo, a cor e o ícone de forma rápida.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        // Live Dynamic Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .height(130.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.12f),
                                color.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AdaptiveIcon(
                                    iconName = iconName,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = title,
                                style = AppTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Small Elegant Progress Bar
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = color,
                            trackColor = color.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = displayDays.toString(),
                            style = AppTypography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp
                            ),
                            color = color
                        )
                        Text(
                            text = displayUnit,
                            style = AppTypography.labelSmall,
                            color = color.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Customization Controls Pane
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Título Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Escolha um Título:",
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val titles = listOf("Estreia do Filme", "Casamento", "Fim das Férias", "Show do Ano")
                    titles.forEach { t ->
                        val isSelected = title == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .bounceClick(useHaptic = true) { onChoicesUpdated(t, daysLeft, colorHex, iconName) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = t,
                                style = AppTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Prazo Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Prazo Estimado:",
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dates = listOf(
                        "Em 3 dias" to 3,
                        "Em 1 semana" to 7,
                        "Em 1 mês" to 30,
                        "Em 1 ano" to 365
                    )
                    dates.forEach { (label, value) ->
                        val isSelected = daysLeft == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .bounceClick(useHaptic = true) { onChoicesUpdated(title, value, colorHex, iconName) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cor Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Escolha uma Cor:",
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        "#8B5CF6" to "Roxo",
                        "#3B82F6" to "Azul",
                        "#F43F5E" to "Coral",
                        "#10B981" to "Verde"
                    )
                    colors.forEach { (hex, description) ->
                        val parsed = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .bounceClick(useHaptic = true) { onChoicesUpdated(title, daysLeft, hex, iconName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = description,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ícone Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Escolha um Ícone:",
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icons = listOf(
                        "Star" to "Estrela",
                        "MusicNote" to "Música",
                        "Favorite" to "Coração",
                        "Work" to "Trabalho"
                    )
                    icons.forEach { (name, desc) ->
                        val isSelected = iconName == name
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .bounceClick(useHaptic = true) { onChoicesUpdated(title, daysLeft, colorHex, name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getIconByName(name),
                                    contentDescription = desc,
                                    tint = if (isSelected) color else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// STEP 3 — RESULT
// ==========================================
@Composable
fun StepResult(
    title: String,
    iconName: String,
    color: Color,
    daysLeft: Int,
    progress: Float,
    unit: String
) {
    var animateCardEntry by remember { mutableStateOf(false) }
    var animateFillProgress by remember { mutableStateOf(0f) }

    val displayDays = when (daysLeft) {
        7 -> 1
        30 -> 1
        365 -> 1
        else -> daysLeft
    }
    val displayUnit = when (daysLeft) {
        3 -> "Dias"
        7 -> "Semana"
        30 -> "Mês"
        365 -> "Ano"
        else -> "Dias"
    }

    LaunchedEffect(Unit) {
        delay(80)
        animateCardEntry = true
        delay(120)
        animateFillProgress = progress
    }

    val slideOffset by animateFloatAsState(
        targetValue = if (animateCardEntry) 0f else 64f,
        animationSpec = tween(durationMillis = 240, easing = { cubic -> cubic }), label = ""
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (animateCardEntry) 1f else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )
    val smoothProgress by animateFloatAsState(
        targetValue = animateFillProgress,
        animationSpec = tween(durationMillis = 250), label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.mediumLarge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tudo pronto!",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pronto. Agora você sempre saberá quanto falta.",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }

        // Miniature Home Container Mockup
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 350.dp)
                .height(220.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Miniature Home top navigation header element
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tô Contando",
                        style = AppTypography.labelLarge.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Scroll content list placeholder inside miniature
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 34.dp)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Gray placeholder for an existing list element
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    )

                    // The actual customized countdown enters dynamically with micro-animations
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(98.dp)
                            .offset(y = slideOffset.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.18f),
                                        color.copy(alpha = 0.03f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                              )
                            .border(
                                width = 1.dp,
                                color = color.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconByName(iconName).let { if (it == Icons.Filled.Star && iconName == "Airplane") getIconByName("Airplane") else it },
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        style = AppTypography.titleMedium.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Progress Bar filling smoothly
                                LinearProgressIndicator(
                                    progress = { smoothProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape),
                                    color = color,
                                    trackColor = color.copy(alpha = 0.08f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Counter
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = displayDays.toString(),
                                    style = AppTypography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = color
                                )
                                Text(
                                    text = displayUnit,
                                    style = AppTypography.labelSmall.copy(fontSize = 8.sp),
                                    color = color.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Another grey placeholder under it
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    )
                }
            }
        }
    }
}
