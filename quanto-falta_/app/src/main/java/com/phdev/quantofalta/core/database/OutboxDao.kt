package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(outboxEntity: OutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OutboxEntity>)

    @Query("SELECT * FROM outbox ORDER BY queuedAt ASC")
    suspend fun getAllPending(): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM outbox WHERE eventId IN (:eventIds)")
    suspend fun deleteByEventIds(eventIds: List<String>)
    
    @Query("SELECT * FROM outbox WHERE eventId = :eventId LIMIT 1")
    suspend fun getByEventId(eventId: String): OutboxEntity?
}
