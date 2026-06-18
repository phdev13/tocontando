package com.phdev.quantofalta.core.analytics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Simple file-based queue for analytics events.
 * Limits to MAX_SIZE events, discarding oldest when full.
 * Thread-safe via synchronized blocks.
 */
class AnalyticsQueue(private val context: Context) {

    companion object {
        private const val TAG = "AnalyticsQueue"
        private const val MAX_SIZE = 500
        private const val QUEUE_FILE = "analytics_queue.json"
        private const val PERF_FILE = "perf_queue.json"
    }

    private val lock = Any()

    private val queueFile: File get() = File(context.filesDir, QUEUE_FILE)
    private val perfFile: File get() = File(context.filesDir, PERF_FILE)

    fun enqueue(event: AnalyticsEvent) {
        synchronized(lock) {
            try {
                val arr = readArray(queueFile)
                if (arr.length() >= MAX_SIZE) {
                    // Drop oldest (index 0) to make room
                    val trimmed = JSONArray()
                    for (i in 1 until arr.length()) trimmed.put(arr.get(i))
                    trimmed.put(eventToJson(event))
                    writeArray(queueFile, trimmed)
                } else {
                    arr.put(eventToJson(event))
                    writeArray(queueFile, arr)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue: ${e.message}")
            }
        }
    }

    fun enqueuePerformance(metrics: List<PerformanceMetric>) {
        synchronized(lock) {
            try {
                val arr = readArray(perfFile)
                metrics.forEach { m ->
                    arr.put(JSONObject().apply {
                        put("type", m.type)
                        put("valueMs", m.valueMs)
                        m.screen?.let { put("screen", it) }
                        put("ts", System.currentTimeMillis())
                    })
                }
                writeArray(perfFile, arr)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue perf: ${e.message}")
            }
        }
    }

    /** Returns up to [batchSize] events without removing them */
    fun peek(batchSize: Int = 50): List<JSONObject> {
        return synchronized(lock) {
            try {
                val arr = readArray(queueFile)
                buildList {
                    for (i in 0 until minOf(batchSize, arr.length())) {
                        add(arr.getJSONObject(i))
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /** Removes the first [count] events after successful send */
    fun drain(count: Int) {
        synchronized(lock) {
            try {
                val arr = readArray(queueFile)
                val remaining = JSONArray()
                for (i in count until arr.length()) remaining.put(arr.get(i))
                writeArray(queueFile, remaining)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to drain queue: ${e.message}")
            }
        }
    }

    fun peekPerformance(batchSize: Int = 50): List<JSONObject> {
        return synchronized(lock) {
            try {
                val arr = readArray(perfFile)
                buildList {
                    for (i in 0 until minOf(batchSize, arr.length())) add(arr.getJSONObject(i))
                }
            } catch (e: Exception) { emptyList() }
        }
    }

    fun drainPerformance(count: Int) {
        synchronized(lock) {
            try {
                val arr = readArray(perfFile)
                val remaining = JSONArray()
                for (i in count until arr.length()) remaining.put(arr.get(i))
                writeArray(perfFile, remaining)
            } catch (e: Exception) { Log.w(TAG, "Failed to drain perf queue") }
        }
    }

    fun size(): Int = synchronized(lock) {
        try { readArray(queueFile).length() } catch (e: Exception) { 0 }
    }

    private fun eventToJson(event: AnalyticsEvent): JSONObject {
        val props = JSONObject()
        event.toProperties().forEach { (k, v) -> props.put(k, v) }
        return JSONObject().apply {
            put("name", event.name)
            put("properties", props)
            put("ts", System.currentTimeMillis())
        }
    }

    private fun readArray(file: File): JSONArray {
        if (!file.exists()) return JSONArray()
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun writeArray(file: File, arr: JSONArray) {
        file.writeText(arr.toString())
    }
}
