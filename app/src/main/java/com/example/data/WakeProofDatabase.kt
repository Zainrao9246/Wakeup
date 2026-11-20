package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class, WakeProofLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WakeProofDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: WakeProofDatabase? = null

        fun getInstance(context: Context): WakeProofDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WakeProofDatabase::class.java,
                    "wakeproof_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
