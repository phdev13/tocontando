package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTimelineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEntry(entry: EventTimelineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelines(entries: List<EventTimelineEntity>)

    @Query("SELECT * FROM event_timeline ORDER BY timestampMillis ASC")
    suspend fun getAllTimelinesSync(): List<EventTimelineEntity>

    @Query("SELECT * FROM event_timeline WHERE eventId = :eventId ORDER BY timestampMillis ASC")
    fun getTimelineForEvent(eventId: String): Flow<List<EventTimelineEntity>>
    
    @Query("DELETE FROM event_timeline WHERE eventId = :eventId")
    suspend fun deleteTimelineForEvent(eventId: String)
}
