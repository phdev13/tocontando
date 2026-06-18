package com.phdev.quantofalta.data.repository

import android.content.Context
import com.phdev.quantofalta.core.database.EventDao
import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.core.database.EventReminderDao
import com.phdev.quantofalta.core.database.EventReminderEntity
import com.phdev.quantofalta.core.database.EventTimelineDao
import com.phdev.quantofalta.core.database.EventTimelineEntity
import com.phdev.quantofalta.core.database.OutboxDao
import com.phdev.quantofalta.core.database.OutboxEntity
import com.phdev.quantofalta.core.database.SyncState
import com.phdev.quantofalta.core.feedback.SmartFeedbackManager
import com.phdev.quantofalta.core.notifications.model.EventReminder
import com.phdev.quantofalta.core.notifications.model.TriggerType
import com.phdev.quantofalta.domain.model.Event
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import com.phdev.quantofalta.feature.widget.EventWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import com.phdev.quantofalta.core.sync.SyncWorker
import com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler
import java.util.UUID

class EventRepository(
    private val context: Context,
    private val eventDao: EventDao,
    private val eventReminderDao: EventReminderDao,
    private val eventTimelineDao: EventTimelineDao,
    private val outboxDao: OutboxDao,
    private val smartFeedbackManager: SmartFeedbackManager,
    private val analyticsManager: AnalyticsManager
) {
    fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun countActiveEvents(): Flow<Int> = eventDao.countActiveEvents()
    fun countCompletedEvents(): Flow<Int> = eventDao.countCompletedEvents()

    fun getEventById(id: String): Flow<Event?> {
        return eventDao.getEventById(id).map { it?.toDomainModel() }
    }

    suspend fun unpinAllEvents() {
        eventDao.unpinAllEvents()
        val allEvents = eventDao.getAllEventsSync()
        allEvents.forEach { event ->
            if (event.syncState == SyncState.PENDING) {
                outboxDao.insertOrReplace(OutboxEntity(event.id, "u", event.localRevision, System.currentTimeMillis()))
            }
        }
        SyncWorker.enqueue(context)
    }

    suspend fun insertEvent(event: Event, reminders: List<EventReminder> = emptyList()) {
        val existing = eventDao.getEventByIdSync(event.id)
        val op = if (existing == null) "c" else "u"
        val nextRevision = (existing?.localRevision ?: 0) + 1
        
        val entity = event.toEntityModel().copy(
            syncState = SyncState.PENDING,
            localRevision = nextRevision,
            serverRevision = existing?.serverRevision ?: 0,
            updatedAt = System.currentTimeMillis(),
            deletedAt = existing?.deletedAt
        )

        // Cancela os alarmes antigos antes de alterar o banco
        val oldReminders = eventReminderDao.getRemindersForEventSync(event.id)
        oldReminders.forEach { oldReminder ->
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, oldReminder.id)
        }
        com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, entity.id)

        eventDao.insertEvent(entity)

        // Limpa antigos e insere novos reminders no banco
        eventReminderDao.deleteRemindersForEvent(entity.id)

        if (!entity.isCompleted && !entity.isArchived) {
            com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, entity)
            if (reminders.isNotEmpty()) {
                eventReminderDao.insertReminders(reminders.map { it.toEntityModel() })
                reminders.forEach { reminder ->
                    com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleReminder(context, entity, reminder)
                }
            }
        } else if (reminders.isNotEmpty()) {
            eventReminderDao.insertReminders(reminders.map { it.toEntityModel() })
        }

        // Timeline log
        if (existing == null) {
            eventTimelineDao.insertTimelineEntry(
                EventTimelineEntity(
                    id = UUID.randomUUID().toString(),
                    eventId = event.id,
                    type = "CREATED",
                    description = "Evento criado",
                    timestampMillis = System.currentTimeMillis()
                )
            )
            // Trigger smart feedback
            smartFeedbackManager.recordEventCreated()

            // Track via the official analytics pipeline (offline-first, batched, respects privacy settings)
            analyticsManager.track(AnalyticsEvent.EventCreated)
        } else {
            val now = System.currentTimeMillis()
            if (existing.title != event.title) {
                eventTimelineDao.insertTimelineEntry(
                    EventTimelineEntity(
                        id = UUID.randomUUID().toString(),
                        eventId = event.id,
                        type = "TITLE_CHANGED",
                        description = "Nome alterado",
                        timestampMillis = now
                    )
                )
            }
            if (existing.targetDate != event.targetDate.toEpochDay() || existing.targetTime != event.targetTime?.toSecondOfDay()) {
                eventTimelineDao.insertTimelineEntry(
                    EventTimelineEntity(
                        id = UUID.randomUUID().toString(),
                        eventId = event.id,
                        type = "DATE_CHANGED",
                        description = "Data/Horário alterado",
                        timestampMillis = now
                    )
                )
            }
        }
        
        outboxDao.insertOrReplace(OutboxEntity(entity.id, op, nextRevision, System.currentTimeMillis()))
        SyncWorker.enqueue(context)
        
        WidgetUpdateScheduler(context).updateWidgetsForEvent(event.id)
    }

    suspend fun deleteEventById(id: String) {
        val reminders = eventReminderDao.getRemindersForEventSync(id)
        reminders.forEach { reminder ->
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)
        }
        com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, id)
        
        val existing = eventDao.getEventByIdSync(id)
        if (existing != null) {
            eventDao.deleteEventById(id)
            val updated = eventDao.getEventByIdSync(id) // fetch logical deleted entity
            if (updated != null) {
                outboxDao.insertOrReplace(OutboxEntity(id, "d", updated.localRevision, System.currentTimeMillis()))
                SyncWorker.enqueue(context)
            }
        }
        WidgetUpdateScheduler(context).updateWidgetsForEvent(id)
    }

    suspend fun markEventAsCompleted(id: String, isCompleted: Boolean) {
        eventDao.updateEventCompletedStatus(id, isCompleted)
        val event = eventDao.getEventByIdSync(id)
        if (event != null) {
            outboxDao.insertOrReplace(OutboxEntity(id, "u", event.localRevision, System.currentTimeMillis()))
            SyncWorker.enqueue(context)
        }
        eventTimelineDao.insertTimelineEntry(
            EventTimelineEntity(
                id = UUID.randomUUID().toString(),
                eventId = id,
                type = if (isCompleted) "COMPLETED" else "RESTORED",
                description = if (isCompleted) "Evento concluído" else "Evento reativado",
                timestampMillis = System.currentTimeMillis()
            )
        )
        if (isCompleted) {
            val reminders = eventReminderDao.getRemindersForEventSync(id)
            reminders.forEach { reminder ->
                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)
            }
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, id)
        } else {
            val event = eventDao.getEventByIdSync(id)
            if (event != null) {
                com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, event)
            }
        }
    }

    suspend fun markEventAsArchived(id: String, isArchived: Boolean) {
        eventDao.updateEventArchivedStatus(id, isArchived)
        val event = eventDao.getEventByIdSync(id)
        if (event != null) {
            outboxDao.insertOrReplace(OutboxEntity(id, "u", event.localRevision, System.currentTimeMillis()))
            SyncWorker.enqueue(context)
        }
        eventTimelineDao.insertTimelineEntry(
            EventTimelineEntity(
                id = UUID.randomUUID().toString(),
                eventId = id,
                type = if (isArchived) "ARCHIVED" else "UNARCHIVED",
                description = if (isArchived) "Evento arquivado" else "Evento desarquivado",
                timestampMillis = System.currentTimeMillis()
            )
        )
        if (isArchived) {
            val reminders = eventReminderDao.getRemindersForEventSync(id)
            reminders.forEach { reminder ->
                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)
            }
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, id)
        } else {
            val event = eventDao.getEventByIdSync(id)
            if (event != null) {
                com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, event)
            }
        }
    }

    suspend fun deleteAllEvents() {
        deleteAllEventsSafely()
    }

    suspend fun deleteAllEventsSafely() {
        val allEvents = eventDao.getAllEventsSync()
        allEvents.forEach { event ->
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, event.id)
            val reminders = eventReminderDao.getRemindersForEventSync(event.id)
            reminders.forEach { reminder ->
                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)
            }
            outboxDao.insertOrReplace(OutboxEntity(event.id, "d", event.localRevision + 1, System.currentTimeMillis()))
        }
        eventDao.deleteAllEvents()
        SyncWorker.enqueue(context)
        WidgetUpdateScheduler(context).updateAllWidgets()
    }
}

