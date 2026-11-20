package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_proof_logs")
data class WakeProofLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val alarmId: Long,
    val alarmLabel: String,
    val scheduledTimeFormatted: String,
    val verifiedAtMillis: Long = System.currentTimeMillis(),
    val reRingAttempts: Int = 0,
    val epochDay: Long
)
