package com.elara.app.data.local.dao

import androidx.room.*
import com.elara.app.data.local.entity.WordEntity

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity)

    @Update
    suspend fun update(word: WordEntity)

    @Query("SELECT * FROM words ORDER BY savedAt DESC")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): WordEntity?

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words WHERE nextReview <= :now ORDER BY nextReview ASC LIMIT 20")
    suspend fun getDueForReview(now: Long = System.currentTimeMillis()): List<WordEntity>

    @Query("SELECT COUNT(DISTINCT word) FROM words")
    suspend fun uniqueCount(): Int
}
