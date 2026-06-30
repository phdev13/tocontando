package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(scheduledNotification: ScheduledNotificationEntity)

    @Query("SELECT * FROM scheduled_notifications")
    suspend fun getAll(): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE id = :id")
    suspend fun getById(id: String): ScheduledNotificationEntity?

    @Query("UPDATE scheduled_notifications SET status = :status, lastTriggeredAt = :triggeredAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, triggeredAt: Long?)

    @Query("""
        UPDATE scheduled_notifications
        SET status = 'DISPATCHING'
        WHERE id = :id AND triggerAt = :triggerAt AND status = 'SCHEDULED'
    """)
    suspend fun claimForDispatching(id: String, triggerAt: Long): Int

    @Query("UPDATE scheduled_notifications SET snoozeCount = snoozeCount + 1, status = 'SNOOZED' WHERE id = :id")
    suspend fun incrementSnooze(id: String)

    @Query("DELETE FROM scheduled_notifications WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM scheduled_notifications WHERE id = :id")
    suspend fun deleteById(id: String)
}
