package com.phdev.quantofalta.feature.finance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.billing.blocks
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.*
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.feature.eventdetails.EventDetailsViewModel
import com.phdev.quantofalta.feature.eventdetails.getActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryDetailsScreen(
    event: EventUiModel,
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: EventDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val salaryState = event.salaryUiState ?: return
    var showValue by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    var premiumFeatureToExplain by remember { mutableStateOf<PremiumFeatureInfo?>(null) }
    
    premiumFeatureToExplain?.let { info ->
        PremiumFeatureModal(
            feature = info,
            onDismiss = { premiumFeatureToExplain = null },
            onNavigatePremium = {
                premiumFeatureToExplain = null
                onNavigate(Screen.Premium.route)
            }
        )
    }

    if (showShareSheet) {
        com.phdev.quantofalta.feature.eventdetails.ShareBottomSheet(
            event = event,
            isPremium = isPremium,
            onDismiss = { showShareSheet = false },
            onNavigateToPremium = {
                showShareSheet = false
                onNavigate(Screen.Premium.route)
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir modo salário") },
            text = { Text("Tem certeza que deseja excluir '${event.title}' permanentemente? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (e: Exception) {}
                    showDeleteDialog = false
                    viewModel.deleteEvent(event.id)
                    onNavigateBack()
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                    }
                    IconButton(onClick = {
                        onNavigate(Screen.CreateSalary.createRoute(event.id))
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (event.isPinned) "Remover destaque" else "Destacar") },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Filled.Star, 
                                        contentDescription = null,
                                        tint = if (event.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                    ) 
                                },
                                onClick = { 
                                    showMenu = false
                                    viewModel.togglePin(event.id) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (event.isArchived) "Desarquivar" else "Arquivar") },
                                leadingIcon = { Icon(if (event.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, contentDescription = null) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.toggleArchived(event.id, !event.isArchived) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (event.isCompleted) "Reativar" else "Concluir") },
                                leadingIcon = { Icon(if (event.isCompleted) Icons.Filled.RestartAlt else Icons.Filled.Check, contentDescription = null) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.toggleCompleted(event.id, !event.isCompleted) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    showMenu = false
                                    showDeleteDialog = true 
                                }
                            )
                        }
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
            
            // Icon Circle
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
                text = "Pagamento dia ${salaryState.paymentDay}",
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.huge))
            
            // Main Countdown
            Text(
                text = salaryState.primaryText,
                style = AppTypography.displayLarge.copy(
                    fontSize = if (salaryState.primaryText.length > 10) 36.sp else 48.sp,
                    lineHeight = if (salaryState.primaryText.length > 10) 40.sp else 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = event.color,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
            
            // Badge
            if (salaryState.businessDaysRemaining != null) {
                Box(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(event.color.copy(alpha = 0.1f))
                        .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small)
                ) {
                    Text(
                        text = "${salaryState.businessDaysRemaining} dias úteis",
                        style = AppTypography.labelLarge,
                        color = event.color
                    )
                }
            } else if (event.badgeText.isNotBlank()) {
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
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            // Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = salaryState.cycleProgressPercentage / 100f,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "salaryProgress"
            )
            
            Text(
                text = "Ciclo atual: ${salaryState.cycleProgressPercentage}% concluído",
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
                            text = "Frequência",
                            style = AppTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        Text(
                            text = when (salaryState.frequency) {
                                "monthly" -> "Mensal"
                                "biweekly" -> "Quinzenal"
                                "weekly" -> "Semanal"
                                else -> "Personalizada"
                            },
                            style = AppTypography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
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
                            text = "Fim de Semana",
                            style = AppTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        Text(
                            text = when (salaryState.weekendRule) {
                                "friday" -> "Antecipa"
                                "monday" -> "Adia"
                                else -> "Mantém"
                            },
                            style = AppTypography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            
            // Salary Value (Private)
            if (salaryState.salaryValue != null && salaryState.salaryValue > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = AppShapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Valor (Privado)",
                                style = AppTypography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (showValue) {
                                Text(
                                    "R$ ${String.format("%.2f", salaryState.salaryValue)}",
                                    style = AppTypography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    "R$ ••••••",
                                    style = AppTypography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        IconButton(onClick = { 
                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch(e: Exception){}
                            showValue = !showValue 
                        }) {
                            Icon(
                                imageVector = if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar Visibilidade",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.large))
            }
            
            // Timeline Button
            var showTimeline by remember { mutableStateOf(false) }
            val isTimelineLocked = isPremium.blocks(PremiumFeature.FULL_STATISTICS)
            
            TextButton(
                onClick = { 
                    if (isTimelineLocked) {
                        premiumFeatureToExplain = PremiumFeatureInfo(
                            title = "Histórico do Evento",
                            description = "Veja a linha do tempo de criação, edições e mudanças de status do evento.",
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
}
