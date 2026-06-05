package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "errors")
data class ErrorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val original: String,
    val corrected: String,
    val context: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0,
    val reviewCount: Int = 0
)
