package com.phdev.quantofalta.core.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.SyncOperationEntity
import com.phdev.quantofalta.core.database.SyncOperationStatus
import com.phdev.quantofalta.core.database.SyncState
import com.phdev.quantofalta.core.network.ApiClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 5) {
            return Result.failure(workDataOf("error" to "Muitas tentativas falhas. Verifique sua conexão e tente novamente."))
        }

        setProgress(workDataOf("state" to "syncing"))

        val authManager = AuthManager(applicationContext)
        val token = authManager.getAccessToken()
        val deviceId = authManager.getDeviceId()

        if (token == null || deviceId == null) {
            return Result.success()
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val syncOperationDao = database.syncOperationDao()
        val eventDao = database.eventDao()

        eventDao.getAllEventsSync().forEach { event ->
            if (syncOperationDao.countPendingForEntity("card", event.id) == 0) {
                syncOperationDao.insert(
                    SyncOperationEntity(
                        operationId = UUID.randomUUID().toString(),
                        entityType = "card",
                        entityId = event.id,
                        operationType = "UPDATE",
                        payload = null,
                        status = SyncOperationStatus.PENDING
                    )
                )
            }
        }

        // 1. PUSH PENDING OPERATIONS
        val pendingOperations = syncOperationDao.getPendingOperations().take(50)

        if (pendingOperations.isNotEmpty()) {
            val opsArray = JSONArray()
            for (op in pendingOperations) {
                val opJson = JSONObject()
                opJson.put("operationId", op.operationId)
                opJson.put("entityType", op.entityType)
                opJson.put("entityId", op.entityId)
                opJson.put("operationType", op.operationType)

                if ((op.operationType == "CREATE" || op.operationType == "UPDATE") && op.entityType == "card") {
                    val event = eventDao.getEventByIdSync(op.entityId)
                    if (event != null) {
                        val payload = JSONObject()
                        payload.put("title", event.title)
                        payload.put("colorArgb", event.colorArgb)
                        payload.put("iconName", event.iconName)
                        payload.put("targetDate", event.targetDate)
                        event.targetTime?.let { payload.put("targetTime", it) }
                        payload.put("zoneId", event.zoneId)
                        event.referenceDate?.let { payload.put("referenceDate", it) }
                        payload.put("format", event.format)
                        payload.put("direction", event.direction)
                        payload.put("createdAtMillis", event.createdAtMillis)
                        payload.put("isCompleted", event.isCompleted)
                        payload.put("isArchived", event.isArchived)
                        payload.put("isPrivate", event.isPrivate)
                        payload.put("isPinned", event.isPinned)
                        payload.put("standardModeStyle", event.standardModeStyle)
                        event.relationshipType?.let { payload.put("relationshipType", it) }
                        event.relationshipStartEpochDay?.let { payload.put("relationshipStartEpochDay", it) }
                        payload.put("relationshipMonthlyEnabled", event.relationshipMonthlyEnabled)
                        payload.put("relationshipAnnualEnabled", event.relationshipAnnualEnabled)
                        payload.put("relationshipMilestonesEnabled", event.relationshipMilestonesEnabled)
                        event.salaryFrequency?.let { payload.put("salaryFrequency", it) }
                        event.salaryPaymentDay?.let { payload.put("salaryPaymentDay", it) }
                        event.salaryPaymentDateEpochDay?.let { payload.put("salaryPaymentDateEpochDay", it) }
                        event.salaryCustomIntervalDays?.let { payload.put("salaryCustomIntervalDays", it) }
                        event.salaryWeekendRule?.let { payload.put("salaryWeekendRule", it) }
                        payload.put("salaryShowBusinessDays", event.salaryShowBusinessDays)
                        event.salaryValue?.let { payload.put("salaryValue", it) }
                        payload.put("salaryModeStyle", event.salaryModeStyle)
                        event.salaryGoalTarget?.let { payload.put("salaryGoalTarget", it) }
                        event.salaryCustomPhrase?.let { payload.put("salaryCustomPhrase", it) }
                        payload.put("type", event.type)

                        val reminders = database.eventReminderDao().getRemindersForEventSync(event.id)
                        payload.put("reminders", JSONArray(reminders.map { reminder ->
                            JSONObject().apply {
                                put("id", reminder.id)
                                put("triggerType", reminder.triggerType)
                                reminder.offsetMinutes?.let { put("offsetMinutes", it) }
                                reminder.customDateTimeMillis?.let { put("customDateTimeMillis", it) }
                                put("enabled", reminder.enabled)
                                put("allowSnooze", reminder.allowSnooze)
                                put("soundEnabled", reminder.soundEnabled)
                                put("vibrationEnabled", reminder.vibrationEnabled)
                                put("createdAtMillis", reminder.createdAtMillis)
                                put("updatedAtMillis", reminder.updatedAtMillis)
                            }
                        }))
                        opJson.put("payload", payload.toString())
                    }
                }
                opsArray.put(opJson)
            }

            val pushPayload = JSONObject().apply { put("operations", opsArray) }
            try {
                val headers = mapOf("Authorization" to "Bearer $token")
                val response = ApiClient.post("/api/v1/sync/push", pushPayload, headers)
                if (response.statusCode == 401) {
                    val refreshResult = authManager.refreshTokens()
                    return if (refreshResult.isSuccess) Result.retry() else Result.failure(workDataOf("error" to "Sessão expirada."))
                }
                if (response.isSuccess()) {
                    val responseJson = ApiClient.unwrapDataObject(response.body)
                    val processedArray = responseJson.optJSONArray("processed")
                    if (processedArray != null) {
                        val processedIds = mutableListOf<String>()
                        for (i in 0 until processedArray.length()) processedIds.add(processedArray.getString(i))
                        syncOperationDao.deleteOperations(processedIds)

                        // Set items back to SYNCED
                        for (op in pendingOperations) {
                            if (op.operationId in processedIds && op.entityType == "card") {
                                val e = eventDao.getEventByIdSync(op.entityId)
                                if (e != null) {
                                    eventDao.acknowledgeSync(e.id, e.serverRevision + 1, System.currentTimeMillis())
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncWorker", "Push failed", e)
            }
        }

        // 2. PULL REMOTE CHANGES
        val cursor = authManager.getSyncCursor()
        try {
            val headers = mapOf("Authorization" to "Bearer $token")
            val pullUrl = "/api/v1/sync/pull${if (cursor.isNotEmpty()) "?cursor=$cursor" else ""}"
            val response = ApiClient.get(pullUrl, headers)

            if (response.isSuccess()) {
                val responseJson = ApiClient.unwrapDataObject(response.body)
                val entitiesArray = responseJson.optJSONArray("entities")
                val nextCursor = responseJson.optString("nextCursor", "")
                val syncGeneration = responseJson.optInt("syncGeneration", 1)

                // Compare Generation
                val localGeneration = authManager.getSyncGeneration()
                if (syncGeneration > localGeneration) {
                    // It's a fresh restart / wipe data
                    database.eventDao().physicalDeleteAllEvents()
                    database.eventReminderDao().deleteAllReminders()
                    database.eventTimelineDao().deleteAllTimeline()
                    database.syncOperationDao().deleteAllOperations()
                    authManager.saveSyncGeneration(syncGeneration)
                }

                if (entitiesArray != null) {
                    val eventsToInsert = mutableListOf<com.phdev.quantofalta.core.database.EventEntity>()
                    val remindersByEvent = mutableMapOf<String, List<com.phdev.quantofalta.core.database.EventReminderEntity>>()

                    for (i in 0 until entitiesArray.length()) {
                        val change = entitiesArray.getJSONObject(i)
                        val id = change.getString("id")
                        val payloadValue = change.opt("payload")
                        val payload = when (payloadValue) {
                            is JSONObject -> payloadValue
                            is String -> if (payloadValue.isNotEmpty() && payloadValue != "null") JSONObject(payloadValue) else null
                            else -> null
                        }
                        val isDeleted = change.optBoolean("isDeleted", false)
                        val remoteVersion = change.optInt("syncVersion", 0)

                        val existing = eventDao.getEventByIdSync(id)
                        if (existing != null && remoteVersion <= existing.syncVersion) {
                            continue
                        }

                        if (isDeleted) {
                            if (existing != null) {
                                val reminders = database.eventReminderDao().getRemindersForEventSync(id)
                                reminders.forEach {
                                    com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(applicationContext, it.id)
                                }
                                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(applicationContext, id)
                                database.eventReminderDao().deleteRemindersForEvent(id)
                                database.eventTimelineDao().deleteTimelineForEvent(id)
                                com.phdev.quantofalta.core.utils.ImageStorageHelper.deleteInternalImage(applicationContext, existing.coverImageUri)
                                eventDao.applyRemoteDelete(id, remoteVersion)
                                eventDao.redactDeletedEvent(id)
                                com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler(applicationContext).updateWidgetsForEvent(id)
                            }
                        } else if (payload != null) {
                            if (payload.has("reminders")) {
                                val reminderArray = payload.optJSONArray("reminders") ?: JSONArray()
                                val remoteReminders = mutableListOf<com.phdev.quantofalta.core.database.EventReminderEntity>()
                                for (j in 0 until reminderArray.length()) {
                                    val r = reminderArray.getJSONObject(j)
                                    remoteReminders.add(com.phdev.quantofalta.core.database.EventReminderEntity(
                                        id = r.getString("id"),
                                        eventId = id,
                                        triggerType = r.optString("triggerType", "EXACT"),
                                        offsetMinutes = if (r.has("offsetMinutes") && !r.isNull("offsetMinutes")) r.getInt("offsetMinutes") else null,
                                        customDateTimeMillis = if (r.has("customDateTimeMillis") && !r.isNull("customDateTimeMillis")) r.getLong("customDateTimeMillis") else null,
                                        enabled = r.optBoolean("enabled", true),
                                        allowSnooze = r.optBoolean("allowSnooze", true),
                                        soundEnabled = r.optBoolean("soundEnabled", true),
                                        vibrationEnabled = r.optBoolean("vibrationEnabled", true),
                                        createdAtMillis = r.optLong("createdAtMillis", System.currentTimeMillis()),
                                        updatedAtMillis = r.optLong("updatedAtMillis", System.currentTimeMillis())
                                    ))
                                }
                                remindersByEvent[id] = remoteReminders
                            }

                            val rawEntity = com.phdev.quantofalta.core.database.EventEntity(
                                id = id,
                                remoteId = id,
                                title = payload.optString("title", existing?.title ?: ""),
                                colorArgb = payload.optInt("colorArgb", existing?.colorArgb ?: 0),
                                iconName = payload.optString("iconName", existing?.iconName ?: ""),
                                targetDate = payload.optLong("targetDate", existing?.targetDate ?: 0L),
                                targetTime = if (payload.has("targetTime") && !payload.isNull("targetTime")) payload.getInt("targetTime") else existing?.targetTime,
                                zoneId = payload.optString("zoneId", existing?.zoneId ?: "UTC"),
                                referenceDate = if (payload.has("referenceDate") && !payload.isNull("referenceDate")) payload.getLong("referenceDate") else existing?.referenceDate,
                                format = payload.optString("format", existing?.format ?: "DAYS"),
                                direction = payload.optString("direction", existing?.direction ?: "AUTO"),
                                createdAtMillis = payload.optLong("createdAtMillis", existing?.createdAtMillis ?: System.currentTimeMillis()),
                                isCompleted = payload.optBoolean("isCompleted", existing?.isCompleted ?: false),
                                isArchived = payload.optBoolean("isArchived", existing?.isArchived ?: false),
                                isPrivate = payload.optBoolean("isPrivate", existing?.isPrivate ?: false),
                                isPinned = payload.optBoolean("isPinned", existing?.isPinned ?: false),
                                coverImageUri = existing?.coverImageUri,
                                standardModeStyle = payload.optString("standardModeStyle", existing?.standardModeStyle ?: "classic"),
                                relationshipType = if (payload.has("relationshipType") && !payload.isNull("relationshipType")) payload.getString("relationshipType") else existing?.relationshipType,
                                relationshipStartEpochDay = if (payload.has("relationshipStartEpochDay") && !payload.isNull("relationshipStartEpochDay")) payload.getLong("relationshipStartEpochDay") else existing?.relationshipStartEpochDay,
                                relationshipMonthlyEnabled = payload.optBoolean("relationshipMonthlyEnabled", existing?.relationshipMonthlyEnabled ?: false),
                                relationshipAnnualEnabled = payload.optBoolean("relationshipAnnualEnabled", existing?.relationshipAnnualEnabled ?: true),
                                relationshipMilestonesEnabled = payload.optBoolean("relationshipMilestonesEnabled", existing?.relationshipMilestonesEnabled ?: true),
                                salaryFrequency = if (payload.has("salaryFrequency") && !payload.isNull("salaryFrequency")) payload.getString("salaryFrequency") else existing?.salaryFrequency,
                                salaryPaymentDay = if (payload.has("salaryPaymentDay") && !payload.isNull("salaryPaymentDay")) payload.getInt("salaryPaymentDay") else existing?.salaryPaymentDay,
                                salaryPaymentDateEpochDay = if (payload.has("salaryPaymentDateEpochDay") && !payload.isNull("salaryPaymentDateEpochDay")) payload.getLong("salaryPaymentDateEpochDay") else existing?.salaryPaymentDateEpochDay,
                                salaryCustomIntervalDays = if (payload.has("salaryCustomIntervalDays") && !payload.isNull("salaryCustomIntervalDays")) payload.getInt("salaryCustomIntervalDays") else existing?.salaryCustomIntervalDays,
                                salaryWeekendRule = if (payload.has("salaryWeekendRule") && !payload.isNull("salaryWeekendRule")) payload.getString("salaryWeekendRule") else existing?.salaryWeekendRule,
                                salaryShowBusinessDays = payload.optBoolean("salaryShowBusinessDays", existing?.salaryShowBusinessDays ?: false),
                                salaryValue = if (payload.has("salaryValue") && !payload.isNull("salaryValue")) payload.getDouble("salaryValue") else existing?.salaryValue,
                                salaryModeStyle = payload.optString("salaryModeStyle", existing?.salaryModeStyle ?: "next_salary"),
                                salaryGoalTarget = if (payload.has("salaryGoalTarget") && !payload.isNull("salaryGoalTarget")) payload.getDouble("salaryGoalTarget") else existing?.salaryGoalTarget,
                                salaryCustomPhrase = if (payload.has("salaryCustomPhrase") && !payload.isNull("salaryCustomPhrase")) payload.getString("salaryCustomPhrase") else existing?.salaryCustomPhrase,
                                type = payload.optString("type", existing?.type ?: "STANDARD"),
                                syncState = SyncState.SYNCED,
                                syncVersion = remoteVersion,
                                localRevision = remoteVersion,
                                serverRevision = remoteVersion,
                                updatedAt = System.currentTimeMillis(),
                                deletedAt = null
                            )
                            runCatching {
                                com.phdev.quantofalta.core.validation.AppDataValidator.validateEventEntity(rawEntity)
                            }.onSuccess { validated ->
                                eventsToInsert.add(validated)
                            }.onFailure { error ->
                                Log.w("SyncWorker", "Ignoring invalid remote card $id: ${error.message}")
                            }
                        }
                    }

                    if (eventsToInsert.isNotEmpty()) {
                        eventDao.insertEvents(eventsToInsert)
                        eventsToInsert.forEach { event ->
                            remindersByEvent[event.id]?.let { reminders ->
                                database.eventReminderDao().getRemindersForEventSync(event.id).forEach {
                                    com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(applicationContext, it.id)
                                }
                                database.eventReminderDao().deleteRemindersForEvent(event.id)
                                if (reminders.isNotEmpty()) {
                                    database.eventReminderDao().insertReminders(reminders)
                                    reminders.forEach { reminder ->
                                        com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleReminder(
                                            applicationContext, event,
                                            com.phdev.quantofalta.core.notifications.model.EventReminder(
                                                id = reminder.id, eventId = reminder.eventId,
                                                triggerType = runCatching { com.phdev.quantofalta.core.notifications.model.TriggerType.valueOf(reminder.triggerType) }.getOrDefault(com.phdev.quantofalta.core.notifications.model.TriggerType.EXACT),
                                                offsetMinutes = reminder.offsetMinutes, customDateTimeMillis = reminder.customDateTimeMillis,
                                                enabled = reminder.enabled, allowSnooze = reminder.allowSnooze, soundEnabled = reminder.soundEnabled,
                                                vibrationEnabled = reminder.vibrationEnabled, createdAtMillis = reminder.createdAtMillis, updatedAtMillis = reminder.updatedAtMillis
                                            )
                                        )
                                    }
                                }
                            }
                            com.phdev.quantofalta.core.notifications.NotificationScheduler.scheduleEventCompletion(applicationContext, event)
                            com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler(applicationContext).updateWidgetsForEvent(event.id)
                        }
                    }
                }

                if (nextCursor.isNotEmpty()) {
                    authManager.saveSyncCursor(nextCursor)
                }

                val hasMore = responseJson.optBoolean("hasMore", false)
                if (hasMore) {
                    enqueue(applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Pull failed", e)
        }

        setProgress(workDataOf("state" to "success", "last_sync" to System.currentTimeMillis()))
        return Result.success(workDataOf("last_sync" to System.currentTimeMillis()))
    }

    companion object {
        const val WORK_NAME = "sync"
        const val PERIODIC_WORK_NAME = "periodic_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
