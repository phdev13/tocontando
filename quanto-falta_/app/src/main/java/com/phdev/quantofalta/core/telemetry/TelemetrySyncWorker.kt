package com.phdev.quantofalta.core.telemetry

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.*
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.core.analytics.AnalyticsQueue
import com.phdev.quantofalta.core.analytics.InstallationManager
import com.phdev.quantofalta.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

class TelemetrySyncWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TelemetrySyncWorker"
        private const val WORK_NAME = "telemetry_send"
        private const val BATCH_SIZE = 100
        private val BASE_URL: String
            get() = BuildConfig.API_BASE_URL

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
        
        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<TelemetrySyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME + "_immediate",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queue = AnalyticsQueue(context)
        val performanceDao = AppDatabase.getDatabase(context).performanceDao()
        val installationId = InstallationManager.getOrCreateId(context)
        val privacySettings = com.phdev.quantofalta.core.privacy.PrivacySettings(context)
        val canSendUsage = privacySettings.shareUsageData.first()
        val canSendPerformance = privacySettings.sharePerformanceData.first()
        if (!canSendUsage && !canSendPerformance) {
            return@withContext Result.success()
        }

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }

        val versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it).toInt() } ?: 1
        val versionName = packageInfo?.versionName ?: "1.0"

        // Register installation (UPSERT)
        try {
            val regPayload = JSONObject().apply {
                put("installationId", installationId)
                put("versionCode", versionCode)
                put("versionName", versionName)
                put("androidVersion", android.os.Build.VERSION.RELEASE)
                put("architecture", System.getProperty("os.arch")?.take(20))
                put("language", java.util.Locale.getDefault().language.take(10))
                put("manufacturer", android.os.Build.MANUFACTURER.take(50))
                put("model", android.os.Build.MODEL.take(50))
                put("releaseChannel", "stable")
            }
            post("$BASE_URL/api/v1/app/installations/register", regPayload.toString(), useGzip = false)
        } catch (e: Exception) {
            Log.w(TAG, "Install register failed: ${e.message}")
        }

        var allSuccess = true

        // 1. Send Event Batch
        try {
            val events = if (canSendUsage) queue.peek(BATCH_SIZE) else emptyList()
            if (events.isNotEmpty()) {
                val payload = JSONObject().apply {
                    put("installationId", installationId)
                    put("source", "android")
                    put("versionCode", versionCode)
                    put("events", JSONArray(events.map { e ->
                        JSONObject().apply {
                            put("event", e.getString("name"))
                            put("properties", e.optJSONObject("properties") ?: JSONObject())
                            put("occurredAt", e.optLong("ts", System.currentTimeMillis()))
                        }
                    }))
                }
                val success = post("$BASE_URL/api/v1/app/telemetry", payload.toString(), useGzip = true)
                if (success) {
                    queue.drain(events.size)
                } else {
                    allSuccess = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Analytics send failed: ${e.message}")
            allSuccess = false
        }

        // 2. Send General Performance Metrics (Startup/Queries)
        try {
            val perfMetrics = if (canSendPerformance) queue.peekPerformance(50) else emptyList()
            if (perfMetrics.isNotEmpty()) {
                val payload = JSONObject().apply {
                    put("installationId", installationId)
                    put("versionCode", versionCode)
                    put("androidVersion", android.os.Build.VERSION.RELEASE)
                    put("metrics", JSONArray(perfMetrics.map { m ->
                        JSONObject().apply {
                            put("type", m.getString("type"))
                            put("valueMs", m.getDouble("valueMs"))
                            m.optString("screen").takeIf { it.isNotBlank() }?.let { put("screen", it) }
                        }
                    }))
                }
                val success = post("$BASE_URL/api/v1/app/telemetry/performance", payload.toString(), useGzip = true)
                if (success) {
                    queue.drainPerformance(perfMetrics.size)
                } else {
                    allSuccess = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Perf metrics send failed: ${e.message}")
            allSuccess = false
        }

        // 3. Send locally aggregated startup and slow-frame measurements
        try {
            val dbMetrics = if (canSendPerformance) performanceDao.getAllMetrics().take(50) else emptyList()
            val invalidMetrics = dbMetrics.filter { it.durationMs <= 0 }
            if (invalidMetrics.isNotEmpty()) {
                performanceDao.deleteMetrics(invalidMetrics.map { it.id })
            }
            val validMetrics = dbMetrics.filter { it.durationMs > 0 }
            if (validMetrics.isNotEmpty()) {
                val payload = JSONObject().apply {
                    put("installationId", installationId)
                    put("versionCode", versionCode)
                    put("androidVersion", android.os.Build.VERSION.RELEASE)
                    put("metrics", JSONArray(validMetrics.map { metric ->
                        JSONObject().apply {
                            put("type", if (metric.metricType == "STARTUP") "cold_start" else "slow_frame")
                            put("valueMs", metric.durationMs)
                            put("screen", metric.screenName.take(64))
                        }
                    }))
                }
                val success = post(
                    "$BASE_URL/api/v1/app/telemetry/performance",
                    payload.toString(),
                    useGzip = true
                )
                if (success) {
                    performanceDao.deleteMetrics(validMetrics.map { it.id })
                } else {
                    allSuccess = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JankStats send failed: ${e.message}")
            allSuccess = false
        }

        return@withContext if (allSuccess) Result.success() else Result.retry()
    }

    private fun gzipContent(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(content.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    private fun post(url: String, json: String, useGzip: Boolean): Boolean {
        val code = postReturnCode(url, json, useGzip)
        return code in 200..299
    }
    
    private fun postReturnCode(url: String, json: String, useGzip: Boolean): Int {
        return try {
            val requestBuilder = Request.Builder().url(url)

            if (useGzip) {
                val gzipped = gzipContent(json)
                requestBuilder.addHeader("Content-Encoding", "gzip")
                requestBuilder.post(gzipped.toRequestBody("application/json".toMediaType()))
            } else {
                requestBuilder.post(json.toRequestBody("application/json".toMediaType()))
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                response.code
            }
        } catch (e: Exception) {
            Log.w(TAG, "POST failed to $url: ${e.message}")
            -1
        }
    }
}
