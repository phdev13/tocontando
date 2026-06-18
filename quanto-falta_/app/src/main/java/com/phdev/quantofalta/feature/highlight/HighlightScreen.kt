package com.phdev.quantofalta.feature.highlight

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.domain.model.EventUiModel

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
        HighlightUiState.Loading -> {
            Scaffold { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        HighlightUiState.Empty -> {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "Destaque Coletivo",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.Close, contentDescription = "Sair", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                        containerColor = Color.Transparent
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = getIconByName("Star"),
                        title = "Nenhum evento ativo em destaque",
                        description = "Crie uma contagem regressiva para vê-la brilhar em tela cheia na aba de destaques!",
                        buttonText = "Criar Evento Agora",
                        onButtonClick = { onNavigate(Screen.CreateEvent.route) },
                        testTag = "highlight_empty_state_create_button"
                    )
                }
            }
        }
        
        is HighlightUiState.Success -> {
            val event = state.event
            val infiniteTransition = rememberInfiniteTransition(label = "floating")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetY"
            )
            
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                        },
                        containerColor = Color.Transparent
                    )
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    event.color.copy(alpha = 0.8f),
                                    event.color
                                )
                            )
                        )
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppSpacing.large)
                            .fadeSlideIn(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        
                        Box(
                            modifier = Modifier
                                .graphicsLayer { translationY = offsetY }
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconByName(event.iconName),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                        
                        HighlightAnimatedNumbers(event)
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Text(
                            text = event.date,
                            style = AppTypography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = event.time,
                            style = AppTypography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
                        
                        Spacer(modifier = Modifier.height(AppSpacing.medium))
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightAnimatedNumbers(event: EventUiModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = event.number,
            style = AppTypography.displayLarge.copy(
                fontSize = 160.sp,
                lineHeight = 160.sp,
                fontFeatureSettings = "tnum"
            ),
            color = Color.White
        )
        Text(
            text = event.units,
            style = AppTypography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))
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
            style = AppTypography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
