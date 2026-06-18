package com.phdev.quantofalta.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppBottomNav
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.CompactEventCard
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.components.EventLimitModal
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.feature.createevent.TemplateSelectionBottomSheet
import com.phdev.quantofalta.billing.PremiumFeature

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val events by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    // Featured event: prioritize pinned event, fallback to first future event
    val featuredEvent = remember(events) { 
        events?.firstOrNull { it.isPinned } ?: events?.firstOrNull() 
    }
    // Rest = compact list
    val otherEvents = remember(events, featuredEvent) { 
        events?.filter { it.id != featuredEvent?.id } ?: emptyList() 
    }

    var showTemplateSheet by remember { mutableStateOf(false) }
    var showLimitModal by remember { mutableStateOf(false) }

    // Extracted to avoid duplicating the same logic in onClick AND bounceClick
    val handleAddEvent: () -> Unit = remember(isPremium, events) {
        {
            if (!isPremium && (events?.size ?: 0) >= PremiumFeature.FREE_EVENT_LIMIT) {
                showLimitModal = true
            } else {
                showTemplateSheet = true
            }
        }
    }

    if (showLimitModal) {
        EventLimitModal(
            onNavigatePremium = { onNavigate(Screen.Premium.route) },
            onManageEvents = { showLimitModal = false },
            onDismiss = { showLimitModal = false }
        )
    }

    if (showTemplateSheet) {
        TemplateSelectionBottomSheet(
            onDismiss = { showTemplateSheet = false },
            onTemplateSelected = { title, colorHex, iconName ->
                showTemplateSheet = false
                val safeColor = colorHex.replace("#", "")
                onNavigate(Screen.CreateEvent.createPrefillRoute(title, safeColor, iconName, 1))
            },
            onCustomSelected = {
                showTemplateSheet = false
                onNavigate(Screen.CreateEvent.route)
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tô Contando",
                actions = {
                    IconButton(
                        onClick = handleAddEvent,
                        modifier = Modifier
                            .bounceClick(onClick = handleAddEvent)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Adicionar evento",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.small))
                }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentRoute = Screen.Home.route,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (events == null) {
            // Loading state — empty box prevents flicker while data loads
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else if (events!!.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = getIconByName("Event"),
                    title = "Nenhum evento ativo",
                    description = "Acompanhe seus prazos, viagens ou momentos especiais criados por você.",
                    buttonText = "Criar Meu Primeiro Evento",
                    onButtonClick = handleAddEvent,
                    testTag = "empty_state_create_button"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = AppSpacing.medium, bottom = AppSpacing.large),
            ) {
                // 1. Featured (big) card — full width, centered
                if (featuredEvent != null) {
                    item(key = "featured_card", contentType = "featured_card") {
                        MainEventCard(
                            title = featuredEvent.title,
                            date = featuredEvent.date,
                            number = featuredEvent.number,
                            units = featuredEvent.units,
                            color = featuredEvent.color,
                            iconName = featuredEvent.iconName,
                            progress = featuredEvent.progress,
                            contextMessage = featuredEvent.contextMessage,
                            isToday = featuredEvent.isToday,
                            targetDateMillis = featuredEvent.dateMillis,
                            coverImageUri = featuredEvent.coverImageUri,
                                onClick = { onNavigate(Screen.EventDetails.createRoute(featuredEvent.id)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.medium)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.medium))
                    }
                }

                // 2. The rest of the events as compact cards.
                // PERF FIX: Using itemsIndexed() instead of items() + indexOf()
                // indexOf() was O(n) per item = O(n²) total. itemsIndexed() is O(1).
                if (otherEvents.isNotEmpty()) {
                    itemsIndexed(
                        items = otherEvents,
                        key = { _, event -> event.id },
                        contentType = { _, _ -> "compact_event_card" }
                    ) { _, event ->
                        CompactEventCard(
                            title = event.title,
                            date = event.date,
                            number = event.number,
                            units = event.units,
                            color = event.color,
                            iconName = event.iconName,
                            targetDateMillis = event.dateMillis,
                            coverImageUri = event.coverImageUri,
                            onClick = { onNavigate(Screen.EventDetails.createRoute(event.id)) },
                            modifier = Modifier
                                .padding(horizontal = AppSpacing.medium)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.medium))
                    }
                }

                item { Spacer(modifier = Modifier.height(AppSpacing.large)) }
            }
        }
    }
}

@Composable
fun MiniHighlightCard(
    title: String,
    number: String,
    units: String,
    color: Color,
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Card(
        modifier = modifier
            .bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color,
                            color.copy(
                                red = color.red * 0.7f,
                                green = color.green * 0.7f,
                                blue = (color.blue * 1.3f).coerceAtMost(1f)
                            )
                        )
                    )
                )
                .padding(AppSpacing.medium)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Em breve",
                        style = AppTypography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = offsetY }
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(iconName),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                Text(
                    text = title,
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = number,
                        style = AppTypography.headlineLarge.copy(
                            fontSize = 40.sp,
                            lineHeight = 40.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = units,
                        style = AppTypography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small)
    )
}

@Composable
fun SummarySection(totalActive: Int, totalSoon: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Eventos Ativos",
            value = totalActive.toString(),
            iconName = "List",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            title = "Próximos",
            value = totalSoon.toString(),
            iconName = "Alarm",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    iconName: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getIconByName(iconName),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = AppTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Text(
                text = title,
                style = AppTypography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
