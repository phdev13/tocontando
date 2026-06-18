package com.phdev.quantofalta.core.analytics

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.core.privacy.PrivacySettings
import com.phdev.quantofalta.core.telemetry.TelemetrySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Public API for analytics throughout the app.
 * Fire-and-forget — never blocks the caller.
 * Respects privacy settings.
 */
class AnalyticsManager(private val context: Context) {

    companion object {
        private const val TAG = "AnalyticsManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val privacySettings = PrivacySettings(context)
    private val queue = AnalyticsQueue(context)

    fun track(event: AnalyticsEvent) {
        scope.launch {
            try {
                val canTrack = privacySettings.shareUsageData.first()
                if (!canTrack) return@launch

                queue.enqueue(event)
                TelemetrySyncWorker.scheduleImmediate(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue event ${event.name}: ${e.message}")
                // Silent failure — analytics never crashes the app
            }
        }
    }

    fun trackPerformance(metrics: List<PerformanceMetric>) {
        scope.launch {
            try {
                val canTrack = privacySettings.sharePerformanceData.first()
                if (!canTrack) return@launch
                queue.enqueuePerformance(metrics)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue performance metrics: ${e.message}")
            }
        }
    }
}

data class PerformanceMetric(
    val type: String,    // cold_start | warm_start | query_duration | render_time | slow_frame
    val valueMs: Double,
    val screen: String? = null,
)
