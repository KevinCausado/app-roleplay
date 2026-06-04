package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val word: String,
    val translation: String = "",
    val context: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val nextReview: Long = System.currentTimeMillis() + 86400000L,
    val reviewInterval: Int = 1,
    val reviewCount: Int = 0
)
