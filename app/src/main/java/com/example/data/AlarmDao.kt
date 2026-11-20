package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC, id ASC")
    fun observeAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 OR isPendingReRing = 1")
    suspend fun getActiveAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    fun observeAlarmById(id: Long): Flow<AlarmEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeLog(log: WakeProofLogEntity): Long

    @Query("SELECT * FROM wake_proof_logs ORDER BY verifiedAtMillis DESC LIMIT 30")
    fun observeRecentWakeLogs(): Flow<List<WakeProofLogEntity>>
}
