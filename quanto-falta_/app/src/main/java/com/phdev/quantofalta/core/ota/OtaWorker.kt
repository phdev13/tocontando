package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.privacy.PrivacySettings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * WorkManager periodic worker for OTA checks.
 * Respects battery, network, and storage constraints.
 * Adds jitter to prevent thundering herd on servers.
 */
class OtaWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "OtaWorker"
        private const val WORK_NAME = "ota_check_periodic"
        private const val INTERVAL_HOURS = 6L
        private const val FLEX_HOURS = 2L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<OtaWorker>(
                INTERVAL_HOURS, TimeUnit.HOURS,
                FLEX_HOURS, TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                // Jitter: initial delay 0..30 min to distribute first check
                .setInitialDelay(Random.nextLong(0, 30), TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.d(TAG, "OTA worker scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        if (com.phdev.quantofalta.BuildConfig.FLAVOR == "playStore") {
            Log.d(TAG, "OTA check disabled for playStore flavor")
            return Result.success()
        }

        Log.d(TAG, "OTA check starting...")

        val otaManager = OtaManager.getInstance(context)
        val dataStore = OtaDataStore(context)

        // Cooldown: don't check too often (respect server-side config)
        val lastCheck = dataStore.lastCheckTimestamp.first()
        val minIntervalMs = INTERVAL_HOURS * 60 * 60 * 1000
        if (System.currentTimeMillis() - lastCheck < minIntervalMs) {
            Log.d(TAG, "OTA check skipped — too soon since last check")
            return Result.success()
        }

        return try {
            otaManager.checkForUpdates()
            dataStore.recordCheck()
            Log.d(TAG, "OTA check completed")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "OTA check failed: ${e.message}")
            Result.retry()
        }
    }
}
