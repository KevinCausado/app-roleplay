package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val world: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val escalationLevel: Int = 0
)
