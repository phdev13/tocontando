package com.phdev.quantofalta.feature.highlight

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.components.SkeletonIcon
import com.phdev.quantofalta.core.designsystem.components.SkeletonText
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.domain.model.EventState
import com.phdev.quantofalta.domain.model.EventType
import com.phdev.quantofalta.domain.model.EventUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighlightScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: HighlightViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiStateFlow = remember(eventId) { viewModel.getHighlightUiState(eventId) }
    val uiState by uiStateFlow.collectAsStateWithLifecycle()

    when (val state = uiState) {
        HighlightUiState.Loading -> HighlightLoading(onBack = onBack)
        HighlightUiState.Empty -> HighlightEmpty(
            onBack = onBack,
            onCreate = { onNavigate(Screen.CreateEvent.route) },
            onChoose = { onNavigate(Screen.Home.route) }
        )
        is HighlightUiState.Success -> {
            val pagerState = rememberPagerState(pageCount = { state.events.size })
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(0.dp),
                    key = { page -> state.events[page].stableId }
                ) { page ->
                    HighlightHeroPage(
                        event = state.events[page],
                        pageOffsetProvider = { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // FIX 1: Botão de voltar — ícone AutoMirrored (correto para Material3)
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = AppSpacing.small, top = AppSpacing.small)
                        .align(Alignment.TopStart)
                        .size(48.dp)
                ) {
                    // Camada de blur separada atrás do conteúdo — glassmorphism correto
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // FIX 2: AutoMirrored.Filled.ArrowBack em vez de Filled.ArrowBack (deprecated no M3)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                }

                if (state.events.size > 1) {
                    HighlightPagerDots(
                        count = state.events.size,
                        selectedIndex = pagerState.currentPage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = AppSpacing.large)
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightLoading(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // FIX 2 (consistência): AutoMirrored também aqui
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                containerColor = Color.Transparent
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SkeletonIcon(size = 120.dp)
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            SkeletonText(width = 220.dp, height = 120.dp)
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            SkeletonText(width = 300.dp, height = 28.dp)
        }
    }
}

@Composable
private fun HighlightEmpty(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onChoose: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Destaque",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Sair")
                    }
                },
                containerColor = Color.Transparent
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center
        ) {
            EmptyState(
                icon = getIconByName("Star"),
                title = "Escolha uma contagem para acompanhar de perto",
                description = "Marque uma contagem como destaque para vê-la aqui em tela cheia.",
                buttonText = "Criar contagem",
                onButtonClick = onCreate,
                testTag = "highlight_empty_state_create_button"
            )
            OutlinedButton(
                onClick = onChoose,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = AppSpacing.large)
            ) {
                Text("Ver minhas contagens")
            }
        }
    }
}

