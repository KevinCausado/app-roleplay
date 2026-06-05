package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_plan")
data class StudyPlanEntity(
    @PrimaryKey val id: Int = 1,
    val currentCefrLevel: String = "A1",
    val completedScenarios: String = "",
    val dailyGoalMinutes: Int = 10,
    val lastStudyDate: String = "",
    val studyStreak: Int = 0
)
