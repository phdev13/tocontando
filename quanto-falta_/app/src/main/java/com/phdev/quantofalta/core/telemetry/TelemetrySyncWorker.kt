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
            val events = queue.peek(BATCH_SIZE)
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
            val perfMetrics = queue.peekPerformance(BATCH_SIZE)
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

        // 3. Send JankStats / Macrobenchmark Runs
        try {
            val dbMetrics = performanceDao.getAllMetrics()
            if (dbMetrics.isNotEmpty()) {
                val groupedMetrics = dbMetrics.groupBy { it.runId }
                for ((runId, runMetrics) in groupedMetrics) {
                    val payload = JSONObject().apply {
                        put("id", runId)
                        put("source", "JANKSTATS")
                        put("app_version", versionName)
                        put("version_code", versionCode)
                        put("build_type", BuildConfig.BUILD_TYPE)
                        put("device_model", android.os.Build.MODEL)
                        put("device_manufacturer", android.os.Build.MANUFACTURER)
                        put("android_version", android.os.Build.VERSION.RELEASE)
                        put("api_level", android.os.Build.VERSION.SDK_INT)
                        put("status", "COMPLETED")
                        put("payload_hash", java.util.UUID.randomUUID().toString()) 
                    }

                    var runCreated = false
                    try {
                        val responseCode = postReturnCode("$BASE_URL/api/v1/performance/runs", payload.toString(), useGzip = true, auth = "PUBLIC_INGEST_TOKEN")
                        if (responseCode in 200..299 || responseCode == 409) {
                            runCreated = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        allSuccess = false
                        continue
                    }

                    if (!runCreated) {
                        allSuccess = false
                        continue
                    }

                    // Send Metrics for the run
                    val metricsArray = JSONArray()
                    for (m in runMetrics) {
                        if (m.metricType == "STARTUP") {
                            metricsArray.put(JSONObject().apply {
                                put("id", java.util.UUID.randomUUID().toString())
                                put("metric_name", "StartupTime")
                                put("value", m.durationMs)
                                put("unit", "ms")
                            })
                        } else if (m.metricType == "JANK") {
                            metricsArray.put(JSONObject().apply {
                                put("id", java.util.UUID.randomUUID().toString())
                                put("metric_name", "TotalFrames_${m.screenName}")
                                put("value", m.totalFrames)
                                put("unit", "frames")
                            })
                            metricsArray.put(JSONObject().apply {
                                put("id", java.util.UUID.randomUUID().toString())
                                put("metric_name", "JankFrames_${m.screenName}")
                                put("value", m.jankFrames)
                                put("unit", "frames")
                            })
                        }
                    }

                    val metricsPayload = JSONObject().apply {
                        put("metrics", metricsArray)
                    }

                    try {
                        val success = post("$BASE_URL/api/v1/performance/runs/$runId/metrics", metricsPayload.toString(), useGzip = true, auth = "PUBLIC_INGEST_TOKEN")
                        if (success) {
                            performanceDao.deleteMetrics(runMetrics.map { it.id })
                        } else {
                            allSuccess = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        allSuccess = false
                    }
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

    private fun post(url: String, json: String, useGzip: Boolean, auth: String? = null): Boolean {
        val code = postReturnCode(url, json, useGzip, auth)
        return code in 200..299
    }
    
    private fun postReturnCode(url: String, json: String, useGzip: Boolean, auth: String? = null): Int {
        return try {
            val requestBuilder = Request.Builder().url(url)
            
            if (auth != null) {
                requestBuilder.addHeader("Authorization", "Bearer $auth")
            }

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
