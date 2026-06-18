package com.phdev.quantofalta.feature.eventdetails

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.core.AppViewModelProvider
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.blocks
import com.phdev.quantofalta.core.designsystem.components.PremiumBadge
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureModal
import com.phdev.quantofalta.core.designsystem.components.PremiumFeatureInfo
import androidx.compose.ui.draw.alpha

@Composable
fun EventDetailsScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: EventDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiStateEvent by remember(eventId) { viewModel.getEventUiState(eventId) }.collectAsStateWithLifecycle(null)
    val event = uiStateEvent
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf(false) }
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }

    if (event == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    LaunchedEffect(event.isPrivate, isPremium, isAuthenticated) {
        if (event.isPrivate && isPremium && !isAuthenticated) {
            val activity = context.getActivity()
            if (activity != null) {
                val biometricManager = BiometricManager.from(context)
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                authError = true
                                onBack() // Go back if auth fails/cancelled
                            }
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isAuthenticated = true
                            }
                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                // Keep showing the prompt
                            }
                        })
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Evento Privado")
                        .setSubtitle("Desbloqueie para visualizar detalhes")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    isAuthenticated = true // Fallback if biometrics aren't available
                }
            } else {
                isAuthenticated = true
            }
        } else if (!event.isPrivate || !isPremium) {
            isAuthenticated = true
        }
    }

    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            if (authError) {
                Text("Erro de autenticação", color = MaterialTheme.colorScheme.error)
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    // Ocultar da tela de aplicativos recentes e impedir screenshots se for privado
    androidx.compose.runtime.DisposableEffect(event.isPrivate) {
        val activity = context.getActivity()
        if (event.isPrivate) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (event.isPrivate) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir evento") },
            text = { Text("Tem certeza que deseja excluir '${event.title}' permanentemente? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (e: Exception) {}
                    showDeleteDialog = false
                    viewModel.deleteEvent(eventId)
                    onBack()
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    var showShareSheet by remember { mutableStateOf(false) }
    
    if (showShareSheet) {
        ShareBottomSheet(
            event = event,
            isPremium = isPremium,
            onDismiss = { showShareSheet = false },
            onNavigateToPremium = { onNavigate(Screen.Premium.route) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Share button
                    if (!event.isPrivate) {
                        IconButton(onClick = {
                            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                            showShareSheet = true
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                        }
                    }
                    IconButton(onClick = { onNavigate(Screen.CreateEvent.createRoute(eventId)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) {}
                        viewModel.toggleArchived(eventId, !event.isArchived)
                    }) {
                        Icon(if (event.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, contentDescription = if (event.isArchived) "Desarquivar" else "Arquivar")
                    }
                    IconButton(onClick = {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (e: Exception) {}
                        viewModel.toggleCompleted(eventId, !event.isCompleted)
                    }) {
                        Icon(if (event.isCompleted) Icons.Filled.RestartAlt else Icons.Filled.Check, contentDescription = if (event.isCompleted) "Reativar" else "Concluir")
                    }
                    IconButton(onClick = {
                        showDeleteDialog = true
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Deletar")
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
                .padding(horizontal = AppSpacing.large)
                .fadeSlideIn()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(event.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(event.iconName),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
            
            // Title and Date
            Text(
                text = event.title,
                style = AppTypography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
            Text(
                text = "${event.date} às ${event.time}",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.huge))
            
            // Main Countdown
            Text(
                text = event.number,
                style = AppTypography.displayLarge.copy(fontFeatureSettings = "tnum"),
                color = event.color
            )
            Text(
                text = event.units,
                style = AppTypography.headlineMedium,
                color = event.color
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
            
            // Badge
            Box(
                modifier = Modifier
                    .clip(AppShapes.pill)
                    .background(event.color.copy(alpha = 0.1f))
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small)
            ) {
                Text(
                    text = event.badgeText,
                    style = AppTypography.labelLarge,
                    color = event.color
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            // Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = event.progress,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "eventDetailsProgress"
            )
            
            Text(
                text = "${(event.progress * 100).toInt()}% concluído",
                style = AppTypography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(AppShapes.pill),
                color = event.color,
                trackColor = event.color.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            // Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Time Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = AppShapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Horas restantes",
                            style = AppTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        Text(
                            text = event.totalHoursRemaining,
                            style = AppTypography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Progress Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = AppShapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Progresso total",
                            style = AppTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        Text(
                            text = "${(event.progress * 100).toInt()}%",
                            style = AppTypography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            // Bottom Info Message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(AppSpacing.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = com.phdev.quantofalta.core.utils.EventMessageProvider.getHighlightMessage(
                        eventId = event.id,
                        iconName = event.iconName,
                        numberStr = event.number,
                        units = event.units,
                        isCompleted = event.isCompleted,
                        isSoon = event.isSoon,
                        rawTitle = event.title
                    ),
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.large))
            
            // Botão de Renovação (Se concluído)
            val isDone = event.isCompleted || event.progress >= 1f
            if (isDone) {
                Button(
                    onClick = {
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                        viewModel.duplicateForNextYear(event.id) {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AppShapes.medium,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = event.color
                    )
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Renovar para o próximo ano",
                        style = AppTypography.labelLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(AppSpacing.medium))
            }
            
            // Botão Linha do Tempo
            var showTimeline by remember { mutableStateOf(false) }
            val isTimelineLocked = isPremium.blocks(PremiumFeature.FULL_STATISTICS)
            
            androidx.compose.material3.TextButton(
                onClick = { 
                    if (isTimelineLocked) {
                        premiumFeatureToExplain = PremiumFeatureInfo(
                            title = "Histórico do Evento",
                            description = "Veja a linha do tempo completa de edições, alertas e marcos alcançados.",
                            icon = Icons.Filled.History
                        )
                    } else {
                        showTimeline = true 
                    }
                },
                modifier = Modifier.fillMaxWidth().then(if (isTimelineLocked) Modifier.alpha(0.65f) else Modifier)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.small))
                Text(
                    "Ver Histórico do Evento",
                    style = AppTypography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isTimelineLocked) {
                    Spacer(modifier = Modifier.width(AppSpacing.small))
                    PremiumBadge()
                }
            }
            
            if (showTimeline) {
                com.phdev.quantofalta.feature.timeline.TimelineBottomSheet(
                    eventId = event.id,
                    onDismiss = { showTimeline = false }
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.large))
        }
    }

    premiumFeatureToExplain?.let { feature ->
        PremiumFeatureModal(
            feature = feature,
            onNavigatePremium = {
                premiumFeatureToExplain = null
                onNavigate(Screen.Premium.route)
            },
            onDismiss = { premiumFeatureToExplain = null }
        )
    }
}

fun android.content.Context.getActivity(): androidx.fragment.app.FragmentActivity? = when (this) {
    is androidx.fragment.app.FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.getActivity()
    else -> null
}
