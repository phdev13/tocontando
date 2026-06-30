package com.phdev.quantofalta.data.repository



import android.content.Context
import android.util.Log
import com.phdev.quantofalta.core.database.EventDao

import com.phdev.quantofalta.core.database.EventEntity

import com.phdev.quantofalta.core.database.EventReminderDao

import com.phdev.quantofalta.core.database.EventReminderEntity

import com.phdev.quantofalta.core.database.EventTimelineDao

import com.phdev.quantofalta.core.database.EventTimelineEntity

import com.phdev.quantofalta.core.database.SyncOperationDao

import com.phdev.quantofalta.core.database.SyncOperationEntity

import com.phdev.quantofalta.core.database.SyncOperationStatus

import com.phdev.quantofalta.core.database.SyncState

import com.phdev.quantofalta.core.feedback.SmartFeedbackManager

import com.phdev.quantofalta.core.notifications.model.EventReminder

import com.phdev.quantofalta.core.notifications.model.TriggerType

import com.phdev.quantofalta.domain.model.Event

import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.phdev.quantofalta.core.sync.SyncWorker

import com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler

import java.util.UUID
class EventRepository(
    private val context: Context,

    private val eventDao: EventDao,

    private val eventReminderDao: EventReminderDao,

    private val eventTimelineDao: EventTimelineDao,

    private val syncOperationDao: SyncOperationDao,

    private val smartFeedbackManager: SmartFeedbackManager,

    private val analyticsManager: AnalyticsManager,

    private val entitlementManager: com.phdev.quantofalta.billing.EntitlementManager
) {
    private companion object {
        const val TAG = "EventRepository"
    }

    fun getAllEvents(): Flow<List<Event>> {

        return eventDao.getAllEvents().map { entities ->

            entities.map { it.toDomainModel() }

        }

    }



    fun countActiveEvents(): Flow<Int> = eventDao.countActiveEvents()
    fun countActiveEventsByType(type: com.phdev.quantofalta.domain.model.EventType): Flow<Int> = eventDao.countActiveEventsByType(type.name)

    fun countCompletedEvents(): Flow<Int> = eventDao.countCompletedEvents()



    fun getEventById(id: String): Flow<Event?> {

        return eventDao.getEventById(id).map { it?.toDomainModel() }

    }



    fun getActiveEvents(): Flow<List<Event>> =

        eventDao.getActiveEvents().map { entities -> entities.map { it.toDomainModel() } }



    suspend fun getRemindersForEvent(id: String): List<EventReminder> =

        eventReminderDao.getRemindersForEventSync(id).map { entity ->

            EventReminder(

                id = entity.id,

                eventId = entity.eventId,

                triggerType = runCatching { TriggerType.valueOf(entity.triggerType) }.getOrDefault(TriggerType.EXACT),

                offsetMinutes = entity.offsetMinutes,

                customDateTimeMillis = entity.customDateTimeMillis,

                enabled = entity.enabled,

                allowSnooze = entity.allowSnooze,

                soundEnabled = entity.soundEnabled,

                vibrationEnabled = entity.vibrationEnabled,

                createdAtMillis = entity.createdAtMillis,

                updatedAtMillis = entity.updatedAtMillis

            )

        }



    suspend fun unpinAllEventsForType(type: com.phdev.quantofalta.domain.model.EventType) {

        val pinnedEntitiesOfType = eventDao.getAllEventsSync()

            .filter { it.isPinned && it.toDomainModel().type == type }



        pinnedEntitiesOfType.forEach { previous ->
            eventDao.updateEventPinnedStatus(previous.id, false)
            enqueueCardSyncSafely(previous.id, "UPDATE")
        }
        if (pinnedEntitiesOfType.isNotEmpty()) {
            runCatching { SyncWorker.enqueue(context) }
                .onFailure { Log.w(TAG, "Failed to enqueue sync after unpin", it) }
        }
    }


    suspend fun unpinAllEvents() {

        val pinnedEvents = eventDao.getAllEventsSync().filter { it.isPinned }

        eventDao.unpinAllEvents()
        pinnedEvents.forEach { previous ->
            enqueueCardSyncSafely(previous.id, "UPDATE")
        }
        runCatching { SyncWorker.enqueue(context) }
            .onFailure { Log.w(TAG, "Failed to enqueue sync after unpin all", it) }
    }



    suspend fun togglePin(id: String) {

        val entity = eventDao.getEventByIdSync(id) ?: return

        val event = entity.toDomainModel()

        val newPinnedStatus = !event.isPinned



        // If we're pinning this one, we should unpin all others of the same type

        if (newPinnedStatus) {

            unpinAllEventsForType(event.type)

        }



        eventDao.updateEventPinnedStatus(id, newPinnedStatus)



        // Re-fetch to get updated server revision if needed, but we can just use the one we had

        enqueueCardSyncSafely(id, "UPDATE")
        updateWidgetsForEventSafely(id)
    }



    suspend fun insertEvent(event: Event, reminders: List<EventReminder> = emptyList()) {

        val premium = entitlementManager.hasActivePremium.first()

        val policyEvent = if (premium) event else event.copy(

            iconName = com.phdev.quantofalta.billing.PremiumPolicy.allowedIcon(event.iconName, false),

            colorArgb = com.phdev.quantofalta.billing.PremiumPolicy.allowedColor(event.colorArgb, false),

            format = com.phdev.quantofalta.domain.model.CountdownFormat.DAYS,

            isPrivate = false,

            isPinned = false,

            coverImageUri = null,

            relationshipMonthlyEnabled = false,

            relationshipAnnualEnabled = false,

            relationshipMilestonesEnabled = false,

        )

        val validatedEvent = com.phdev.quantofalta.core.validation.AppDataValidator.validateEvent(policyEvent)

        if (

            validatedEvent.coverImageUri != null &&

            !com.phdev.quantofalta.core.utils.ImageStorageHelper.isAvailable(validatedEvent.coverImageUri)

        ) {

            throw com.phdev.quantofalta.core.validation.DataValidationException("A imagem de capa não está disponível.")

        }

        val policyReminders = if (premium) reminders else reminders

            .filter { it.triggerType == TriggerType.EXACT }

            .take(1)

        val validatedReminders =

            com.phdev.quantofalta.core.validation.AppDataValidator.validateReminders(validatedEvent.id, policyReminders)

        val existing = eventDao.getEventByIdSync(validatedEvent.id)



        if (existing != null) {

            val existingDomain = existing.toDomainModel()

            if (existingDomain.type != validatedEvent.type) {

                // Feature constraint (Rule 17): não permitir trocar o modo de um card existente sem migração explícita

                throw com.phdev.quantofalta.core.validation.DataValidationException("Não é permitido trocar o modo de um evento já existente.")

            }

        }



        if (existing == null && !premium && eventDao.getActiveEventCountByTypeSync(validatedEvent.type.name) >= com.phdev.quantofalta.billing.PremiumPolicy.FREE_EVENT_LIMIT) {

            throw com.phdev.quantofalta.domain.EventLimitExceededException()

        }

        val op = if (existing == null) "c" else "u"

        val nextRevision = (existing?.localRevision ?: 0) + 1



        val entity = validatedEvent.toEntityModel().copy(

            syncState = SyncState.PENDING_UPDATE,

            localRevision = nextRevision,

            serverRevision = existing?.serverRevision ?: 0,

            updatedAt = System.currentTimeMillis(),

            deletedAt = existing?.deletedAt

        )



        // Cancela os alarmes antigos antes de alterar o banco. Falhas aqui nao podem bloquear o salvamento.
        runCatching {
            val oldReminders = eventReminderDao.getRemindersForEventSync(event.id)
            oldReminders.forEach { oldReminder ->
                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, oldReminder.id)
            }
            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, entity.id)
            com.phdev.quantofalta.core.notifications.SmartNotificationManager.cancelSmartNotifications(context, entity.id)
        }.onFailure { Log.w(TAG, "Failed to cancel previous notifications for ${event.id}", it) }


        // Se o evento está sendo destacado, desmarca os outros

        if (entity.isPinned) {

            val pinnedEntitiesOfType = eventDao.getAllEventsSync()

                .filter { it.isPinned && it.toDomainModel().type == validatedEvent.type && it.id != entity.id }



            pinnedEntitiesOfType.forEach { previous ->

                eventDao.updateEventPinnedStatus(previous.id, false)
                enqueueCardSyncSafely(previous.id, "UPDATE")
            }
        }


        eventDao.insertEvent(entity)



        // Limpa antigos e insere novos reminders no banco

        eventReminderDao.deleteRemindersForEvent(entity.id)



        if (!entity.isCompleted && !entity.isArchived) {

            if (validatedReminders.isNotEmpty()) {

                eventReminderDao.insertReminders(validatedReminders.map { it.toEntityModel() })
                validatedReminders.forEach { reminder ->
                    runCatching {
                        com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleReminder(context, entity, reminder)
                    }.onFailure { Log.w(TAG, "Failed to schedule reminder ${reminder.id}", it) }
                }
            }
            runCatching {
                com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, entity)
            }.onFailure { Log.w(TAG, "Failed to schedule completion for ${entity.id}", it) }
            runCatching {
                com.phdev.quantofalta.core.notifications.SmartNotificationManager.scheduleSmartNotifications(context, entity)
            }.onFailure { Log.w(TAG, "Failed to schedule smart notifications for ${entity.id}", it) }
        } else if (validatedReminders.isNotEmpty()) {
            eventReminderDao.insertReminders(validatedReminders.map { it.toEntityModel() })
        }


        // Timeline log

        if (existing == null) {

            runCatching { eventTimelineDao.insertTimelineEntry(
                EventTimelineEntity(
                    id = UUID.randomUUID().toString(),
                    eventId = event.id,
                    type = "CREATED",
                    description = "Evento criado",
                    timestampMillis = System.currentTimeMillis()
                )
            ) }.onFailure { Log.w(TAG, "Failed to write creation timeline for ${event.id}", it) }
            // Trigger smart feedback

            runCatching { smartFeedbackManager.recordEventCreated() }
                .onFailure { Log.w(TAG, "Failed to record smart feedback signal", it) }


            // Track via the official analytics pipeline (offline-first, batched, respects privacy settings)

            runCatching { analyticsManager.track(AnalyticsEvent.EventCreated) }
                .onFailure { Log.w(TAG, "Failed to track event creation", it) }
        } else {

            val now = System.currentTimeMillis()

            if (existing.title != validatedEvent.title) {

                runCatching { eventTimelineDao.insertTimelineEntry(
                    EventTimelineEntity(
                        id = UUID.randomUUID().toString(),
                        eventId = event.id,
                        type = "TITLE_CHANGED",
                        description = "Nome alterado",
                        timestampMillis = now
                    )
                ) }.onFailure { Log.w(TAG, "Failed to write title timeline for ${event.id}", it) }
            }

            if (existing.targetDate != validatedEvent.targetDate.toEpochDay() || existing.targetTime != validatedEvent.targetTime?.toSecondOfDay()) {

                runCatching { eventTimelineDao.insertTimelineEntry(
                    EventTimelineEntity(

                        id = UUID.randomUUID().toString(),

                        eventId = event.id,

                        type = "DATE_CHANGED",

                        description = "Data/Horário alterado",

                        timestampMillis = now

                    )

                ) }.onFailure { Log.w(TAG, "Failed to write date timeline for ${event.id}", it) }
            }

        }



        enqueueCardSyncSafely(entity.id, if (op == "c") "CREATE" else if (op == "d") "DELETE" else "UPDATE")
        updateWidgetsForEventSafely(validatedEvent.id)
    }



    suspend fun deleteEventById(id: String) {

        val reminders = eventReminderDao.getRemindersForEventSync(id)

        reminders.forEach { reminder ->

            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)

        }

        com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, id)
        com.phdev.quantofalta.core.notifications.SmartNotificationManager.cancelSmartNotifications(context, id)



        val existing = eventDao.getEventByIdSync(id)

        if (existing != null) {

            eventDao.deleteEventById(id)

            val updated = eventDao.getEventByIdSync(id) // fetch logical deleted entity

            if (updated != null) {

                syncOperationDao.insert(SyncOperationEntity(UUID.randomUUID().toString(), "card", id, "DELETE", null, SyncOperationStatus.PENDING))

                SyncWorker.enqueue(context)

            }

            eventReminderDao.deleteRemindersForEvent(id)

            eventTimelineDao.deleteTimelineForEvent(id)

            eventDao.redactDeletedEvent(id)

            com.phdev.quantofalta.core.utils.ImageStorageHelper.deleteInternalImage(context, existing.coverImageUri)

        }

        WidgetUpdateScheduler(context).updateWidgetsForEvent(id)

    }



    suspend fun markEventAsCompleted(id: String, isCompleted: Boolean) {

        eventDao.updateEventCompletedStatus(id, isCompleted)

        val event = eventDao.getEventByIdSync(id)

        if (event != null) {

            syncOperationDao.insert(SyncOperationEntity(UUID.randomUUID().toString(), "card", id, "UPDATE", null, SyncOperationStatus.PENDING))

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
            com.phdev.quantofalta.core.notifications.SmartNotificationManager.cancelSmartNotifications(context, id)

        } else {

            val event = eventDao.getEventByIdSync(id)

            if (event != null) {

                com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, event)
                com.phdev.quantofalta.core.notifications.SmartNotificationManager.scheduleSmartNotifications(context, event)

            }

        }

        WidgetUpdateScheduler(context).updateWidgetsForEvent(id)

    }



    suspend fun markEventAsArchived(id: String, isArchived: Boolean) {

        eventDao.updateEventArchivedStatus(id, isArchived)

        val event = eventDao.getEventByIdSync(id)

        if (event != null) {

            syncOperationDao.insert(SyncOperationEntity(UUID.randomUUID().toString(), "card", id, "UPDATE", null, SyncOperationStatus.PENDING))

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
            com.phdev.quantofalta.core.notifications.SmartNotificationManager.cancelSmartNotifications(context, id)

        } else {

            val event = eventDao.getEventByIdSync(id)

            if (event != null) {

                com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(context, event)
                com.phdev.quantofalta.core.notifications.SmartNotificationManager.scheduleSmartNotifications(context, event)

            }

        }

        WidgetUpdateScheduler(context).updateWidgetsForEvent(id)

    }



    suspend fun deleteAllEvents() {

        deleteAllEventsSafely()

    }



    suspend fun deleteAllEventsSafely() {

        val allEvents = eventDao.getAllEventsSync()

        allEvents.forEach { event ->

            com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, event.id)
            com.phdev.quantofalta.core.notifications.SmartNotificationManager.cancelSmartNotifications(context, event.id)

            val reminders = eventReminderDao.getRemindersForEventSync(event.id)

            reminders.forEach { reminder ->

                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, reminder.id)

            }

            eventReminderDao.deleteRemindersForEvent(event.id)

            eventTimelineDao.deleteTimelineForEvent(event.id)

            com.phdev.quantofalta.core.utils.ImageStorageHelper.deleteInternalImage(context, event.coverImageUri)

        }

        eventDao.deleteAllEvents()



        syncOperationDao.insert(SyncOperationEntity(

            operationId = java.util.UUID.randomUUID().toString(),

            entityType = "account",

            entityId = "all",

            operationType = "RESET",

            payload = null,

            status = com.phdev.quantofalta.core.database.SyncOperationStatus.PENDING

        ))



        SyncWorker.enqueue(context)

        WidgetUpdateScheduler(context).updateAllWidgets()

    }



    suspend fun updateRelationshipMonthly(id: String, enabled: Boolean) {

        eventDao.updateRelationshipMonthly(id, enabled)

        val event = eventDao.getEventByIdSync(id) ?: return

        syncOperationDao.insert(SyncOperationEntity(UUID.randomUUID().toString(), "card", id, "UPDATE", null, SyncOperationStatus.PENDING))

        SyncWorker.enqueue(context)

        WidgetUpdateScheduler(context).updateWidgetsForEvent(id)

    }



    suspend fun updateRelationshipAnnual(id: String, enabled: Boolean) {
        eventDao.updateRelationshipAnnual(id, enabled)
        val event = eventDao.getEventByIdSync(id) ?: return
        enqueueCardSyncSafely(id, "UPDATE")
        updateWidgetsForEventSafely(id)
    }

    private suspend fun enqueueCardSyncSafely(id: String, operationType: String) {
        runCatching {
            syncOperationDao.insert(
                SyncOperationEntity(UUID.randomUUID().toString(), "card", id, operationType, null, SyncOperationStatus.PENDING)
            )
            SyncWorker.enqueue(context)
        }.onFailure { Log.w(TAG, "Failed to enqueue sync for $id", it) }
    }

    private suspend fun updateWidgetsForEventSafely(id: String) {
        runCatching {
            WidgetUpdateScheduler(context).updateWidgetsForEvent(id)
        }.onFailure { Log.w(TAG, "Failed to update widgets for $id", it) }
    }

    suspend fun updateLastCelebrationEpochDay(id: String, epochDay: Long) {
        eventDao.updateLastCelebrationEpochDay(id, epochDay)
        enqueueCardSyncSafely(id, "UPDATE")
        updateWidgetsForEventSafely(id)
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



    val type = runCatching { com.phdev.quantofalta.domain.model.EventType.valueOf(this.type) }

        .getOrDefault(com.phdev.quantofalta.domain.model.EventType.STANDARD)



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

        coverImageUri = coverImageUri,

        standardModeStyle = com.phdev.quantofalta.domain.model.mode.StandardCardStyle.fromId(standardModeStyle),

        relationshipType = relationshipType,
        relationshipStartEpochDay = relationshipStartEpochDay,
        relationshipMonthlyEnabled = relationshipMonthlyEnabled,
        relationshipAnnualEnabled = relationshipAnnualEnabled,
        relationshipMilestonesEnabled = relationshipMilestonesEnabled,
        relationshipModeStyle = com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle.fromId(relationshipModeStyle),
        salaryFrequency = salaryFrequency,
        salaryPaymentDay = salaryPaymentDay,

        salaryPaymentDateEpochDay = salaryPaymentDateEpochDay,

        salaryCustomIntervalDays = salaryCustomIntervalDays,

        salaryWeekendRule = salaryWeekendRule,

        salaryShowBusinessDays = salaryShowBusinessDays,

        salaryValue = salaryValue,

        salaryModeStyle = com.phdev.quantofalta.domain.model.mode.SalaryCardStyle.fromId(salaryModeStyle),
        salaryGoalTarget = salaryGoalTarget,
        salaryCustomPhrase = salaryCustomPhrase,
        lastCelebrationEpochDay = lastCelebrationEpochDay,
        type = type
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

        updatedAt = updatedAtMillis,

        isCompleted = isCompleted,

        isArchived = isArchived,

        isPrivate = isPrivate,

        isPinned = isPinned,

        coverImageUri = coverImageUri,

        standardModeStyle = standardModeStyle.styleId,

        relationshipType = relationshipType,
        relationshipStartEpochDay = relationshipStartEpochDay,
        relationshipMonthlyEnabled = relationshipMonthlyEnabled,
        relationshipAnnualEnabled = relationshipAnnualEnabled,
        relationshipMilestonesEnabled = relationshipMilestonesEnabled,
        relationshipModeStyle = relationshipModeStyle.styleId,
        salaryFrequency = salaryFrequency,
        salaryPaymentDay = salaryPaymentDay,

        salaryPaymentDateEpochDay = salaryPaymentDateEpochDay,

        salaryCustomIntervalDays = salaryCustomIntervalDays,

        salaryWeekendRule = salaryWeekendRule,

        salaryShowBusinessDays = salaryShowBusinessDays,
        salaryValue = salaryValue,
        salaryModeStyle = salaryModeStyle.styleId,
        salaryGoalTarget = salaryGoalTarget,
        salaryCustomPhrase = salaryCustomPhrase,
        lastCelebrationEpochDay = lastCelebrationEpochDay,

        type = type.name

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


