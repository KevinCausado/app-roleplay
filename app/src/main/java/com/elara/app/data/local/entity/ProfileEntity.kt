package com.elara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val estimatedLevel: Float = 0.2f,
    val escalationLevel: Int = 0,
    val currentWorld: String = "MODERN",
    val currentSessionId: Long? = null,
    // Lorebook — hechos acumulados sobre Kevin que siempre van al prompt
    val lorebookEntries: String = DEFAULT_LOREBOOK
) {
    companion object {
        const val DEFAULT_LOREBOOK = """
[KEVIN — PERMANENT PROFILE]
Name: Kevin Causado. Age: 33. Colombian. Developer (React, Laravel, Android).
Interests: calisthenics, programming, AI/tech, roleplay, sex, economics, politics, cooking.
Personality: curious, direct, adaptable. Playful smile. Confident gaze.
English level: beginner-intermediate. Speaks broken English — that's fine and normal.
He is learning English through immersion. Never correct him explicitly.
        """
    }
}
