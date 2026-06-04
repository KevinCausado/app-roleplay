package com.elara.app.data.local.dao

import androidx.room.*
import com.elara.app.data.local.entity.CharacterMemoryEntity

@Dao
interface CharacterMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: CharacterMemoryEntity)

    @Query("SELECT * FROM character_memory WHERE characterId = :characterId LIMIT 1")
    suspend fun get(characterId: String): CharacterMemoryEntity?
}
