package com.elara.app.data.local.dao

import androidx.room.*
import com.elara.app.data.local.entity.MessageEntity
import com.elara.app.data.local.entity.ProfileEntity
import com.elara.app.data.local.entity.SessionEntity

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: Long): List<MessageEntity>

    @Query("SELECT * FROM sessions WHERE world = :world AND personality = :personality ORDER BY startTime DESC LIMIT 1")
    suspend fun getSessionByCombo(world: String, personality: String): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastSession(): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): ProfileEntity?

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessages(sessionId: Long)
}
