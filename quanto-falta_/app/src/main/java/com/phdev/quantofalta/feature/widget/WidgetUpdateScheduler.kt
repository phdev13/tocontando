package com.phdev.quantofalta.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import java.util.Calendar

class WidgetUpdateScheduler(private val context: Context) {

    suspend fun updateWidgetsForEvent(eventId: String) {
        val manager = GlanceAppWidgetManager(context)
        
        val widgetClasses = listOf(EventWidget::class.java, MicroWidget::class.java, HeroWidget::class.java, ListWidget::class.java)
        val widgetInstances = listOf(EventWidget(), MicroWidget(), HeroWidget(), ListWidget())
        
        widgetClasses.forEachIndexed { index, clazz ->
            val glanceIds = manager.getGlanceIds(clazz)
            for (glanceId in glanceIds) {
                val prefs = androidx.glance.appwidget.state.getAppWidgetState(context, androidx.glance.state.PreferencesGlanceStateDefinition, glanceId)
                val mappedEventId = prefs[EventWidget.eventIdKey]
                
                if (mappedEventId == eventId || clazz == ListWidget::class.java) {
                    widgetInstances[index].update(context, glanceId)
                }
            }
        }
    }
    
    suspend fun updateAllWidgets() {
        val manager = GlanceAppWidgetManager(context)
        
        val widgetClasses = listOf(EventWidget::class.java, MicroWidget::class.java, HeroWidget::class.java, ListWidget::class.java)
        val widgetInstances = listOf(EventWidget(), MicroWidget(), HeroWidget(), ListWidget())
        
        widgetClasses.forEachIndexed { index, clazz ->
            val glanceIds = manager.getGlanceIds(clazz)
            for (glanceId in glanceIds) {
                widgetInstances[index].update(context, glanceId)
            }
        }
    }

    fun scheduleDailyUpdates() {
        // Schedule a daily job shortly after midnight to update all widgets
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 5)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WidgetDailyUpdateWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    fun cancelDailyUpdates() {
        WorkManager.getInstance(context).cancelUniqueWork("WidgetDailyUpdateWork")
    }
}
