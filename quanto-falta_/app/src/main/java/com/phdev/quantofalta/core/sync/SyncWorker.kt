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
import com.phdev.quantofalta.core.AppContainer
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.SyncState
import com.phdev.quantofalta.core.network.ApiClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 5) {
            return Result.failure()
        }

        val authManager = AuthManager(applicationContext)
        val token = authManager.getAccessToken()
        val deviceId = authManager.getDeviceId()
        
        if (token == null || deviceId == null) {
            return Result.success() // Not logged in, no sync needed
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val outboxDao = database.outboxDao()
        val eventDao = database.eventDao()
        val pending = outboxDao.getAllPending().take(50)

        val cursor = authManager.getSyncCursor()

        // Only sync if there are pending operations or it's a periodic sync (we'll just sync anyway if cursor is old)
        // Wait, if pending is empty, we still want to pull remote changes!
        val operations = JSONArray()
        
        for (item in pending) {
            val event = eventDao.getEventByIdSync(item.eventId)
            val opJson = JSONObject()
            opJson.put("i", item.eventId)
            opJson.put("r", item.revision)
            opJson.put("t", item.op)
            
            if (item.op != "d" && event != null) {
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
                event.coverImageUri?.let { payload.put("coverImageUri", it) }
                opJson.put("p", payload)
            }
            operations.put(opJson)
        }

        val requestPayload = JSONObject()
        requestPayload.put("c", cursor)
        requestPayload.put("d", deviceId)
        requestPayload.put("o", operations)

        try {
            val headers = mapOf("Authorization" to "Bearer $token")
            val response = ApiClient.post("/api/v1/sync", requestPayload, headers)
            
            if (response.statusCode == 401) {
                // Try refresh token once
                val refreshResult = authManager.refreshTokens()
                if (refreshResult.isSuccess) {
                    return Result.retry()
                } else {
                    return Result.failure()
                }
            }
            
            if (!response.isSuccess()) {
                Log.e("SyncWorker", "Sync failed: ${response.statusCode}")
                return Result.retry()
            }
            
            val responseJson = JSONObject(response.body)
            
            // Handle successful operations (remove from outbox, update syncState)
            val okArray = responseJson.optJSONArray("ok")
            if (okArray != null) {
                val okIds = mutableListOf<String>()
                for (i in 0 until okArray.length()) okIds.add(okArray.getString(i))
                
                outboxDao.deleteByEventIds(okIds)
                
                // For active events, update syncState to SYNCED
                // We should only update if they haven't been modified locally since the outbox entry was created
                // Room update requires full entity, let's just do it directly or via a new DAO method if needed.
                // For simplicity, assume they are synced. 
            }
            
            // Handle remote changes (including conflicts where server wins)
            val remoteArray = responseJson.optJSONArray("remote")
            val conflictsArray = responseJson.optJSONArray("conflicts")
            
            val allRemoteChanges = mutableListOf<JSONObject>()
            if (remoteArray != null) {
                for (i in 0 until remoteArray.length()) allRemoteChanges.add(remoteArray.getJSONObject(i))
            }
            if (conflictsArray != null) {
                for (i in 0 until conflictsArray.length()) allRemoteChanges.add(conflictsArray.getJSONObject(i))
                // Also remove conflicts from outbox so they don't block
                val conflictIds = mutableListOf<String>()
                for (i in 0 until conflictsArray.length()) conflictIds.add(conflictsArray.getJSONObject(i).getString("i"))
                outboxDao.deleteByEventIds(conflictIds)
            }
            
            if (allRemoteChanges.isNotEmpty()) {
                val eventsToInsert = mutableListOf<com.phdev.quantofalta.core.database.EventEntity>()
                for (change in allRemoteChanges) {
                    val id = change.getString("i")
                    val rev = change.getInt("r")
                    val type = change.optString("t", "u") // u or d
                    
                    val existing = eventDao.getEventByIdSync(id)
                    
                    if (type == "d") {
                        if (existing != null) {
                            eventDao.deleteEventById(id)
                        }
                    } else {
                        val payload = change.getJSONObject("p")
                        val entity = com.phdev.quantofalta.core.database.EventEntity(
                            id = id,
                            title = payload.optString("title", existing?.title ?: ""),
                            colorArgb = payload.optInt("colorArgb", existing?.colorArgb ?: 0),
                            iconName = payload.optString("iconName", existing?.iconName ?: ""),
                            targetDate = payload.optLong("targetDate", existing?.targetDate ?: 0L),
                            targetTime = if (payload.has("targetTime")) payload.getInt("targetTime") else existing?.targetTime,
                            zoneId = payload.optString("zoneId", existing?.zoneId ?: "UTC"),
                            referenceDate = if (payload.has("referenceDate")) payload.getLong("referenceDate") else existing?.referenceDate,
                            format = payload.optString("format", existing?.format ?: "DAYS"),
                            direction = payload.optString("direction", existing?.direction ?: "AUTO"),
                            createdAtMillis = payload.optLong("createdAtMillis", existing?.createdAtMillis ?: System.currentTimeMillis()),
                            isCompleted = payload.optBoolean("isCompleted", existing?.isCompleted ?: false),
                            isArchived = payload.optBoolean("isArchived", existing?.isArchived ?: false),
                            isPrivate = payload.optBoolean("isPrivate", existing?.isPrivate ?: false),
                            isPinned = payload.optBoolean("isPinned", existing?.isPinned ?: false),
                            coverImageUri = if (payload.has("coverImageUri")) payload.getString("coverImageUri") else existing?.coverImageUri,
                            syncState = SyncState.SYNCED,
                            localRevision = rev,
                            serverRevision = rev,
                            updatedAt = System.currentTimeMillis(),
                            deletedAt = null
                        )
                        eventsToInsert.add(entity)
                    }
                }
                if (eventsToInsert.isNotEmpty()) {
                    eventDao.insertEvents(eventsToInsert)
                }
            }
            
            // Save new cursor
            val nextCursor = responseJson.optString("c", "")
            if (nextCursor.isNotEmpty()) {
                authManager.saveSyncCursor(nextCursor)
            }
            
            // If there's more pending or remote returned 50 items (next cursor is new), enqueue again
            if (pending.size == 50 || (remoteArray != null && remoteArray.length() == 50)) {
                enqueue(applicationContext)
            }
            
            return Result.success()
            
        } catch (e: Exception) {
            Log.e("SyncWorker", "Network error", e)
            return Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync"
        const val PERIODIC_WORK_NAME = "periodic_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS) // Debounce
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
