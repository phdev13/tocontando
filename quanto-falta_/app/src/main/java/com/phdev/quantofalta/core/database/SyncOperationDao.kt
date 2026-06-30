package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<SyncOperationEntity>)

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Query(
        """
        SELECT COUNT(*) FROM sync_operations
        WHERE entityType = :entityType
          AND entityId = :entityId
          AND (status = 'PENDING' OR status = 'FAILED')
        """
    )
    suspend fun countPendingForEntity(entityType: String, entityId: String): Int

    @Query("UPDATE sync_operations SET status = :status WHERE operationId = :operationId")
    suspend fun updateStatus(operationId: String, status: SyncOperationStatus)

    @Query("DELETE FROM sync_operations WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)

    @Query("DELETE FROM sync_operations WHERE operationId IN (:operationIds)")
    suspend fun deleteOperations(operationIds: List<String>)

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAllOperations()

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAll()
}
