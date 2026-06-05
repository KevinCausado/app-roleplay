package com.elara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.elara.app.data.local.entity.StudyPlanEntity

@Dao
interface StudyPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: StudyPlanEntity)

    @Query("SELECT * FROM study_plan WHERE id = 1")
    suspend fun get(): StudyPlanEntity?
}
