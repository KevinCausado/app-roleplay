package com.elara.app.data.local.dao

import androidx.room.*
import com.elara.app.data.local.entity.CustomCharacterEntity

@Dao
interface CustomCharacterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(character: CustomCharacterEntity)

    @Delete
    suspend fun delete(character: CustomCharacterEntity)

    @Query("SELECT * FROM custom_characters ORDER BY createdAt DESC")
    suspend fun getAll(): List<CustomCharacterEntity>

    @Query("SELECT * FROM custom_characters WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomCharacterEntity?
}
