package com.phdev.quantofalta.feature.completed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.phdev.quantofalta.core.designsystem.components.AppBottomNav
import com.phdev.quantofalta.core.designsystem.components.AdaptiveContent
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.CompletedEventCard
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import androidx.compose.foundation.layout.Box
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.domain.model.EventType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.core.AppViewModelProvider
import androidx.compose.runtime.getValue
import com.phdev.quantofalta.core.designsystem.components.fadeSlideIn
import com.phdev.quantofalta.core.designsystem.components.bounceClick

@Composable
fun CompletedScreen(
    onNavigate: (String) -> Unit,
    viewModel: CompletedViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val completedEvents by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Memórias",
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Screen.Home.route) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentRoute = Screen.Completed.route,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (completedEvents == null) {
            AdaptiveContent(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                item {
                    Text(
                        text = "Recentemente",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                }
                items(2) {
                    com.phdev.quantofalta.core.designsystem.components.CompletedEventCardSkeleton(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.medium))
                    Text(
                        text = "Anteriores",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                }
                items(4) {
                    com.phdev.quantofalta.core.designsystem.components.CompletedEventCardSkeleton(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }
        } else if (completedEvents!!.isEmpty()) {
            AdaptiveContent(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = getIconByName("FavoriteBorder"),
                    title = "Nenhuma memória guardada",
                    description = "Os dias que chegaram e viraram memória aparecerão aqui.",
                    buttonText = "Ver Eventos Ativos",
                    onButtonClick = { onNavigate(Screen.Home.route) },
                    testTag = "completed_empty_state_timeline_button"
                )
            }
            }
        } else {
            AdaptiveContent(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                completedEvents!!.forEach { (groupName, events) ->
                    item(key = "header_$groupName") {
                        if (groupName != completedEvents!!.keys.first()) {
                            Spacer(modifier = Modifier.height(AppSpacing.medium))
                        }
                        Text(
                            text = groupName,
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                    }
                    items(events, key = { "completed_memories:${it.visualKey}" }) { event ->
                        val index = events.indexOf(event)
                        CompletedEventCard(
                            event = event,
                            onClick = { 
                                val route = detailRouteForCompleted(event)
                                onNavigate(route) 
                            },
                            modifier = Modifier.fadeSlideIn(delayMillis = (index * 40).coerceAtMost(240))
                        )
                    }
                }
                
                // Bottom Info
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                    Text(
                        text = "Boas lembranças são contagens\nregressivas que já chegaram ao fim. 💜",
                        style = AppTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            }
        }
    }
}

private fun detailRouteForCompleted(event: com.phdev.quantofalta.domain.model.EventUiModel): String = when (event.type) {
    EventType.STANDARD -> Screen.EventDetails.createRoute(event.id)
    EventType.RELATIONSHIP -> Screen.RelationshipDetail.createRoute(event.id)
    EventType.SALARY -> Screen.SalaryDetails.createRoute(event.id)
}
