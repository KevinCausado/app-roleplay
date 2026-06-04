package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "character_memory")
data class CharacterMemoryEntity(
    @PrimaryKey val characterId: String,
    val memory: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
