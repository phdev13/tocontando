package com.phdev.quantofalta.core.database

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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

            context.contentResolver.openOutputStream(uri)?.use { out ->
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
                InputStreamReader(input).readText()
            } ?: return@withContext false

            val rootJson = JSONObject(jsonString)
            if (!rootJson.has("version")) return@withContext false

            val eventsArray = rootJson.optJSONArray("events")
            if (eventsArray != null) {
                val newEvents = mutableListOf<EventEntity>()
                for (i in 0 until eventsArray.length()) {
                    val obj = eventsArray.getJSONObject(i)
                    val targetDate = if (obj.has("targetDate")) obj.getLong("targetDate") else obj.getLong("dateMillis") / 86400000L
                    val format = if (obj.has("format")) obj.getString("format") else "DAYS"
                    val direction = if (obj.has("direction")) obj.getString("direction") else "AUTO"
                    val zoneId = if (obj.has("zoneId")) obj.getString("zoneId") else "UTC"
                    
                    newEvents.add(
                        EventEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            targetDate = targetDate,
                            targetTime = if (obj.has("targetTime") && !obj.isNull("targetTime")) obj.getInt("targetTime") else null,
                            zoneId = zoneId,
                            referenceDate = if (obj.has("referenceDate") && !obj.isNull("referenceDate")) obj.getLong("referenceDate") else null,
                            format = format,
                            direction = direction,
                            createdAtMillis = obj.getLong("createdAtMillis"),
                            colorArgb = obj.getInt("colorArgb"),
                            iconName = obj.getString("iconName"),
                            isCompleted = obj.getBoolean("isCompleted"),
                            isArchived = obj.getBoolean("isArchived"),
                            isPrivate = obj.optBoolean("isPrivate", false),
                            isPinned = obj.optBoolean("isPinned", false),
                            coverImageUri = if (obj.has("coverImageUri") && !obj.isNull("coverImageUri")) obj.getString("coverImageUri") else null
                        )
                    )
                }
                database.eventDao().insertEvents(newEvents)
            }

            val timelinesArray = rootJson.optJSONArray("timelines")
            if (timelinesArray != null) {
                val newTimelines = mutableListOf<EventTimelineEntity>()
                for (i in 0 until timelinesArray.length()) {
                    val obj = timelinesArray.getJSONObject(i)
                    newTimelines.add(
                        EventTimelineEntity(
                            id = obj.getString("id"),
                            eventId = obj.getString("eventId"),
                            type = obj.getString("type"),
                            description = obj.getString("description"),
                            timestampMillis = obj.getLong("timestampMillis")
                        )
                    )
                }
                database.eventTimelineDao().insertTimelines(newTimelines)
            }

            val remindersArray = rootJson.optJSONArray("reminders")
            if (remindersArray != null) {
                val newReminders = mutableListOf<EventReminderEntity>()
                for (i in 0 until remindersArray.length()) {
                    val obj = remindersArray.getJSONObject(i)
                    newReminders.add(
                        EventReminderEntity(
                            id = obj.getString("id"),
                            eventId = obj.getString("eventId"),
                            triggerType = obj.getString("triggerType"),
                            offsetMinutes = if (obj.isNull("offsetMinutes")) null else obj.getInt("offsetMinutes"),
                            customDateTimeMillis = if (obj.isNull("customDateTimeMillis")) null else obj.getLong("customDateTimeMillis"),
                            enabled = obj.getBoolean("enabled"),
                            allowSnooze = obj.getBoolean("allowSnooze"),
                            soundEnabled = obj.getBoolean("soundEnabled"),
                            vibrationEnabled = obj.getBoolean("vibrationEnabled"),
                            createdAtMillis = obj.getLong("createdAtMillis"),
                            updatedAtMillis = obj.getLong("updatedAtMillis")
                        )
                    )
                }
                database.eventReminderDao().insertReminders(newReminders)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
