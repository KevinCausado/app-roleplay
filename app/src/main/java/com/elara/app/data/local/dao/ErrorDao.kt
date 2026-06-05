package com.elara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elara.app.data.local.entity.ErrorEntity

@Dao
interface ErrorDao {

    @Insert
    suspend fun insert(error: ErrorEntity)

    @Query("SELECT * FROM errors ORDER BY savedAt DESC")
    suspend fun getAll(): List<ErrorEntity>

    @Query("SELECT * FROM errors ORDER BY reviewCount ASC, savedAt DESC LIMIT 10")
    suspend fun getForReview(): List<ErrorEntity>

    @Query("UPDATE errors SET reviewCount = reviewCount + 1, reviewedAt = :now WHERE id = :id")
    suspend fun markReviewed(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM errors WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM errors")
    suspend fun count(): Int
}
