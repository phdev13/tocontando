package com.phdev.quantofalta.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.DeliveredMilestoneEntity
import com.phdev.quantofalta.data.repository.toDomainModel
import com.phdev.quantofalta.domain.CountdownCalculator
import com.phdev.quantofalta.domain.MilestoneDetector
import com.phdev.quantofalta.presentation.CountdownTextProvider
import java.time.Instant

class MilestoneWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val eventDao = database.eventDao()
        val milestoneDao = database.deliveredMilestoneDao()
        val calculator = CountdownCalculator()
        val textProvider = CountdownTextProvider(context)
        val now = Instant.now()

        val allEvents = eventDao.getAllEventsSync()
        
        for (entity in allEvents) {
            if (entity.isArchived || entity.isCompleted) continue
            
            val event = entity.toDomainModel()
            val result = calculator.calculate(event, now)
            
            val detectedMilestones = MilestoneDetector.detectMilestones(event, result)
            if (detectedMilestones.isEmpty()) continue

            val delivered = milestoneDao.getMilestonesForEvent(event.id).map { it.milestoneKey }

            for (milestone in detectedMilestones) {
                if (!delivered.contains(milestone)) {
                    sendNotification(event, milestone, textProvider, result)
                    
                    milestoneDao.insertMilestone(
                        DeliveredMilestoneEntity(
                            eventId = event.id,
                            milestoneKey = milestone,
                            deliveredAtMillis = now.toEpochMilli()
                        )
                    )
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(
        event: com.phdev.quantofalta.domain.model.Event,
        milestone: String,
        textProvider: CountdownTextProvider,
        result: com.phdev.quantofalta.domain.model.CountdownResult
    ) {
        // Here we'd call the Android NotificationManager to build and show the notification.
        // The title would be event.title, and the message would be textProvider.getFullText(result).
        // Since we are mocking the actual system notification call for now to keep it clean:
        val message = textProvider.getFullText(result)
        android.util.Log.i("MilestoneWorker", "Notification Sent for ${event.title} - $milestone: $message")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val builder = androidx.core.app.NotificationCompat.Builder(context, "milestones_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Marco alcançado: ${event.title}")
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(event.id.hashCode() + milestone.hashCode(), builder.build())
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
