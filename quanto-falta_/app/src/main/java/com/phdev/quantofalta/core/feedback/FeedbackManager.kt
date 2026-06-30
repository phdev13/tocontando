package com.phdev.quantofalta.core.feedback

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.core.analytics.InstallationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class FeedbackData(
    val rating: Int?,
    val category: String,
    val message: String,
    val includeTechData: Boolean,
    val sourceScreen: String? = null,
    val versionCode: Int? = null,
    val androidVersion: String? = null,
    val model: String? = null,
    val language: String? = null,
    val theme: String? = null,
)

/**
 * Manages feedback submission with offline support.
 * Never sends personal event data.
 * Tech data only included with explicit user consent.
 */
class FeedbackManager(private val context: Context) {

    companion object {
        private const val TAG = "FeedbackManager"
        private const val QUEUE_FILE = "feedback_queue.json"
        private val BASE_URL: String = BuildConfig.API_BASE_URL
        private val queueLock = Any()
        private const val MAX_QUEUE_SIZE = 100
        private const val MAX_MESSAGE_LENGTH = 2000
        private val VALID_CATEGORIES = setOf("suggestion", "bug", "compliment", "question", "other")
    }

    private val queueFile: File get() = File(context.filesDir, QUEUE_FILE)

    suspend fun submit(feedback: FeedbackData): Boolean = withContext(Dispatchers.IO) {
        val normalizedMessage = feedback.message.trim()
        if (
            normalizedMessage.isBlank() ||
            normalizedMessage.length > MAX_MESSAGE_LENGTH ||
            feedback.rating?.let { it !in 1..5 } == true ||
            feedback.category !in VALID_CATEGORIES
        ) return@withContext false
        val validatedFeedback = feedback.copy(message = normalizedMessage)
        val installationId = InstallationManager.getOrCreateId(context)

        val payload = buildPayload(validatedFeedback, installationId)

        // Try to send immediately
        return@withContext try {
            val success = post("$BASE_URL/api/v1/app/feedback", payload)
            if (!success) {
                savePending(payload)
                FeedbackRetryWorker.enqueue(context)
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Offline — saving feedback for later: ${e.message}")
            savePending(payload)
            FeedbackRetryWorker.enqueue(context)
            false // Returns false = "will send later"
        }
    }

    suspend fun sendPendingNow(): Boolean = withContext(Dispatchers.IO) {
        try {
            val arr = synchronized(queueLock) { readQueue() }
            if (arr.length() == 0) return@withContext true
            val items = JSONArray()
            for (i in 0 until arr.length()) {
                items.put(arr.getJSONObject(i))
            }
            val batch = JSONObject().apply { put("items", items) }
            val success = post("$BASE_URL/api/v1/app/feedback/offline", batch.toString())
            if (success) {
                removeSentItems(arr.length())
                synchronized(queueLock) { readQueue().length() == 0 }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send pending feedback: ${e.message}")
            false
        }
    }

    private fun buildPayload(feedback: FeedbackData, installationId: String): String {
        val clientId = UUID.randomUUID().toString()
        val obj = JSONObject().apply {
            put("installationId", installationId)
            put("clientId", clientId)
            feedback.rating?.let { put("rating", it) }
            put("category", feedback.category)
            put("message", feedback.message)
            put("includeTechData", feedback.includeTechData)
            if (feedback.includeTechData) {
                put("techData", JSONObject().apply {
                    feedback.versionCode?.let { put("versionCode", it) }
                    feedback.androidVersion?.let { put("androidVersion", it) }
                    feedback.model?.let { put("model", it) }
                    feedback.language?.let { put("language", it) }
                    feedback.theme?.let { put("theme", it) }
                    feedback.sourceScreen?.let { put("sourceScreen", it) }
                })
            }
        }
        return obj.toString()
    }

    private fun savePending(payload: String) {
        synchronized(queueLock) {
            try {
                val arr = readQueue()
                val bounded = JSONArray()
                val firstIndex = (arr.length() - (MAX_QUEUE_SIZE - 1)).coerceAtLeast(0)
                for (i in firstIndex until arr.length()) {
                    bounded.put(arr.get(i))
                }
                bounded.put(JSONObject(payload))
                queueFile.writeText(bounded.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save pending feedback")
            }
        }
    }

    private fun readQueue(): JSONArray {
        return if (queueFile.exists()) {
            try { JSONArray(queueFile.readText()) } catch (_: Exception) { JSONArray() }
        } else JSONArray()
    }

    private fun removeSentItems(count: Int) {
        synchronized(queueLock) {
            val current = readQueue()
            val remaining = JSONArray()
            for (i in count until current.length()) {
                remaining.put(current.get(i))
            }
            if (remaining.length() == 0) queueFile.delete()
            else queueFile.writeText(remaining.toString())
        }
    }

    private fun post(url: String, json: String): Boolean {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            conn.outputStream.use { it.write(json.toByteArray()) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            throw e
        } finally {
            conn.disconnect()
        }
    }
}
