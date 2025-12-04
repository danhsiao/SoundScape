package com.cs407.soundscape.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, SoundEventEntity::class], version = 2, exportSchema = false)
abstract class SoundscapeDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun soundEventDao(): SoundEventDao

    companion object {
        @Volatile
        private var INSTANCE: SoundscapeDatabase? = null

        fun getDatabase(context: Context): SoundscapeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundscapeDatabase::class.java,
                    "soundscape.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}