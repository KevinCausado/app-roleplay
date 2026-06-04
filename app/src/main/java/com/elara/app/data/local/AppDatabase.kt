package com.elara.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elara.app.data.local.dao.CharacterMemoryDao
import com.elara.app.data.local.dao.CustomCharacterDao
import com.elara.app.data.local.dao.SessionDao
import com.elara.app.data.local.dao.WordDao
import com.elara.app.data.local.entity.CharacterMemoryEntity
import com.elara.app.data.local.entity.CustomCharacterEntity
import com.elara.app.data.local.entity.MessageEntity
import com.elara.app.data.local.entity.ProfileEntity
import com.elara.app.data.local.entity.SessionEntity
import com.elara.app.data.local.entity.WordEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class, ProfileEntity::class, WordEntity::class, CharacterMemoryEntity::class, CustomCharacterEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun wordDao(): WordDao
    abstract fun characterMemoryDao(): CharacterMemoryDao
    abstract fun customCharacterDao(): CustomCharacterDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "elara.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