@Composable
private fun HighlightHeroPage(
    event: EventUiModel,
    pageOffsetProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasCover = !event.coverImageUri.isNullOrBlank()

    Box(
        modifier = modifier
            .background(Color.Black)
            .fadeSlideIn()
    ) {
        if (hasCover) {
            val coverRequest = remember(event.coverImageUri) {
                ImageRequest.Builder(context)
                    .data(event.coverImageUri)
                    .size(720, 1280)
                    .scale(Scale.FILL)
                    .precision(Precision.INEXACT)
                    .crossfade(false)
                    .allowHardware(true)
                    .memoryCacheKey("highlight-cover:${event.coverImageUri}")
                    .diskCacheKey("highlight-cover:${event.coverImageUri}")
                    .build()
            }
            HighlightCoverBackground(
                coverRequest = coverRequest,
                pageOffsetProvider = pageOffsetProvider
            )
        } else {
            Icon(
                imageVector = getIconByName(event.iconName),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.09f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 56.dp, y = 24.dp)
                    .size(280.dp)
            )
        }

        // FIX 3: statusBarsPadding adicionado na Column do conteúdo para evitar
        // colisão com a status bar (o botão de back tem, o conteúdo não tinha)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.large, vertical = AppSpacing.medium)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.12f))
            HighlightModePill(type = event.type)
            Spacer(modifier = Modifier.height(AppSpacing.large))
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphism correto: blur em camada separada atrás do border/clip
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                )
                Icon(
                    imageVector = getIconByName(event.iconName),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(62.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            HighlightNumbers(event)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = event.date,
                style = AppTypography.bodyLarge,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
            Text(
                text = event.time,
                style = AppTypography.bodyMedium,
                color = Color.White.copy(alpha = 0.76f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun HighlightCoverBackground(
    coverRequest: ImageRequest,
    pageOffsetProvider: () -> Float
) {
    AsyncImage(
        model = coverRequest,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val pageOffset = pageOffsetProvider()
                // Parallax suave (reduzido de 0.25f para 0.15f)
                translationX = pageOffset * size.width * 0.15f
                // Escala para compensar o deslocamento e evitar bordas cortadas
                scaleX = 1.35f
                scaleY = 1.35f
                // Crossfade alpha (esmaece a imagem conforme ela sai do centro)
                val fadeAlpha = 1f - (kotlin.math.abs(pageOffset) * 0.8f)
                alpha = fadeAlpha.coerceIn(0f, 1f) * 0.85f // 0.85f é o alpha base original
            },
        contentScale = ContentScale.Crop
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Esmaece também a camada escura durante o swipe para ficar uniforme
                alpha = 1f - (kotlin.math.abs(pageOffsetProvider()) * 0.5f)
            }
            .background(Color.Black.copy(alpha = 0.3f))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - (kotlin.math.abs(pageOffsetProvider()) * 0.5f)
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.90f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}

@Composable
private fun HighlightNumbers(event: EventUiModel) {
    // FIX 4: Import explícito de EventState no topo do arquivo — removido inline import
    // A condição agora usa EventState importada corretamente
    val isSpecialState = event.salaryUiState != null
            || event.relationshipUiState != null
            || event.eventState != EventState.ACTIVE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSpecialState) {
            Text(
                text = event.primaryText,
                style = AppTypography.displayLarge.copy(
                    fontSize = if (event.primaryText.length > 14) 54.sp else 68.sp,
                    lineHeight = if (event.primaryText.length > 14) 60.sp else 74.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            Text(
                text = event.title,
                style = AppTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (event.secondaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(
                    text = event.secondaryText,
                    style = AppTypography.bodyLarge,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = event.number,
                style = AppTypography.displayLarge.copy(
                    fontSize = 150.sp,
                    lineHeight = 150.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 8f),
                        blurRadius = 16f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = event.units,
                style = AppTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
            Text(
                text = com.phdev.quantofalta.core.utils.AppCopyProvider.getHighlightMessage(
                    eventId = event.id,
                    iconName = event.iconName,
                    numberStr = event.number,
                    units = event.units,
                    isCompleted = event.isCompleted || event.eventState == EventState.COMPLETED,
                    isSoon = event.isSoon,
                    rawTitle = event.title,
                    eventType = event.type
                ),
                style = AppTypography.headlineMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HighlightModePill(type: EventType) {
    Box(
        modifier = Modifier.clip(AppShapes.small)
    ) {
        // FIX 5: Blur em camada separada atrás do conteúdo — o clip() antes do blur()
        // no original cortava o efeito nas bordas. Agora blur fica numa Box independente.
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Color.White.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color.White.copy(alpha = 0.35f), AppShapes.small)
        )
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = modeLabel(type),
                style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun HighlightPagerDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = index == selectedIndex

            // FIX 6: Animação suave na largura dos dots — sem animateDpAsState a mudança era abrupta
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 22.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dot_width_$index"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = dotWidth, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White
                        else Color.White.copy(alpha = 0.35f)
                    )
            )
        }
    }
}

private fun modeLabel(type: EventType): String = when (type) {
    EventType.STANDARD -> "Evento em destaque"
    EventType.RELATIONSHIP -> "Relacionamento em destaque"
    EventType.SALARY -> "Financeiro em destaque"
}