package com.phdev.quantofalta.core.testers

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class Tester(
    val id: String,
    val displayName: String,
    val nickname: String?,
    val avatarUrl: String?,
    val badgeKey: String?,
    val message: String?,
    val participationVersion: String?,
    val participationPeriod: String?,
    val isFeatured: Boolean
)

sealed class TestersState {
    object Loading : TestersState()
    data class Content(val testers: List<Tester>) : TestersState()
    object Error : TestersState()
}

class TestersManager(
    private val context: Context,
    private val analyticsManager: AnalyticsManager
) {
    companion object {
        private const val TAG = "TestersManager"
        private const val CACHE_FILE = "testers_cache.json"
        private val BASE_URL: String = BuildConfig.API_BASE_URL
    }

    private val cacheFile get() = File(context.filesDir, CACHE_FILE)
    
    private val _state = MutableStateFlow<TestersState>(TestersState.Loading)
    val state: StateFlow<TestersState> = _state.asStateFlow()

    suspend fun loadAndSync() = withContext(Dispatchers.IO) {
        _state.value = TestersState.Loading

        // 1. Load from cache immediately
        val cached = readCache()
        if (cached != null) {
            _state.value = TestersState.Content(cached.testers)
        }

        // 2. Fetch from network
        try {
            val url = URL("$BASE_URL/api/v1/public/testers")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (cached != null && cached.etag.isNotEmpty()) {
                conn.setRequestProperty("If-None-Match", cached.etag)
            }

            val responseCode = conn.responseCode
            if (responseCode == 304) {
                Log.d(TAG, "Testers not modified (304). Using cache.")
                analyticsManager.track(AnalyticsEvent.TestersSyncSucceeded)
                return@withContext
            }

            if (responseCode in 200..299) {
                val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                val newEtag = conn.getHeaderField("ETag") ?: ""
                
                val root = JSONObject(responseString)
                val json = root.optJSONObject("data") ?: root
                val newTesters = parseTesters(json)

                // Save to cache
                writeCache(responseString, newEtag)

                _state.value = TestersState.Content(newTesters)
                analyticsManager.track(AnalyticsEvent.TestersSyncSucceeded)
            } else {
                Log.w(TAG, "Failed to sync testers. Code: $responseCode")
                if (cached == null) {
                    _state.value = TestersState.Error
                }
                analyticsManager.track(AnalyticsEvent.TestersSyncFailed)
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing testers: ${e.message}")
            if (cached == null) {
                _state.value = TestersState.Error
            }
            analyticsManager.track(AnalyticsEvent.TestersSyncFailed)
        }
    }

    private fun parseTesters(json: JSONObject): List<Tester> {
        val list = mutableListOf<Tester>()
        if (json.has("testers")) {
            val arr = json.getJSONArray("testers")
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                list.add(
                    Tester(
                        id = t.getString("id"),
                        displayName = t.getString("displayName"),
                        nickname = t.optString("nickname", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        avatarUrl = t.optString("avatarUrl", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        badgeKey = t.optString("badgeKey", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        message = t.optString("message", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        participationVersion = t.optString("participationVersion", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        participationPeriod = t.optString("participationPeriod", null.toString()).takeIf { it != "null" && it.isNotEmpty() },
                        isFeatured = t.optBoolean("isFeatured", false)
                    )
                )
            }
        }
        return list
    }

    private fun readCache(): CachedData? {
        if (!cacheFile.exists()) return null
        try {
            val json = JSONObject(cacheFile.readText())
            val etag = json.optString("etag", "")
            val payload = json.getJSONObject("payload")
            return CachedData(etag, parseTesters(payload))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read testers cache", e)
            return null
        }
    }

    private fun writeCache(payloadString: String, etag: String) {
        try {
            val json = JSONObject()
            json.put("etag", etag)
            val root = JSONObject(payloadString)
            json.put("payload", root.optJSONObject("data") ?: root)
            cacheFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write testers cache", e)
        }
    }

    private data class CachedData(val etag: String, val testers: List<Tester>)
}
