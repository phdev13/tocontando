package com.phdev.quantofalta.feature.standard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.phdev.quantofalta.billing.EntitlementManager
import java.util.UUID

class CreateEventViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository,
    private val permissionsUseCase: com.phdev.quantofalta.domain.usecase.PermissionsUseCase
) : ViewModel() {
    
    val isPremium = permissionsUseCase.isPremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    suspend fun getEvent(id: String): Event? {
        return repository.getEventById(id).firstOrNull()
    }

    suspend fun getReminders(id: String) = repository.getRemindersForEvent(id)
    
    fun saveEvent(
        id: String?,
        title: String,
        dateMillis: Long,
        unit: String,
        colorArgb: Int,
        iconName: String,
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        coverImageUri: String? = null,
        standardModeStyle: String = com.phdev.quantofalta.domain.model.mode.StandardCardStyle.CLASSIC.styleId,
        remindAtEvent: Boolean = true,
        remindOneHourBefore: Boolean = false,
        remindOneDayBefore: Boolean = false,
        onSaved: () -> Unit = {},
        onLimitReached: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val currentCount = repository.countActiveEventsByType(com.phdev.quantofalta.domain.model.EventType.STANDARD).first()
                if (
                    id == null &&
                    !permissionsUseCase.canCreateEvent(currentCount, com.phdev.quantofalta.domain.model.EventType.STANDARD, isPremium.value)
                ) {
                    onLimitReached()
                    return@launch
                }
                if (isPinned) {
                    repository.unpinAllEventsForType(com.phdev.quantofalta.domain.model.EventType.STANDARD)
                }

                val existing = id?.let { repository.getEventById(it).firstOrNull() }
                val premium = isPremium.value
                val zdt = java.time.Instant.ofEpochMilli(dateMillis).atZone(java.time.ZoneId.systemDefault())
                val requestedFormat = runCatching {
                    com.phdev.quantofalta.domain.model.CountdownFormat.valueOf(unit)
                }.getOrDefault(com.phdev.quantofalta.domain.model.CountdownFormat.DAYS)
                val selectedFormat = if (
                    requestedFormat.tier == com.phdev.quantofalta.domain.model.FormatTier.FREE ||
                    isPremium.value ||
                    existing?.format == requestedFormat
                ) requestedFormat else com.phdev.quantofalta.domain.model.CountdownFormat.DAYS

                val baseEvent = existing ?: Event(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    iconName = iconName,
                    colorArgb = colorArgb,
                    targetDate = zdt.toLocalDate(),
                    targetTime = zdt.toLocalTime(),
                    zoneId = zdt.zone.id,
                    referenceDate = null,
                    format = selectedFormat,
                    direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
                    createdAtMillis = System.currentTimeMillis()
                )
                val event = com.phdev.quantofalta.core.validation.AppDataValidator.validateEvent(baseEvent.copy(
                    title = title.trim(),
                    iconName = com.phdev.quantofalta.billing.PremiumPolicy.allowedIcon(iconName, premium),
                    colorArgb = com.phdev.quantofalta.billing.PremiumPolicy.allowedColor(colorArgb, premium),
                    targetDate = zdt.toLocalDate(),
                    targetTime = zdt.toLocalTime(),
                    zoneId = zdt.zone.id,
                    referenceDate = null,
                    format = selectedFormat,
                    direction = com.phdev.quantofalta.domain.model.CountdownDirection.AUTO,
                    isCompleted = existing?.isCompleted ?: false,
                    isArchived = existing?.isArchived ?: false,
                    isPrivate = premium && isPrivate,
                    isPinned = isPinned,
                    coverImageUri = coverImageUri.takeIf { premium },
                    standardModeStyle = com.phdev.quantofalta.domain.model.mode.StandardCardStyle.fromId(standardModeStyle),
                    updatedAtMillis = System.currentTimeMillis()
                ))

                val existingReminders = if (existing != null) repository.getRemindersForEvent(event.id) else emptyList()
                val preservedReminders = existingReminders.filter { reminder ->
                    premium && (
                        reminder.triggerType == com.phdev.quantofalta.core.notifications.model.TriggerType.CUSTOM ||
                            (reminder.triggerType == com.phdev.quantofalta.core.notifications.model.TriggerType.OFFSET &&
                                reminder.offsetMinutes !in setOf(60, 1440))
                    )
                }
                fun reminderId(type: com.phdev.quantofalta.core.notifications.model.TriggerType, offset: Int?) =
                    existingReminders.firstOrNull { it.triggerType == type && it.offsetMinutes == offset }?.id
                        ?: UUID.randomUUID().toString()

                val reminders = buildList {
                    addAll(preservedReminders)
                    if (remindAtEvent) {
                        add(
                            com.phdev.quantofalta.core.notifications.model.EventReminder(
                                id = reminderId(com.phdev.quantofalta.core.notifications.model.TriggerType.EXACT, null),
                                eventId = event.id,
                                triggerType = com.phdev.quantofalta.core.notifications.model.TriggerType.EXACT
                            )
                        )
                    }
                    if (isPremium.value && remindOneHourBefore) {
                        add(
                            com.phdev.quantofalta.core.notifications.model.EventReminder(
                                id = reminderId(com.phdev.quantofalta.core.notifications.model.TriggerType.OFFSET, 60),
                                eventId = event.id,
                                triggerType = com.phdev.quantofalta.core.notifications.model.TriggerType.OFFSET,
                                offsetMinutes = 60
                            )
                        )
                    }
                    if (isPremium.value && remindOneDayBefore) {
                        add(
                            com.phdev.quantofalta.core.notifications.model.EventReminder(
                                id = reminderId(com.phdev.quantofalta.core.notifications.model.TriggerType.OFFSET, 1440),
                                eventId = event.id,
                                triggerType = com.phdev.quantofalta.core.notifications.model.TriggerType.OFFSET,
                                offsetMinutes = 1440
                            )
                        )
                    }
                }

                val validatedReminders =
                    com.phdev.quantofalta.core.validation.AppDataValidator.validateReminders(event.id, reminders)
                repository.insertEvent(event, validatedReminders)
                if (existing?.coverImageUri != null && existing.coverImageUri != coverImageUri) {
                    com.phdev.quantofalta.core.utils.ImageStorageHelper.deleteInternalImage(
                        application,
                        existing.coverImageUri
                    )
                }
                onSaved()
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }
}
