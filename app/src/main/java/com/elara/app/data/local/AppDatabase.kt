package com.elara.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elara.app.data.local.dao.SessionDao
import com.elara.app.data.local.entity.MessageEntity
import com.elara.app.data.local.entity.ProfileEntity
import com.elara.app.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class, ProfileEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

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
