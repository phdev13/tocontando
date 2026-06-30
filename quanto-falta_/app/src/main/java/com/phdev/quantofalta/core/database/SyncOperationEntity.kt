package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey
    val operationId: String,
    val entityType: String, // e.g. "card"
    val entityId: String,
    val operationType: String, // "CREATE", "UPDATE", "DELETE", "RESET"
    val payload: String?, // JSON payload if necessary
    val status: SyncOperationStatus,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SyncOperationStatus {
    PENDING,
    IN_FLIGHT,
    FAILED
}
