package com.phdev.quantofalta.feature.relationship

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.relationshipTypeLabel
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import com.phdev.quantofalta.core.relationship.RelationshipCalculator
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.feature.eventdetails.ShareBottomSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

@Composable
fun RelationshipDetailScreen(
    eventId: String,
    viewModel: RelationshipViewModel,
    onNavigateBack: () -> Unit,
    onNavigateEdit: (String) -> Unit,
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val successState = detailState as? RelationshipDetailUiState.Success

    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir relacionamento?") },
            text = { Text("Esta ação não pode ser desfeita.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(eventId, onSuccess = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showShareSheet && successState != null) {
        ShareBottomSheet(
            event = successState.event.toUiModel(context = context),
            isPremium = isPremium,
            onDismiss = { showShareSheet = false },
            onNavigateToPremium = { showShareSheet = false },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                    }
                    IconButton(onClick = { onNavigateEdit(eventId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (successState != null) {
                                val event = successState.event
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
                                        viewModel.togglePin(eventId) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (event.isArchived) "Desarquivar" else "Arquivar") },
                                    leadingIcon = { Icon(if (event.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.toggleArchived(eventId, !event.isArchived) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (event.isCompleted) "Reativar" else "Concluir") },
                                    leadingIcon = { Icon(if (event.isCompleted) Icons.Filled.RestartAlt else Icons.Filled.Check, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.toggleCompleted(eventId, !event.isCompleted) 
                                    }
                                )
                            }
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
        }
    ) { padding ->
        when (val s = detailState) {
            is RelationshipDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PurplePrimary)
                }
            }
            is RelationshipDetailUiState.NotFound -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Relacionamento não encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is RelationshipDetailUiState.Success -> {
                RelationshipDetailContent(
                    state = s,
                    viewModel = viewModel,
                    onDeleteClick = { showDeleteDialog = true },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun RelationshipDetailContent(
    state: RelationshipDetailUiState.Success,
    viewModel: RelationshipViewModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = state.event
    val stats = state.stats
    val accentColor = Color(event.colorArgb)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        Spacer(Modifier.height(AppSpacing.small))

        // ── Header ────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(AppShapes.medium)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(event.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.title,
                        style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                    )
                    Text(
                        relationshipTypeLabel(event.relationshipType ?: "other"),
                        style = AppTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Main Stats Card ───────────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.large),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Text(
                    "${stats.totalDays}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(
                    if (stats.totalDays == 1) "dia juntos" else "dias juntos",
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppSpacing.small),
                    color = accentColor.copy(alpha = 0.2f)
                )
                Text(
                    state.primaryText,
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val startDate = event.relationshipStartEpochDay?.let { LocalDate.ofEpochDay(it) }
                if (startDate != null) {
                    Text(
                        "Desde ${startDate.format(DATE_FORMATTER_SHORT)}",
                        style = AppTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Milestones Card ───────────────────────────────────────────────────
        if (stats.nextMilestoneDays != null && stats.daysToNextMilestone != null) {
            InfoCard(
                icon = Icons.Filled.EmojiEvents,
                iconTint = accentColor,
                title = "Próximo marco",
                body = "${stats.nextMilestoneDays} dias juntos",
                detail = if (stats.daysToNextMilestone == 0) "É hoje" else "Faltam ${stats.daysToNextMilestone} dias"
            )
        }

        // ── Anniversaries ─────────────────────────────────────────────────────
        if (event.relationshipAnnualEnabled && stats.daysToNextAnnual != null) {
            InfoCard(
                icon = Icons.Filled.Cake,
                iconTint = accentColor,
                title = "Aniversário anual",
                body = if (stats.daysToNextAnnual == 0) "É hoje" else "Em ${stats.daysToNextAnnual} dias",
                detail = "Mesmo dia e mês da data de início"
            )
        }
        if (event.relationshipMonthlyEnabled && stats.daysToNextMonthly != null) {
            InfoCard(
                icon = Icons.Filled.CalendarMonth,
                iconTint = accentColor,
                title = "Mensário",
                body = if (stats.daysToNextMonthly == 0) "É hoje" else "Em ${stats.daysToNextMonthly} dias",
                detail = "Todo mês no dia ${LocalDate.ofEpochDay(event.relationshipStartEpochDay ?: 0).dayOfMonth}"
            )
        }

        // ── Settings toggles ──────────────────────────────────────────────────
        SectionCard(title = "Configurações") {
            DetailToggleRow(
                label = "Mensário",
                checked = event.relationshipMonthlyEnabled,
                onCheckedChange = { viewModel.toggleMonthly(event, it) }
            )
            DetailToggleRow(
                label = "Aniversário anual",
                checked = event.relationshipAnnualEnabled,
                onCheckedChange = { viewModel.toggleAnnual(event, it) }
            )
        }

        // ── Delete button ─────────────────────────────────────────────────────
        Spacer(Modifier.height(AppSpacing.small))
        OutlinedButton(
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ),
            shape = AppShapes.medium
        ) {
            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Excluir relacionamento", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(AppSpacing.large))
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    detail: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShapes.small)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTypography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    body,
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(detail, style = AppTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.medium)) {
            Text(
                title,
                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(AppSpacing.small))
            content()
        }
    }
}

@Composable
private fun DetailToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = AppTypography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PurplePrimary)
        )
    }
}
