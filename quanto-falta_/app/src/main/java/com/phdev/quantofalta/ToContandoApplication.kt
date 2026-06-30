package com.phdev.quantofalta

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.phdev.quantofalta.core.AppContainer
import com.phdev.quantofalta.core.config.AppConfigManager
import com.phdev.quantofalta.core.telemetry.TelemetrySyncWorker
import com.phdev.quantofalta.core.ota.OtaWorker
import com.phdev.quantofalta.core.ota.OtaNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ToContandoApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    lateinit var container: AppContainer

    companion object {
        var appStartTime: Long = 0
    }

    override fun onCreate() {
        appStartTime = System.currentTimeMillis()
        super.onCreate()
        container = AppContainer(this)
        
        // Create OTA notification channel early — must exist before any worker fires
        if (AppConfigManager.isOtaEnabled(this)) {
            OtaNotificationHelper.ensureChannel(this)
        }
        
        // Schedule background jobs after the first screen has time to settle.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            delay(10_000)
            try {
                AppConfigManager.syncWithServer(this@ToContandoApplication)
                if (AppConfigManager.isOtaEnabled(this@ToContandoApplication)) {
                    OtaNotificationHelper.ensureChannel(this@ToContandoApplication)
                    OtaWorker.schedule(this@ToContandoApplication)
                } else {
                    OtaWorker.cancel(this@ToContandoApplication)
                    OtaNotificationHelper.cancelAll(this@ToContandoApplication)
                    container.otaManager.cleanup()
                }
                TelemetrySyncWorker.schedule(this@ToContandoApplication)
                com.phdev.quantofalta.domain.worker.MilestoneWorker.schedule(this@ToContandoApplication)
                com.phdev.quantofalta.core.sync.SyncWorker.enqueuePeriodic(this@ToContandoApplication)
                com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler(this@ToContandoApplication).scheduleDailyUpdates()
                com.phdev.quantofalta.core.feedback.FeedbackRetryWorker.enqueue(this@ToContandoApplication)
        } catch (e: Exception) {
            android.util.Log.e("ToContandoApp", "Erro não-crítico ao agendar background jobs", e)
            }
        }
        
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30) // 30% of available memory – keeps cover photos snappy
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // 5% of free disk space for user cover photos
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false) // Offline-first; rely on Coil's internal caching
            .build()
    }
}