fun EventEntity.toDomainModel(): Event {
    val targetDateLocal = java.time.LocalDate.ofEpochDay(targetDate)
    val targetTimeLocal = targetTime?.let { java.time.LocalTime.ofSecondOfDay(it.toLong()) }
    val referenceDateLocal = referenceDate?.let { java.time.LocalDate.ofEpochDay(it) }
    val formatEnum = runCatching { com.phdev.quantofalta.domain.model.CountdownFormat.valueOf(format) }
        .getOrDefault(com.phdev.quantofalta.domain.model.CountdownFormat.DAYS)
    val directionEnum = runCatching { com.phdev.quantofalta.domain.model.CountdownDirection.valueOf(direction) }
        .getOrDefault(com.phdev.quantofalta.domain.model.CountdownDirection.AUTO)

    return Event(
        id = id,
        title = title,
        iconName = iconName,
        colorArgb = colorArgb,
        targetDate = targetDateLocal,
        targetTime = targetTimeLocal,
        zoneId = zoneId,
        referenceDate = referenceDateLocal,
        format = formatEnum,
        direction = directionEnum,
        createdAtMillis = createdAtMillis,
        isCompleted = isCompleted,
        isArchived = isArchived,
        isPrivate = isPrivate,
        isPinned = isPinned,
        coverImageUri = coverImageUri
    )
}

fun Event.toEntityModel(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        colorArgb = colorArgb,
        iconName = iconName,
        targetDate = targetDate.toEpochDay(),
        targetTime = targetTime?.toSecondOfDay(),
        zoneId = zoneId,
        referenceDate = referenceDate?.toEpochDay(),
        format = format.name,
        direction = direction.name,
        createdAtMillis = createdAtMillis,
        isCompleted = isCompleted,
        isArchived = isArchived,
        isPrivate = isPrivate,
        isPinned = isPinned,
        coverImageUri = coverImageUri
    )
}

fun EventReminder.toEntityModel(): EventReminderEntity {
    return EventReminderEntity(
        id = id,
        eventId = eventId,
        triggerType = triggerType.name,
        offsetMinutes = offsetMinutes,
        customDateTimeMillis = customDateTimeMillis,
        enabled = enabled,
        allowSnooze = allowSnooze,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
