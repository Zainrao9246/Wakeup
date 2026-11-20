package com.example

import android.app.Application
import com.example.alarm.AlarmScheduler
import com.example.data.AlarmRepository
import com.example.data.ClockPreferences
import com.example.data.WakeProofDatabase

class WakeProofApplication : Application() {

    lateinit var database: WakeProofDatabase
        private set

    lateinit var scheduler: AlarmScheduler
        private set

    lateinit var repository: AlarmRepository
        private set

    lateinit var preferences: ClockPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = WakeProofDatabase.getInstance(this)
        scheduler = AlarmScheduler(this)
        repository = AlarmRepository(database.alarmDao(), scheduler)
        preferences = ClockPreferences(this)
    }
}
