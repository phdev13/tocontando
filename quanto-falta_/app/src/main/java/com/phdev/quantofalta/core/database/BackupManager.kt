package com.phdev.quantofalta.core.database

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.phdev.quantofalta.core.validation.AppDataValidator
import com.phdev.quantofalta.core.validation.DataValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class BackupManager(private val context: Context, private val database: AppDatabase) {

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val events = database.eventDao().getAllEventsSync()
            val timelines = database.eventTimelineDao().getAllTimelinesSync()
            val reminders = database.eventReminderDao().getAllRemindersSync()

            val rootJson = JSONObject()
            rootJson.put("version", 1)

            val eventsArray = JSONArray()
            events.forEach { e ->
                val json = JSONObject()
                json.put("id", e.id)
                json.put("title", e.title)
                json.put("targetDate", e.targetDate)
                if (e.targetTime != null) json.put("targetTime", e.targetTime)
                json.put("zoneId", e.zoneId)
                if (e.referenceDate != null) json.put("referenceDate", e.referenceDate)
                json.put("format", e.format)
                json.put("direction", e.direction)
                json.put("createdAtMillis", e.createdAtMillis)
                json.put("colorArgb", e.colorArgb)
                json.put("iconName", e.iconName)
                json.put("isCompleted", e.isCompleted)
                json.put("isArchived", e.isArchived)
                json.put("isPrivate", e.isPrivate)
                json.put("isPinned", e.isPinned)
                if (e.coverImageUri != null) json.put("coverImageUri", e.coverImageUri)
                eventsArray.put(json)
            }
            rootJson.put("events", eventsArray)

            val timelinesArray = JSONArray()
            timelines.forEach { t ->
                val json = JSONObject()
                json.put("id", t.id)
                json.put("eventId", t.eventId)
                json.put("type", t.type)
                json.put("description", t.description)
                json.put("timestampMillis", t.timestampMillis)
                timelinesArray.put(json)
            }
            rootJson.put("timelines", timelinesArray)

            val remindersArray = JSONArray()
            reminders.forEach { r ->
                val json = JSONObject()
                json.put("id", r.id)
                json.put("eventId", r.eventId)
                json.put("triggerType", r.triggerType)
                json.put("offsetMinutes", r.offsetMinutes)
                json.put("customDateTimeMillis", r.customDateTimeMillis)
                json.put("enabled", r.enabled)
                json.put("allowSnooze", r.allowSnooze)
                json.put("soundEnabled", r.soundEnabled)
                json.put("vibrationEnabled", r.vibrationEnabled)
                json.put("createdAtMillis", r.createdAtMillis)
                json.put("updatedAtMillis", r.updatedAtMillis)
                remindersArray.put(json)
            }
            rootJson.put("reminders", remindersArray)

            val output = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            output.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(rootJson.toString(2))
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                val content = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var totalBytes = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    if (totalBytes > AppDataValidator.MAX_BACKUP_BYTES) {
                        throw DataValidationException("O arquivo de backup é grande demais.")
                    }
                    content.write(buffer, 0, count)
                }
                content.toString(StandardCharsets.UTF_8.name())
            } ?: return@withContext false

            val rootJson = JSONObject(jsonString)
            if (rootJson.optInt("version", -1) != 1) return@withContext false

            val eventsArray = rootJson.optJSONArray("events") ?: JSONArray()
            val timelinesArray = rootJson.optJSONArray("timelines") ?: JSONArray()
            val remindersArray = rootJson.optJSONArray("reminders") ?: JSONArray()
            if (
                eventsArray.length() > AppDataValidator.MAX_BACKUP_EVENTS ||
                timelinesArray.length() > AppDataValidator.MAX_BACKUP_TIMELINES ||
                remindersArray.length() > AppDataValidator.MAX_BACKUP_REMINDERS
            ) throw DataValidationException("O backup ultrapassa os limites aceitos.")

            val eventIds = mutableSetOf<String>()
            val newEvents = mutableListOf<EventEntity>()
            for (i in 0 until eventsArray.length()) {
                val obj = eventsArray.getJSONObject(i)
                val targetDate = if (obj.has("targetDate")) obj.getLong("targetDate")
                    else obj.getLong("dateMillis") / 86_400_000L
                val entity = AppDataValidator.validateEventEntity(
                    EventEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        targetDate = targetDate,
                        targetTime = if (obj.has("targetTime") && !obj.isNull("targetTime")) obj.getInt("targetTime") else null,
                        zoneId = obj.optString("zoneId", "UTC"),
                        referenceDate = if (obj.has("referenceDate") && !obj.isNull("referenceDate")) obj.getLong("referenceDate") else null,
                        format = obj.optString("format", "DAYS"),
                        direction = obj.optString("direction", "AUTO"),
                        createdAtMillis = obj.getLong("createdAtMillis"),
                        colorArgb = obj.getInt("colorArgb"),
                        iconName = obj.getString("iconName"),
                        isCompleted = obj.getBoolean("isCompleted"),
                        isArchived = obj.getBoolean("isArchived"),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        isPinned = obj.optBoolean("isPinned", false),
                        coverImageUri = obj.optString("coverImageUri").takeIf {
                            it.isNotBlank() && com.phdev.quantofalta.core.utils.ImageStorageHelper.isAvailable(it)
                        }
                    )
                )
                if (!eventIds.add(entity.id)) throw DataValidationException("O backup contém eventos duplicados.")
                newEvents.add(entity)
            }

            val timelineIds = mutableSetOf<String>()
            val newTimelines = mutableListOf<EventTimelineEntity>()
            for (i in 0 until timelinesArray.length()) {
                val obj = timelinesArray.getJSONObject(i)
                val eventId = AppDataValidator.requireId(obj.getString("eventId"))
                val id = AppDataValidator.requireId(obj.getString("id"), "ID do histórico")
                val type = obj.getString("type").trim()
                val description = obj.getString("description").trim()
                val timestamp = obj.getLong("timestampMillis")
                if (
                    eventId !in eventIds || !timelineIds.add(id) || type.isBlank() ||
                    type.length > 50 || description.length > 500 || timestamp <= 0L
                ) throw DataValidationException("Histórico inválido no backup.")
                newTimelines.add(EventTimelineEntity(id, eventId, type, description, timestamp))
            }

            val reminderIds = mutableSetOf<String>()
            val newReminders = mutableListOf<EventReminderEntity>()
            for (i in 0 until remindersArray.length()) {
                val obj = remindersArray.getJSONObject(i)
                val eventId = AppDataValidator.requireId(obj.getString("eventId"))
                if (eventId !in eventIds) throw DataValidationException("Lembrete sem evento correspondente.")
                val entity = AppDataValidator.validateReminderEntity(
                    eventId,
                    EventReminderEntity(
                        id = obj.getString("id"),
                        eventId = eventId,
                        triggerType = obj.getString("triggerType"),
                        offsetMinutes = if (!obj.has("offsetMinutes") || obj.isNull("offsetMinutes")) null else obj.getInt("offsetMinutes"),
                        customDateTimeMillis = if (!obj.has("customDateTimeMillis") || obj.isNull("customDateTimeMillis")) null else obj.getLong("customDateTimeMillis"),
                        enabled = obj.getBoolean("enabled"),
                        allowSnooze = obj.getBoolean("allowSnooze"),
                        soundEnabled = obj.getBoolean("soundEnabled"),
                        vibrationEnabled = obj.getBoolean("vibrationEnabled"),
                        createdAtMillis = obj.getLong("createdAtMillis"),
                        updatedAtMillis = obj.getLong("updatedAtMillis")
                    )
                )
                if (!reminderIds.add(entity.id)) throw DataValidationException("Lembretes duplicados no backup.")
                newReminders.add(entity)
            }
            newReminders.groupBy { it.eventId }.forEach { (_, reminders) ->
                if (reminders.size > AppDataValidator.MAX_REMINDERS_PER_EVENT) {
                    throw DataValidationException("Há lembretes demais para um evento.")
                }
                val keys = reminders.map { Triple(it.triggerType, it.offsetMinutes, it.customDateTimeMillis) }
                if (keys.distinct().size != keys.size) throw DataValidationException("Há lembretes duplicados.")
            }

            newEvents.forEach { event ->
                database.eventReminderDao().getRemindersForEventSync(event.id).forEach {
                    com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelReminder(context, it.id)
                }
                com.phdev.quantofalta.core.notifications.NotificationScheduler.cancelEventCompletion(context, event.id)
            }
            database.withTransaction {
                newEvents.forEach {
                    database.eventReminderDao().deleteRemindersForEvent(it.id)
                    database.eventTimelineDao().deleteTimelineForEvent(it.id)
                }
                database.eventDao().insertEvents(newEvents)
                if (newTimelines.isNotEmpty()) database.eventTimelineDao().insertTimelines(newTimelines)
                if (newReminders.isNotEmpty()) database.eventReminderDao().insertReminders(newReminders)
                database.syncOperationDao().insertAll(
                    newEvents.map {
                        com.phdev.quantofalta.core.database.SyncOperationEntity(
                            java.util.UUID.randomUUID().toString(),
                            "card",
                            it.id,
                            "UPDATE",
                            null,
                            com.phdev.quantofalta.core.database.SyncOperationStatus.PENDING
                        )
                    }
                )
            }

            val request = androidx.work.OneTimeWorkRequestBuilder<
                com.phdev.quantofalta.core.notifications.worker.NotificationReschedulerWorker
            >().build()
            androidx.work.WorkManager.getInstance(context).enqueue(request)
            com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler(context).updateAllWidgets()
            com.phdev.quantofalta.core.sync.SyncWorker.enqueue(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
