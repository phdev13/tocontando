package com.phdev.quantofalta.feature.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.time.Ticker
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.domain.model.toUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.phdev.quantofalta.domain.model.dateMillis

sealed interface HighlightUiState {
    object Loading : HighlightUiState
    data class Success(val events: List<EventUiModel>) : HighlightUiState
    object Empty : HighlightUiState
}

class HighlightViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository
) : ViewModel() {

    // Single ticker shared across all highlight state flows — avoids multiple 1s coroutines.
    // WhileSubscribed(5000) on the resulting StateFlow means the ticker stops when no UI
    // is collecting (e.g., screen is in background).
    private val ticker = Ticker.tickerFlow(1000)

    // Cache by eventId so that repeated calls from recomposition don't create duplicate flows.
    private val stateFlowCache = mutableMapOf<String, StateFlow<HighlightUiState>>()

    fun getHighlightUiState(id: String): StateFlow<HighlightUiState> {
        return stateFlowCache.getOrPut(id) { buildStateFlow(id) }
    }

    private fun buildStateFlow(id: String): StateFlow<HighlightUiState> {
        val eventsFlow = if (id == "1" || id.isBlank()) {
            repository.getAllEvents().map { events ->
                val active = events.filter { !it.isArchived }
                val pinned = active.filter { it.isPinned }
                if (pinned.isNotEmpty()) {
                    pinned.sortedWith(
                        compareBy<com.phdev.quantofalta.domain.model.Event> { it.type.ordinal }
                            .thenBy { it.targetDate.toEpochDay() }
                    )
                } else {
                    // Se não tiver nenhum pino, pegamos os auto-destacados de cada modo (igual na Home)
                    val activeIncomplete = active.filter { !it.isCompleted }
                    val standardFirst = activeIncomplete.filter { it.type == com.phdev.quantofalta.domain.model.EventType.STANDARD }
                        .minByOrNull { it.targetDate.toEpochDay() }
                    val relFirst = activeIncomplete.filter { it.type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP }
                        .minByOrNull { it.createdAtMillis }
                    val salaryFirst = activeIncomplete.filter { it.type == com.phdev.quantofalta.domain.model.EventType.SALARY }
                        .minByOrNull { it.createdAtMillis }
                    listOfNotNull(standardFirst, relFirst, salaryFirst)
                }
            }
        } else {
            repository.getEventById(id).map { event -> event?.let(::listOf).orEmpty() }
        }

        return combine(eventsFlow, ticker) { events, currentMillis ->
            val uiEvents = events.map { event ->
                event.toUiModel(java.time.Instant.ofEpochMilli(currentMillis), application)
            }
            if (uiEvents.isNotEmpty()) {
                HighlightUiState.Success(uiEvents)
            } else {
                HighlightUiState.Empty
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HighlightUiState.Loading
        )
    }

    override fun onCleared() {
        super.onCleared()
        stateFlowCache.clear()
    }
}
