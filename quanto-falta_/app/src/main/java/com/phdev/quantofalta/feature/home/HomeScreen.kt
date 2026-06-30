package com.phdev.quantofalta.feature.home
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppBottomNav
import com.phdev.quantofalta.core.designsystem.components.AdaptiveContent
import com.phdev.quantofalta.core.designsystem.components.CompactEventCard
import com.phdev.quantofalta.core.designsystem.components.SkeletonBox
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.components.EventLimitModal
import com.phdev.quantofalta.core.designsystem.components.AppContentMaxWidth
import com.phdev.quantofalta.core.designsystem.components.MainEventCard
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.domain.model.EventType
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.feature.createevent.TemplateSelectionBottomSheet
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    targetMode: EventType? = null,
    onTargetModeConsumed: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val homeStateOrNull by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState = homeStateOrNull ?: HomeUiState()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showLimitModal by remember { mutableStateOf(false) }
    val pages = homeState.pages
    val allCardsCount = remember(pages) {
        pages.sumOf { (if (it.featuredCard != null) 1 else 0) + it.compactCards.size }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    LaunchedEffect(targetMode, pages.size) {
        val targetIndex = targetMode?.let { mode -> pages.indexOfFirst { it.mode == mode } } ?: -1
        if (targetIndex >= 0) {
            pagerState.scrollToPage(targetIndex)
            onTargetModeConsumed()
        }
    }
    
    val currentMode = if (pages.isNotEmpty()) {
        pages.getOrNull(pagerState.currentPage)?.mode ?: EventType.STANDARD
    } else {
        EventType.STANDARD
    }

    val standardCardsCount = remember(pages) {
        pages.find { it.mode == EventType.STANDARD }?.let { (if (it.featuredCard != null) 1 else 0) + it.compactCards.size } ?: 0
    }
    val relationshipCardsCount = remember(pages) {
        pages.find { it.mode == EventType.RELATIONSHIP }?.let { (if (it.featuredCard != null) 1 else 0) + it.compactCards.size } ?: 0
    }
    val salaryCardsCount = remember(pages) {
        pages.find { it.mode == EventType.SALARY }?.let { (if (it.featuredCard != null) 1 else 0) + it.compactCards.size } ?: 0
    }

    val handleCreateNavigation: (EventType, String) -> Unit = remember(pages) {
        { mode, route ->
            onNavigate(route)
        }
    }
    
    val handleAddEvent: () -> Unit = {
        showTemplateSheet = true
    }
    
    if (showLimitModal) {
        EventLimitModal(
            onNavigatePremium = { onNavigate(Screen.Premium.route) },
            onManageEvents = { showLimitModal = false },
            onDismiss = { showLimitModal = false }
        )
    }
    
        homeState.celebrationEvent?.let { eventToCelebrate ->
        com.phdev.quantofalta.core.designsystem.components.CelebrationDialog(
            eventName = eventToCelebrate.title,
            onDismiss = {
                viewModel.markCelebrated(eventToCelebrate.stableId)
            }
        )
    }

    if (showTemplateSheet) {
        TemplateSelectionBottomSheet(
            isPremium = isPremium,
            standardLimitReached = standardCardsCount >= PremiumFeature.FREE_EVENT_LIMIT,
            relationshipLimitReached = relationshipCardsCount >= PremiumFeature.FREE_EVENT_LIMIT,
            salaryLimitReached = salaryCardsCount >= PremiumFeature.FREE_EVENT_LIMIT,
            onLimitReached = {
                showTemplateSheet = false
                showLimitModal = true
            },
            onDismiss = { showTemplateSheet = false },
            onTemplateSelected = { title, colorHex, iconName ->
                showTemplateSheet = false
                val safeColor = colorHex.replace("#", "")
                onNavigate(Screen.CreateEvent.createPrefillRoute(title, safeColor, iconName, 1))
            },
            onCustomSelected = {
                showTemplateSheet = false
                onNavigate(Screen.CreateEvent.route)
            },
            onRelationshipSelected = {
                showTemplateSheet = false
                onNavigate(Screen.CreateRelationship.createRoute())
            },
            onSalarySelected = {
                showTemplateSheet = false
                onNavigate(Screen.CreateSalary.route)
            }
        )
    }
    Scaffold(
        topBar = {
            HomeTopBar(
                totalCount = allCardsCount,
                onAdd = handleAddEvent,
                onSettings = { onNavigate(Screen.More.route) }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentRoute = Screen.Home.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { handleCreateNavigation(currentMode, "") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            homeState.isLoading -> HomeLoadingContent(Modifier.padding(paddingValues))
            pages.isEmpty() -> HomeLoadingContent(Modifier.padding(paddingValues))
            else -> HomeCarouselContent(
                modifier = Modifier.padding(paddingValues),
                pages = pages,
                pagerState = pagerState,
                onNavigate = onNavigate,
                onCreateNavigation = handleCreateNavigation
            )
        }
    }
}
@Composable
private fun HomeTopBar(
    totalCount: Int,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = AppContentMaxWidth)
                .heightIn(min = 64.dp)
                .padding(horizontal = AppSpacing.medium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.phdev.quantofalta.R.drawable.ic_padrao),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "To Contando",
                    style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (totalCount == 0) "Comece sua primeira contagem" else "$totalCount contagens ativas",
                    style = AppTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HomeTopBarButton(
                icon = Icons.Default.Add,
                contentDescription = "Criar contagem",
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            HomeTopBarButton(
                icon = Icons.Default.Settings,
                contentDescription = "Configuracoes",
                onClick = onSettings,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun HomeTopBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(containerColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
@Composable
private fun HomeLoadingContent(modifier: Modifier = Modifier) {
    AdaptiveContent(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            item { SkeletonBox(modifier = Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(24.dp)) }
            items(5) { SkeletonBox(modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) }
        }
    }
}
// HomeEmptyContent and HomeQuickCreateChip removed as they are now handled per mode
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCarouselContent(
    modifier: Modifier = Modifier,
    pages: List<HomeModePage>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNavigate: (String) -> Unit,
    onCreateNavigation: (EventType, String) -> Unit
) {
    AdaptiveContent(modifier = modifier.fillMaxSize().fadeSlideIn(delayMillis = 50)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { index -> pages[index].mode.name }
            ) { index ->
                val page = pages[index]
                val featured = page.featuredCard ?: page.compactCards.firstOrNull()

                if (featured == null && page.compactCards.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (page.mode) {
                            EventType.STANDARD -> {
                                EmptyState(
                                    icon = getIconByName("Event"),
                                    title = "Nenhum evento padrão",
                                    description = "Crie sua primeira contagem para acompanhar uma data importante, viagem ou compromisso.",
                                    buttonText = "Criar Evento",
                                    onButtonClick = { onCreateNavigation(EventType.STANDARD, Screen.CreateEvent.route) }
                                )
                            }
                            EventType.RELATIONSHIP -> {
                                EmptyState(
                                    icon = getIconByName("Favorite"),
                                    title = "Nenhum relacionamento",
                                    description = "Acompanhe o tempo juntos e marcos importantes ao lado de quem você ama.",
                                    buttonText = "Criar Relacionamento",
                                    onButtonClick = { onCreateNavigation(EventType.RELATIONSHIP, Screen.CreateRelationship.createRoute()) }
                                )
                            }
                            EventType.SALARY -> {
                                EmptyState(
                                    icon = getIconByName("AttachMoney"),
                                    title = "Nenhum evento financeiro",
                                    description = "Controle o ciclo até o seu próximo salário ou recebimento.",
                                    buttonText = "Criar Financeiro",
                                    onButtonClick = { onCreateNavigation(EventType.SALARY, Screen.CreateSalary.route) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = AppSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        item(key = "header_${page.mode.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = modeTitle(page.mode),
                                        style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = modeDescription(page.mode),
                                        style = AppTypography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ModePill(page.mode)
                            }
                        }

                        if (featured != null) {
                            item(key = "featured_${page.mode.name}_${featured.id}") {
                                val onClick = androidx.compose.runtime.remember(featured.id) {
                                    { onNavigate(detailRouteForHome(featured)) }
                                }
                                if (featured.type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
                                    com.phdev.quantofalta.core.designsystem.components.RelationshipFeaturedCard(
                                        event = featured,
                                        onClick = onClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppSpacing.medium)
                                    )
                                } else {
                                    MainEventCard(
                                        title = featured.title,
                                        date = featured.date,
                                        number = featured.number,
                                        units = featured.units,
                                        color = featured.color,
                                        iconName = featured.iconName,
                                        progress = featured.progress,
                                        contextMessage = featured.contextMessage,
                                        targetDateMillis = featured.dateMillis,
                                        coverImageUri = featured.coverImageUri,
                                        onClick = onClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppSpacing.medium)
                                    )
                                }
                            }
                        }

                        val compactCards = page.compactCards
                        if (compactCards.isNotEmpty()) {
                            item(key = "mode_header_${page.mode.name}") {
                                Text(
                                    text = "Também em ${modeTitle(page.mode).lowercase()}",
                                    style = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .padding(horizontal = AppSpacing.medium)
                                        .padding(top = AppSpacing.small)
                                )
                            }
                            items(
                                items = compactCards,
                                key = { event -> "compact:${page.mode.name}:${event.id}" },
                                contentType = { "compactCard" }
                            ) { event ->
                                val onClick = androidx.compose.runtime.remember(event.id) {
                                    { onNavigate(detailRouteForHome(event)) }
                                }
                                if (event.type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
                                    com.phdev.quantofalta.core.designsystem.components.RelationshipCompactCard(
                                        event = event,
                                        onClick = onClick,
                                        modifier = Modifier.padding(horizontal = AppSpacing.medium)
                                    )
                                } else {
                                    CompactEventCard(
                                        title = event.title,
                                        date = event.date,
                                        number = event.number,
                                        units = event.units,
                                        color = event.color,
                                        iconName = event.iconName,
                                        targetDateMillis = event.dateMillis,
                                        coverImageUri = event.coverImageUri,
                                        onClick = onClick,
                                        modifier = Modifier.padding(horizontal = AppSpacing.medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (pages.size > 1) {
                HomePagerDots(
                    count = pages.size,
                    selectedIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.small, bottom = AppSpacing.medium)
                )
            }
        }
    }
}
// HomeQuickCreateChip removed
@Composable
private fun ModePill(type: EventType) {
    Row(
        modifier = Modifier
            .clip(AppShapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = modeTitle(type),
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun HomePagerDots(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (index == selectedIndex) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                    )
            )
        }
    }
}
private fun modeTitle(type: EventType): String = when (type) {
    EventType.STANDARD -> "Evento"
    EventType.RELATIONSHIP -> "Relacionamento"
    EventType.SALARY -> "Financeiro"
}
private fun modeDescription(type: EventType): String = when (type) {
    EventType.STANDARD -> "Suas datas importantes"
    EventType.RELATIONSHIP -> "Seus momentos especiais"
    EventType.SALARY -> "Seu ciclo financeiro"
}
private fun detailRouteForHome(event: EventUiModel): String {
    if (event.shouldShowCelebration) {
        return Screen.Celebration.createRoute(event.id)
    }
    return when (event.type) {
        EventType.STANDARD -> Screen.EventDetails.createRoute(event.id)
        EventType.RELATIONSHIP -> Screen.RelationshipDetail.createRoute(event.id)
        EventType.SALARY -> Screen.SalaryDetails.createRoute(event.id)
    }
}
