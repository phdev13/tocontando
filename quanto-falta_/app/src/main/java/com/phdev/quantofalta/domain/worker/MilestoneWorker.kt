package com.phdev.quantofalta.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.DeliveredMilestoneEntity
import com.phdev.quantofalta.data.repository.toDomainModel
import com.phdev.quantofalta.domain.CountdownCalculator
import com.phdev.quantofalta.domain.MilestoneDetector
import com.phdev.quantofalta.core.utils.AppCopyProvider
import java.time.Instant
import kotlinx.coroutines.flow.first

class MilestoneWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val eventDao = database.eventDao()
        val milestoneDao = database.deliveredMilestoneDao()
        val calculator = CountdownCalculator()
        val textProvider = AppCopyProvider
        val now = Instant.now()
        val isPremium = com.phdev.quantofalta.billing.EntitlementManager(context)
            .hasActivePremium.first()
        if (!isPremium) return Result.success()

        val allEvents = eventDao.getAllEventsSync()
        val state = context.getSharedPreferences("milestone_state", Context.MODE_PRIVATE)
        
        for (entity in allEvents) {
            if (entity.isArchived || entity.isCompleted) continue
            if (!entity.relationshipMilestonesEnabled) continue
            
            val event = entity.toDomainModel()
            val result = calculator.calculate(event, now)
            val currentDays = (result as? com.phdev.quantofalta.domain.model.CountdownResult.Days)?.days?.toInt()
            val stateKey = "previous_days_${event.id}"
            val previousDays = if (state.contains(stateKey)) state.getInt(stateKey, 0) else null
            
            val detectedMilestones = MilestoneDetector.detectMilestones(event, result, previousDays)
            if (currentDays != null) state.edit().putInt(stateKey, currentDays).apply()
            if (detectedMilestones.isEmpty()) continue

            val delivered = milestoneDao.getMilestonesForEvent(event.id).map { it.milestoneKey }

            for (milestone in detectedMilestones) {
                if (!delivered.contains(milestone)) {
                    val inserted = milestoneDao.insertMilestone(
                        DeliveredMilestoneEntity(
                            eventId = event.id,
                            milestoneKey = milestone,
                            deliveredAtMillis = now.toEpochMilli()
                        )
                    )
                    if (inserted != -1L) {
                        sendNotification(event, milestone, textProvider, result)
                    }
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(
        event: com.phdev.quantofalta.domain.model.Event,
        milestone: String,
        textProvider: com.phdev.quantofalta.core.utils.AppCopyProvider,
        result: com.phdev.quantofalta.domain.model.CountdownResult
    ) {
        val message = textProvider.getFullText(result)
        android.util.Log.i("MilestoneWorker", "Notification Sent for ${event.title} - $milestone: $message")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val openIntent = android.content.Intent(context, com.phdev.quantofalta.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", event.id)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            com.phdev.quantofalta.core.notifications.NotificationIds.fromKey("open:milestone:${event.id}:$milestone"),
            openIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        com.phdev.quantofalta.core.notifications.NotificationChannels.createAllChannels(context)
        val builder = androidx.core.app.NotificationCompat.Builder(
            context,
            com.phdev.quantofalta.core.notifications.NotificationChannels.CHANNEL_UPCOMING
        )
            .setSmallIcon(com.phdev.quantofalta.R.drawable.ic_notification)
            .setContentTitle(if (event.isPrivate) "Marco de evento privado" else "Marco alcançado: ${event.title}")
            .setContentText(if (event.isPrivate) "Abra o aplicativo para visualizar." else message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(com.phdev.quantofalta.core.notifications.NotificationGrouping.GROUP_EVENTS)
            .setGroupAlertBehavior(androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(
                0,
                "Concluir",
                com.phdev.quantofalta.core.notifications.NotificationFactory.completeAction(
                    context, event.id, milestone
                )
            )

        notificationManager.notify(
            com.phdev.quantofalta.core.notifications.NotificationIds.fromKey("milestone:${event.id}:$milestone"),
            builder.build()
        )
        com.phdev.quantofalta.core.notifications.NotificationGrouping.publishSummary(
            context,
            com.phdev.quantofalta.core.notifications.NotificationChannels.CHANNEL_UPCOMING
        )
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = androidx.work.PeriodicWorkRequestBuilder<MilestoneWorker>(
                12, java.util.concurrent.TimeUnit.HOURS,
                1, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MilestoneWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
