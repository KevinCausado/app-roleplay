package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_memory")
data class CharacterMemoryEntity(
    @PrimaryKey val characterId: String,
    val memory: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
