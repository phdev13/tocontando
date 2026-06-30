package com.phdev.quantofalta.feature.celebration

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider

@Composable
fun CelebrationScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: CelebrationViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    when (val state = uiState) {
        is CelebrationUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CelebrationUiState.Success -> {
            CelebrationContent(
                event = state.event,
                onContinue = {
                    viewModel.markCelebrationViewed(eventId)
                    onNavigateBack()
                }
            )
        }
        is CelebrationUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Erro ao carregar celebração.")
            }
        }
    }
}

@Composable
fun CelebrationContent(
    event: com.phdev.quantofalta.domain.model.Event,
    onContinue: () -> Unit
) {
    val primaryColor = Color(event.colorArgb)
    
    // Scale animation for the central icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Dark immersive background
    ) {
        // Optional Cover Image
        if (event.coverImageUri != null) {
            AsyncImage(
                model = event.coverImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF121212).copy(alpha = 0.8f),
                                Color(0xFF121212)
                            )
                        )
                    )
            )
        } else {
            // Gradient Overlay based on primary color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                Color(0xFF121212)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(event.iconName),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            val message = when {
                event.isSalaryEvent() -> "Chegou o dia do seu pagamento!"
                event.isRelationshipEvent() -> "Um marco especial alcançado!"
                else -> "A espera acabou!"
            }

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.small))

            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            
            Text(
                text = "Aproveite esse momento. O ciclo foi concluído com sucesso.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
            
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(AppSpacing.small))
                Text(
                    text = "Continuar",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
