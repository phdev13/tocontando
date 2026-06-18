package com.phdev.quantofalta.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeliveredMilestoneDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMilestone(milestone: DeliveredMilestoneEntity): Long

    @Query("SELECT * FROM delivered_milestones WHERE eventId = :eventId")
    suspend fun getMilestonesForEvent(eventId: String): List<DeliveredMilestoneEntity>

    @Query("DELETE FROM delivered_milestones WHERE eventId = :eventId")
    suspend fun deleteMilestonesForEvent(eventId: String)
}
