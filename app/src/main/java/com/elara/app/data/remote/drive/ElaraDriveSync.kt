package com.elara.app.data.remote.drive

import android.content.Context
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.CharacterMemoryEntity
import com.elara.app.data.local.entity.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ElaraDriveSync(private val context: Context) {

    val driveService = GoogleDriveService(context)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun pushAll(): Boolean = withContext(Dispatchers.IO) {
        if (!driveService.isAuthenticated()) return@withContext false
        val db = AppDatabase.get(context)
        var ok = true

        // Words
        val words = db.wordDao().getAll()
        ok = ok && driveService.writeFile(DriveConstants.FILE_WORDS, json.encodeToString(words))

        // Character memory
        val memories = listOf("ELARA", "STACY", "LAYLA", "TYLER", "RAVEN")
            .mapNotNull { db.characterMemoryDao().get(it) }
        ok = ok && driveService.writeFile(DriveConstants.FILE_CHARACTER_MEMORY, json.encodeToString(memories))

        ok
    }

    suspend fun pullAll(): Boolean = withContext(Dispatchers.IO) {
        if (!driveService.isAuthenticated()) return@withContext false
        val db = AppDatabase.get(context)
        var ok = true

        // Words
        driveService.readFile(DriveConstants.FILE_WORDS)?.let { raw ->
            runCatching {
                val words = json.decodeFromString<List<WordEntity>>(raw)
                words.forEach { db.wordDao().insert(it) }
            }
        } ?: run { ok = false }

        // Character memory
        driveService.readFile(DriveConstants.FILE_CHARACTER_MEMORY)?.let { raw ->
            runCatching {
                val memories = json.decodeFromString<List<CharacterMemoryEntity>>(raw)
                memories.forEach { db.characterMemoryDao().upsert(it) }
            }
        }

        ok
    }
}
