package com.phdev.quantofalta.feature.eventdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.time.Ticker
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.domain.model.toUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import java.util.Calendar
import java.util.UUID
import com.phdev.quantofalta.billing.EntitlementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class EventDetailsViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    /**
     * Details screen shows a live countdown with seconds-level precision.
     * 1-second ticker is intentional here since the user is actively watching it.
     * WhileSubscribed(5000) on the collector means the ticker stops when the screen
     * leaves composition (back navigation), so no wasted CPU in background.
     */
    private val ticker = Ticker.tickerFlow(1000)

    val isPremium = entitlementManager.hasActivePremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun getEventUiState(id: String): Flow<EventUiModel?> {
        return combine(repository.getEventById(id), ticker) { event, currentMillis ->
            event?.toUiModel(java.time.Instant.ofEpochMilli(currentMillis), application)
        }
    }

    fun toggleCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            if (!isCompleted) {
                val event = repository.getEventById(id).firstOrNull()
                if (event != null) {
                    val now = System.currentTimeMillis()
                    val today = java.time.Instant.now().atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                    if (event.targetDate.isBefore(today) || event.targetDate.isEqual(today)) {
                        val nextTarget = today.plusDays(1)
                        val updatedEvent = event.copy(
                            isCompleted = false,
                            targetDate = nextTarget,
                            createdAtMillis = now
                        )
                        repository.insertEvent(updatedEvent)
                        return@launch
                    }
                }
            }
            repository.markEventAsCompleted(id, isCompleted)
        }
    }

    fun toggleArchived(id: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.markEventAsArchived(id, isArchived)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEventById(id)
        }
    }

    fun duplicateForNextYear(id: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val event = repository.getEventById(id).firstOrNull() ?: return@launch
            
            // Generate the date for next year
            val nextYearDate = event.targetDate.plusYears(1)
            
            val newEvent = event.copy(
                id = UUID.randomUUID().toString(),
                targetDate = nextYearDate,
                createdAtMillis = System.currentTimeMillis(),
                isCompleted = false,
                isArchived = false
            )
            
            repository.insertEvent(newEvent)
            
            // Archive the old event
            repository.markEventAsArchived(id, true)
            
            onComplete()
        }
    }
}
