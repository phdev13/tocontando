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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import java.util.UUID
import com.phdev.quantofalta.billing.EntitlementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn

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
        return combine(
            repository.getEventById(id),
            ticker,
            entitlementManager.hasActivePremium
        ) { event, currentMillis, premium ->
            val visibleEvent = if (premium) event else event?.copy(
                coverImageUri = null,
                isPrivate = false,
                format = com.phdev.quantofalta.domain.model.CountdownFormat.DAYS,
                relationshipMonthlyEnabled = false,
                relationshipAnnualEnabled = false,
                relationshipMilestonesEnabled = false,
            )
            visibleEvent?.toUiModel(java.time.Instant.ofEpochMilli(currentMillis), application)
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    }

    fun toggleCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            if (!isCompleted) {
                val event = repository.getEventById(id).firstOrNull()
                if (event != null) {
                    val now = System.currentTimeMillis()
                    val today = java.time.Instant.now().atZone(java.time.ZoneId.of(event.zoneId)).toLocalDate()
                    if (event.targetDate.isBefore(today) || event.targetDate.isEqual(today)) {
                        val nextTarget = today.plusDays(1)
                        val updatedEvent = event.copy(
                            isCompleted = false,
                            targetDate = nextTarget,
                            createdAtMillis = now
                        )
                        repository.insertEvent(updatedEvent, repository.getRemindersForEvent(id))
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

    fun togglePin(id: String) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEventById(id)
        }
    }

    fun duplicateForNextYear(
        id: String,
        onLimitReached: () -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (!isPremium.value && repository.countActiveEvents().first() >= com.phdev.quantofalta.billing.PremiumFeature.FREE_EVENT_LIMIT) {
                onLimitReached()
                return@launch
            }
            val event = repository.getEventById(id).firstOrNull() ?: return@launch
            
            // Generate the date for next year
            val nextYearDate = event.targetDate.plusYears(1)
            
            val newEvent = event.copy(
                id = UUID.randomUUID().toString(),
                targetDate = nextYearDate,
                createdAtMillis = System.currentTimeMillis(),
                isCompleted = false,
                isArchived = false,
                coverImageUri = com.phdev.quantofalta.core.utils.ImageStorageHelper
                    .duplicateInternalImage(application, event.coverImageUri)
            )

            val clonedReminders = repository.getRemindersForEvent(id).map { reminder ->
                reminder.copy(
                    id = UUID.randomUUID().toString(),
                    eventId = newEvent.id,
                    createdAtMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            }
            repository.insertEvent(newEvent, clonedReminders)
            
            // Archive the old event
            repository.markEventAsArchived(id, true)
            
            onComplete()
        }
    }
}
