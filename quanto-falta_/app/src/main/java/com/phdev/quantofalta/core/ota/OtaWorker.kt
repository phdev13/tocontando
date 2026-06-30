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
import com.phdev.quantofalta.core.config.AppConfigManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * WorkManager periodic worker for OTA checks.
 *
 * Improvements:
 *  - Checks every 4h (was 6h) for faster delivery
 *  - Checks on any connected network so releases appear promptly after publishing
 *  - Creates the notification channel before doing anything (harmless no-op if already created)
 *  - Respects battery, storage, and network constraints
 *  - Adds per-device jitter to prevent thundering herd
 */
class OtaWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "OtaWorker"
        private const val WORK_NAME = "ota_check_periodic"

        /** How often to do the periodic check */
        private const val INTERVAL_HOURS = 4L

        /** Flex window: worker can run anywhere in the last [FLEX_HOURS] of each interval */
        private const val FLEX_HOURS = 1L

        /** Minimum real-time gap between two checks (prevents duplicate checks when app restarts) */
        private const val MIN_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000 // 3 hours

        fun schedule(context: Context) {
            if (!AppConfigManager.isOtaEnabled(context)) {
                cancel(context)
                OtaNotificationHelper.cancelAll(context)
                return
            }

            // Ensure the notification channel exists before the first download completes
            OtaNotificationHelper.ensureChannel(context)

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
                // Per-device jitter 0..20 min so devices don't all hit the server simultaneously
                .setInitialDelay(Random.nextLong(0, 20), TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP: don't reset the existing schedule if already running
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.d(TAG, "OTA worker scheduled (${INTERVAL_HOURS}h interval, ${FLEX_HOURS}h flex)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        if (!AppConfigManager.isOtaEnabled(context)) {
            OtaNotificationHelper.cancelAll(context)
            OtaManager.getInstance(context).cleanup()
            return Result.success()
        }

        Log.d(TAG, "OTA check starting...")
        OtaNotificationHelper.ensureChannel(context)

        val dataStore = OtaDataStore(context)

        // Per-device cooldown — guards against the worker firing multiple times
        // within the same session (e.g., network change events).
        val lastCheck = dataStore.lastCheckTimestamp.first()
        if (System.currentTimeMillis() - lastCheck < MIN_CHECK_INTERVAL_MS) {
            Log.d(TAG, "OTA check skipped — too soon since last check")
            return Result.success()
        }

        // If an APK is already downloaded and valid, don't re-download — just re-notify.
        val pendingPath = dataStore.downloadedApkPath.first()
        val pendingVersion = dataStore.downloadedVersionCode.first()
        if (pendingPath != null && pendingVersion > 0) {
            val otaManager = OtaManager.getInstance(context)
            // Let OtaManager handle re-surfacing the notification
            otaManager.checkForPendingInstallation()
            dataStore.recordCheck()
            Log.d(TAG, "OTA re-notified for already-downloaded v$pendingVersion")
            return Result.success()
        }

        return try {
            val otaManager = OtaManager.getInstance(context)
            val found = otaManager.checkForUpdates()
            dataStore.recordCheck()
            if (found) {
                Log.d(TAG, "OTA check found update — download started in background")
            } else {
                Log.d(TAG, "OTA check completed — no update available")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "OTA check failed: ${e.message}")
            Result.retry()
        }
    }
}
