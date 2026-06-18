package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PerformanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: PerformanceEntity)

    @Query("SELECT * FROM performance_metrics")
    suspend fun getAllMetrics(): List<PerformanceEntity>

    @Query("SELECT COUNT(*) FROM performance_metrics")
    suspend fun getMetricsCount(): Int

    @Query("DELETE FROM performance_metrics WHERE id IN (:ids)")
    suspend fun deleteMetrics(ids: List<Long>)
    
    @Query("DELETE FROM performance_metrics")
    suspend fun clearAll()
}
