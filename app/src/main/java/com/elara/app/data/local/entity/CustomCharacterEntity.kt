package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "custom_characters")
data class CustomCharacterEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val emoji: String = "⭐",
    val description: String = "",
    val systemPrompt: String = "",
    val greeting: String = "",
    val avatarUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
