package com.phdev.quantofalta

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.phdev.quantofalta.core.AppContainer
import com.phdev.quantofalta.core.telemetry.TelemetrySyncWorker
import com.phdev.quantofalta.core.ota.OtaWorker
import kotlinx.coroutines.GlobalScope
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
        
        // Schedule background jobs de forma segura
        try {
            OtaWorker.schedule(this)
            TelemetrySyncWorker.schedule(this)
            com.phdev.quantofalta.domain.worker.MilestoneWorker.schedule(this)
            com.phdev.quantofalta.core.sync.SyncWorker.enqueuePeriodic(this)
            com.phdev.quantofalta.feature.widget.WidgetUpdateScheduler(this).scheduleDailyUpdates()
        } catch (e: Exception) {
            android.util.Log.e("ToContandoApp", "Erro não-crítico ao agendar background jobs", e)
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
                    .maxSizePercent(0.20) // Use at most 20% of the app's available memory.
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use at most 2% of the device's free disk space.
                    .build()
            }
            .crossfade(true) // Crossfade allows smooth transitions without sacrificing much performance.
            // .allowHardware(true) is true by default in modern Coil versions.
            .respectCacheHeaders(false) // Optimize for offline-first, rely on Coil's internal caching.
            .build()
    }
}
