package com.phdev.quantofalta.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.database.EventTimelineEntity
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineBottomSheet(
    eventId: String,
    onDismiss: () -> Unit,
    viewModel: TimelineViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val timeline by remember(eventId) { viewModel.getTimeline(eventId) }.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = AppSpacing.medium)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.large)
        ) {
            Text(
                text = "Linha do Tempo",
                style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            
            if (timeline.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum histórico disponível.",
                        style = AppTypography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(timeline) { index, entry ->
                        val isLast = index == timeline.lastIndex
                        TimelineItem(entry = entry, isLast = isLast)
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(entry: EventTimelineEntity, isLast: Boolean) {
    val dateStr = remember(entry.timestampMillis) {
        com.phdev.quantofalta.core.time.TimeUtils.formatTimelineDate(entry.timestampMillis)
    }
    
    val color = when (entry.type) {
        "CREATED" -> Color(0xFF34C759)
        "COMPLETED" -> Color(0xFF007AFF)
        "RESTORED", "UNARCHIVED" -> Color(0xFFFF9F0A)
        "ARCHIVED" -> Color(0xFF8E8E93)
        "TITLE_CHANGED", "DATE_CHANGED" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Coluna da Esquerda (Linha e Ponto)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp).fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(AppSpacing.medium))
        
        // Coluna da Direita (Conteúdo)
        Column {
            Text(
                text = entry.description,
                style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateStr,
                style = AppTypography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
