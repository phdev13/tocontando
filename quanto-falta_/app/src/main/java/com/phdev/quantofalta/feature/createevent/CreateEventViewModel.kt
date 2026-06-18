package com.phdev.quantofalta.feature.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.phdev.quantofalta.billing.EntitlementManager
import java.util.UUID

class CreateEventViewModel(
    private val repository: EventRepository,
    private val entitlementManager: EntitlementManager
) : ViewModel() {
    
    val isPremium = entitlementManager.hasActivePremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    suspend fun getEvent(id: String): Event? {
        return repository.getEventById(id).firstOrNull()
    }
    
    fun saveEvent(
        id: String?,
        title: String,
        dateMillis: Long,
        unit: String,
        colorArgb: Int,
        iconName: String,
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        coverImageUri: String? = null
    ) {
        viewModelScope.launch {
            if (isPinned) {
                repository.unpinAllEvents()
            }
            
            val existing = id?.let { repository.getEventById(it).firstOrNull() }
            
            val zdt = java.time.Instant.ofEpochMilli(dateMillis).atZone(java.time.ZoneId.systemDefault())
            
            val event = Event(
                id = id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                iconName = iconName,
                colorArgb = colorArgb,
                targetDate = zdt.toLocalDate(),
                targetTime = zdt.toLocalTime(),
                zoneId = zdt.zone.id,
                referenceDate = null,
                format = runCatching { com.phdev.quantofalta.domain.model.CountdownFormat.valueOf(unit) }.getOrDefault(com.phdev.quantofalta.domain.model.CountdownFormat.DAYS),
                direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
                isCompleted = existing?.isCompleted ?: false,
                isArchived = existing?.isArchived ?: false,
                isPrivate = isPrivate,
                isPinned = isPinned,
                coverImageUri = coverImageUri ?: existing?.coverImageUri
            )
            
            // Cria um lembrete padrao pro momento exato do evento
            val defaultReminder = com.phdev.quantofalta.core.notifications.model.EventReminder(
                id = UUID.randomUUID().toString(),
                eventId = event.id,
                triggerType = com.phdev.quantofalta.core.notifications.model.TriggerType.EXACT
            )
            
            repository.insertEvent(event, listOf(defaultReminder))
        }
    }
}
