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
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.CompletedEventCard
import com.phdev.quantofalta.core.designsystem.components.EmptyState
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import androidx.compose.foundation.layout.Box
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.navigation.Screen
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
    val todayEvents = remember(completedEvents) { 
        completedEvents.filter { event ->
            event.date == "Concluído" // "Concluído" is the text used in toUiModel for past/done events
            // We could improve it by looking at the actual date, but let's just divide by "today/soon" and "past"
            // Wait, if it's done naturally, date="Concluído"
            // If it was forced to complete? date is formatted normally.
            true
        }
    }
    
    val nextDoneEvents = remember(completedEvents) { completedEvents.take(3) }
    val olderDoneEvents = remember(completedEvents) { completedEvents.drop(3) }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Concluídos",
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
        if (completedEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = getIconByName("VerifiedUser"),
                    title = "Nenhum evento concluído",
                    description = "Suas contagens regressivas concluídas, passadas ou marcadas como prontas aparecerão aqui.",
                    buttonText = "Ver Eventos Ativos",
                    onButtonClick = { onNavigate(Screen.Home.route) },
                    testTag = "completed_empty_state_timeline_button"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // Section 1
                if (nextDoneEvents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recentemente",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                    }
                    items(nextDoneEvents, key = { it.id }) { event ->
                        val index = nextDoneEvents.indexOf(event)
                        CompletedEventCard(
                            title = event.title,
                            date = event.date,
                            color = event.color,
                            iconName = event.iconName,
                            onClick = { onNavigate(Screen.EventDetails.createRoute(event.id)) },
                            modifier = Modifier.fadeSlideIn(delayMillis = (index * 40).coerceAtMost(240))
                        )
                    }
                }

                // Section 2
                if (olderDoneEvents.isNotEmpty()) {
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
                    items(olderDoneEvents, key = { it.id }) { event ->
                        val index = olderDoneEvents.indexOf(event)
                        CompletedEventCard(
                            title = event.title,
                            date = event.date,
                            color = event.color,
                            iconName = event.iconName,
                            onClick = { onNavigate(Screen.EventDetails.createRoute(event.id)) },
                            modifier = Modifier.fadeSlideIn(delayMillis = ((index + 3) * 40).coerceAtMost(240))
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
