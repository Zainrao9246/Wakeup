package com.example.data

import com.example.alarm.AlarmScheduler
import com.example.alarm.ProofPhraseGenerator
import kotlinx.coroutines.flow.Flow
import java.util.Locale

sealed interface ProofVerificationResult {
    data class Success(val alarmLabel: String, val attempts: Int) : ProofVerificationResult
    data class Mismatch(val message: String) : ProofVerificationResult
    data object NotFound : ProofVerificationResult
}

class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler
) {
    val alarms: Flow<List<AlarmEntity>> = dao.observeAllAlarms()
    val recentWakeLogs: Flow<List<WakeProofLogEntity>> = dao.observeRecentWakeLogs()

    fun observeAlarm(id: Long): Flow<AlarmEntity?> = dao.observeAlarmById(id)

    suspend fun getAlarm(id: Long): AlarmEntity? = dao.getAlarmById(id)

    suspend fun saveAlarm(alarm: AlarmEntity): Long {
        val normalizedPhrase = if (alarm.awakeProofEnabled && alarm.proofPhrase.isBlank()) {
            ProofPhraseGenerator.generate(alarm.label)
        } else {
            alarm.proofPhrase.trim()
        }

        val cleaned = alarm.copy(
            proofPhrase = normalizedPhrase,
            isPendingReRing = false,
            nextReRingAtMillis = null,
            reRingAttemptCount = 0,
            lastDismissedEpochDay = null
        )

        val finalAlarm = if (cleaned.id == 0L) {
            val newId = dao.insertAlarm(cleaned)
            cleaned.copy(id = newId)
        } else {
            dao.updateAlarm(cleaned)
            cleaned
        }

        scheduler.cancelAllForAlarm(finalAlarm.id)
        if (finalAlarm.isEnabled) {
            scheduler.scheduleMainAlarm(finalAlarm)
        }
        return finalAlarm.id
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(
            isEnabled = enabled,
            isPendingReRing = if (!enabled) false else alarm.isPendingReRing,
            nextReRingAtMillis = if (!enabled) null else alarm.nextReRingAtMillis,
            reRingAttemptCount = if (!enabled) 0 else alarm.reRingAttemptCount,
            lastDismissedEpochDay = if (enabled) null else alarm.lastDismissedEpochDay
        )
        dao.updateAlarm(updated)
        if (enabled) {
            scheduler.scheduleMainAlarm(updated)
        } else {
            scheduler.cancelAllForAlarm(updated.id)
        }
    }

    suspend fun deleteAlarm(alarmId: Long) {
        scheduler.cancelAllForAlarm(alarmId)
        dao.deleteAlarmById(alarmId)
    }

    suspend fun markSilencedAndScheduleReRing(alarmId: Long): Pair<AlarmEntity, Long>? {
        val alarm = dao.getAlarmById(alarmId) ?: return null
        val triggerAt = scheduler.scheduleReRing(alarm, alarm.reRingIntervalSeconds)
        val updated = alarm.copy(
            isPendingReRing = true,
            nextReRingAtMillis = triggerAt,
            reRingAttemptCount = alarm.reRingAttemptCount + 1
        )
        dao.updateAlarm(updated)
        return updated to triggerAt
    }

    suspend fun verifyAndDismissForToday(
        alarmId: Long,
        enteredText: String
    ): ProofVerificationResult {
        val alarm = dao.getAlarmById(alarmId) ?: return ProofVerificationResult.NotFound
        if (enteredText.isBlank()) {
            return ProofVerificationResult.Mismatch("Please enter or paste your Awake Proof text.")
        }
        if (!ProofPhraseGenerator.matches(enteredText, alarm.proofPhrase)) {
            return ProofVerificationResult.Mismatch("Text does not match your Awake Proof. Alarm stays active!")
        }

        completeDismissForToday(alarm)
        return ProofVerificationResult.Success(
            alarmLabel = alarm.label.ifBlank { "Alarm" },
            attempts = alarm.reRingAttemptCount
        )
    }

    suspend fun dismissNormalAlarm(alarmId: Long) {
        val alarm = dao.getAlarmById(alarmId) ?: return
        completeDismissForToday(alarm)
    }

    private suspend fun completeDismissForToday(alarm: AlarmEntity) {
        scheduler.cancelReRing(alarm.id)
        val today = AlarmScheduler.currentEpochDay()
        val keepEnabled = alarm.repeatDaysMask != 0
        val updated = alarm.copy(
            isEnabled = keepEnabled,
            isPendingReRing = false,
            nextReRingAtMillis = null,
            reRingAttemptCount = 0,
            lastDismissedEpochDay = today
        )
        dao.updateAlarm(updated)

        if (keepEnabled) {
            scheduler.scheduleMainAlarm(updated)
        } else {
            scheduler.cancelMainAlarm(updated.id)
        }

        val timeFormatted = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
        dao.insertWakeLog(
            WakeProofLogEntity(
                alarmId = alarm.id,
                alarmLabel = alarm.label.ifBlank { "Alarm" },
                scheduledTimeFormatted = timeFormatted,
                verifiedAtMillis = System.currentTimeMillis(),
                reRingAttempts = alarm.reRingAttemptCount,
                epochDay = today
            )
        )
    }

    suspend fun rescheduleAllActiveAlarms() {
        val active = dao.getActiveAlarms()
        val now = System.currentTimeMillis()
        for (alarm in active) {
            if (alarm.isEnabled) {
                scheduler.scheduleMainAlarm(alarm)
            }
            if (alarm.isPendingReRing) {
                val remainingMs = (alarm.nextReRingAtMillis ?: 0L) - now
                val delaySec = if (remainingMs > 5_000L) (remainingMs / 1000L).toInt() else 15
                scheduler.scheduleReRing(alarm, delaySec)
            }
        }
    }
}
