package com.phdev.quantofalta.core.performance

import android.app.Activity
import android.os.Build
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.phdev.quantofalta.core.database.PerformanceDao
import com.phdev.quantofalta.core.database.PerformanceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.Executors

data class JankFrameData(
    val screenName: String,
    val totalFrames: Long = 0,
    val jankFrames: Long = 0
)

/**
 * Aggregates JankStats data per screen and keeps them in memory.
 * Flushes to the Room DB when stop() is called (e.g., app goes to background).
 */
class JankStatsAggregator(
    private val activity: Activity,
    private val performanceDao: PerformanceDao,
    private val isDiagnosticsEnabled: Boolean
) {
    private var jankStats: JankStats? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val runId = UUID.randomUUID().toString()

    private val aggregatedData = MutableStateFlow<Map<String, JankFrameData>>(emptyMap())

    init {
        if (isDiagnosticsEnabled) {
            setupJankStats()
        }
    }

    private fun setupJankStats() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView)
            
            jankStats = JankStats.createAndTrack(activity.window) { frameData ->
                val state = frameData.states.find { it.key == "Screen" }?.value ?: "UnknownScreen"
                val isJank = frameData.isJank
                
                scope.launch {
                    aggregatedData.update { currentMap ->
                        val current = currentMap[state] ?: JankFrameData(state)
                        currentMap + (state to current.copy(
                            totalFrames = current.totalFrames + 1, 
                            jankFrames = current.jankFrames + if (isJank) 1 else 0
                        ))
                    }
                }
            }
            jankStats?.isTrackingEnabled = true
        }
    }

    fun setScreenState(screenName: String) {
        if (!isDiagnosticsEnabled) return
        val metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(activity.window.decorView)
        metricsStateHolder.state?.putState("Screen", screenName)
    }

    fun stop() {
        jankStats?.isTrackingEnabled = false
        flushToDatabase()
    }

    fun resume() {
        if (isDiagnosticsEnabled) {
            jankStats?.isTrackingEnabled = true
        }
    }

    private fun flushToDatabase() {
        if (!isDiagnosticsEnabled) return
        val currentData = aggregatedData.value
        if (currentData.isEmpty()) return

        scope.launch {
            val now = System.currentTimeMillis()
            currentData.values.forEach { data ->
                if (data.jankFrames > 0) {
                    performanceDao.insertMetric(
                        PerformanceEntity(
                            runId = runId,
                            metricType = "JANK",
                            screenName = data.screenName,
                            interaction = null,
                            totalFrames = data.totalFrames,
                            jankFrames = data.jankFrames,
                            durationMs = 0,
                            createdAtMillis = now
                        )
                    )
                }
            }
            // Clear memory after flush
            aggregatedData.value = emptyMap()
        }
    }
}
