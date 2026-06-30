package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deletedAt IS NULL")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE deletedAt IS NULL AND isArchived = 0 AND isCompleted = 0
        ORDER BY targetDate ASC
    """)
    fun getActiveEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE deletedAt IS NULL")
    suspend fun getAllEventsSync(): List<EventEntity>
    
    @Query("SELECT COUNT(*) FROM events WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    fun countActiveEvents(): Flow<Int>

    @Query("SELECT COUNT(*) FROM events WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL AND type = :type")
    fun countActiveEventsByType(type: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM events WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL")
    suspend fun getActiveEventCountSync(): Int

    @Query("SELECT COUNT(*) FROM events WHERE isCompleted = 0 AND isArchived = 0 AND deletedAt IS NULL AND type = :type")
    suspend fun getActiveEventCountByTypeSync(type: String): Int

    @Query("SELECT COUNT(*) FROM events WHERE isCompleted = 1 AND isArchived = 0 AND deletedAt IS NULL")
    fun countCompletedEvents(): Flow<Int>
    
    @Query("SELECT * FROM events WHERE id = :id AND deletedAt IS NULL")
    fun getEventById(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventByIdSync(id: String): EventEntity?

    @Query("SELECT id, title, targetDate, colorArgb, isPrivate, isCompleted, coverImageUri, updatedAt FROM events WHERE id = :id AND deletedAt IS NULL")
    suspend fun getWidgetEventByIdSync(id: String): WidgetEventData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)
    
    @androidx.room.Update
    suspend fun updateAll(events: List<EventEntity>)
    
    @Query("UPDATE events SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE', localRevision = localRevision + 1, updatedAt = :deletedAt WHERE id = :id")
    suspend fun deleteEventById(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE events SET title = '', iconName = 'Star', coverImageUri = NULL, isPrivate = 0, isPinned = 0 WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun redactDeletedEvent(id: String)

    @Query("UPDATE events SET deletedAt = :deletedAt, syncState = 'SYNCED', localRevision = :revision, serverRevision = :revision, updatedAt = :deletedAt WHERE id = :id")
    suspend fun applyRemoteDelete(id: String, revision: Int, deletedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE events SET isCompleted = :isCompleted, syncState = 'PENDING_UPDATE', localRevision = localRevision + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEventCompletedStatus(id: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE events SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE', localRevision = localRevision + 1, updatedAt = :deletedAt WHERE deletedAt IS NULL")
    suspend fun deleteAllEvents(deletedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM events")
    suspend fun physicalDeleteAllEvents()
    
    @Query("UPDATE events SET isArchived = :isArchived, syncState = 'PENDING_UPDATE', localRevision = localRevision + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEventArchivedStatus(id: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE events SET isPinned = :isPinned, syncState = 'PENDING_UPDATE', localRevision = localRevision + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateEventPinnedStatus(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE events
        SET relationshipMonthlyEnabled = :enabled,
            syncState = 'PENDING_UPDATE',
            localRevision = localRevision + 1,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateRelationshipMonthly(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE events
        SET relationshipAnnualEnabled = :enabled,
            syncState = 'PENDING_UPDATE',
            localRevision = localRevision + 1,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateRelationshipAnnual(id: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE events SET isPinned = 0, syncState = 'PENDING_UPDATE', localRevision = localRevision + 1, updatedAt = :updatedAt WHERE isPinned = 1")
    suspend fun unpinAllEvents(updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE events
        SET lastCelebrationEpochDay = :epochDay,
            syncState = 'PENDING_UPDATE',
            localRevision = localRevision + 1,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateLastCelebrationEpochDay(id: String, epochDay: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE events
        SET serverRevision = :serverRevision,
            syncState = CASE WHEN updatedAt <= :sentAt THEN 'SYNCED' ELSE 'PENDING_UPDATE' END
        WHERE id = :id
    """)
    suspend fun acknowledgeSync(id: String, serverRevision: Int, sentAt: Long)
}
