package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventReminderDao {
    @Query("SELECT * FROM event_reminders WHERE eventId = :eventId")
    fun getRemindersForEvent(eventId: String): Flow<List<EventReminderEntity>>

    @Query("SELECT * FROM event_reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEventSync(eventId: String): List<EventReminderEntity>

    @Query("SELECT * FROM event_reminders")
    suspend fun getAllRemindersSync(): List<EventReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<EventReminderEntity>)

    @Query("DELETE FROM event_reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersForEvent(eventId: String)

    @Query("UPDATE event_reminders SET enabled = :enabled WHERE id = :id")
    suspend fun updateReminderStatus(id: String, enabled: Boolean)
}
