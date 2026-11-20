package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WakeProofApplication
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmService
import com.example.alarm.BuiltInAlarmTones
import com.example.alarm.ProofPhraseGenerator
import com.example.data.AlarmEntity
import com.example.data.ProofVerificationResult
import com.example.data.WakeProofLogEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmsUiState(
    val alarms: List<AlarmEntity> = emptyList(),
    val recentLogs: List<WakeProofLogEntity> = emptyList(),
    val use24Hour: Boolean = false,
    val nextAlarmTriggerMillis: Long? = null,
    val nextAlarmLabel: String? = null,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val wakeStreakDays: Int = 0
)

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WakeProofApplication
    private val repository = app.repository
    private val scheduler = app.scheduler
    private val preferences = app.preferences

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<AlarmsUiState> = combine(
        repository.alarms,
        repository.recentWakeLogs,
        preferences.use24HourFormat,
        tickerFlow
    ) { alarms, logs, use24h, now ->
        var earliestMillis: Long? = null
        var earliestLabel: String? = null
        for (alarm in alarms) {
            val pending = if (alarm.isPendingReRing) alarm.nextReRingAtMillis else null
            val scheduled = if (alarm.isEnabled) scheduler.calculateNextTriggerMillis(alarm, now) else null
            val candidate = listOfNotNull(pending, scheduled).minOrNull()
            if (candidate != null && (earliestMillis == null || candidate < earliestMillis)) {
                earliestMillis = candidate
                earliestLabel = alarm.label.ifBlank { "Alarm" }
            }
        }

        AlarmsUiState(
            alarms = alarms,
            recentLogs = logs,
            use24Hour = use24h,
            nextAlarmTriggerMillis = earliestMillis,
            nextAlarmLabel = earliestLabel,
            currentTimeMillis = now,
            wakeStreakDays = computeStreak(logs)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AlarmsUiState()
    )

    val savedWorldZones: StateFlow<Set<String>> = preferences.savedWorldZones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptySet()
        )

    val defaultReRingSeconds: StateFlow<Int> = preferences.defaultReRingSeconds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 60
        )

    private val _selectedAlarmForProofModal = MutableStateFlow<AlarmEntity?>(null)
    val selectedAlarmForProofModal: StateFlow<AlarmEntity?> = _selectedAlarmForProofModal

    fun openProofModal(alarm: AlarmEntity) {
        _selectedAlarmForProofModal.value = alarm
    }

    fun closeProofModal() {
        _selectedAlarmForProofModal.value = null
    }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        if (alarm.isPendingReRing && !enabled) {
            openProofModal(alarm)
            return
        }
        viewModelScope.launch {
            repository.toggleAlarm(alarm, enabled)
        }
    }

    fun saveAlarm(alarm: AlarmEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveAlarm(alarm)
            onSaved(savedId)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        if (alarm.isPendingReRing) {
            openProofModal(alarm)
            return
        }
        viewModelScope.launch {
            repository.deleteAlarm(alarm.id)
        }
    }

    fun createQuickFajrAlarm(onCreated: (AlarmEntity) -> Unit) {
        viewModelScope.launch {
            val phrase = ProofPhraseGenerator.generate("Fajr Namaz")
            val fajr = AlarmEntity(
                hour = 6,
                minute = 0,
                label = "Fajr Namaz",
                isEnabled = true,
                repeatDaysMask = AlarmEntity.MASK_EVERY_DAY,
                awakeProofEnabled = true,
                proofPhrase = phrase,
                reRingIntervalSeconds = defaultReRingSeconds.value,
                strictHidePhrase = true,
                ringtoneUri = BuiltInAlarmTones.URI_DAWN_CHIME,
                ringtoneTitle = "Dawn Chimes",
                vibrate = true,
                gradualVolume = true
            )
            val id = repository.saveAlarm(fajr)
            onCreated(fajr.copy(id = id))
        }
    }

    fun testRingAlarm(alarmId: Long) {
        val intent = Intent(app, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TEST_RING
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        app.sendBroadcast(intent)
    }

    fun verifyProofInApp(
        alarmId: Long,
        enteredText: String,
        onResult: (ProofVerificationResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.verifyAndDismissForToday(alarmId, enteredText)
            if (res is ProofVerificationResult.Success) {
                val stopIntent = Intent(app, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_DISMISS_COMPLETE
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                }
                app.startService(stopIntent)
                closeProofModal()
            }
            onResult(res)
        }
    }

    fun setUse24Hour(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setUse24HourFormat(enabled)
        }
    }

    fun setDefaultReRingSeconds(seconds: Int) {
        viewModelScope.launch {
            preferences.setDefaultReRingSeconds(seconds)
        }
    }

    fun addWorldZone(zoneId: String) {
        viewModelScope.launch { preferences.addWorldZone(zoneId) }
    }

    fun removeWorldZone(zoneId: String) {
        viewModelScope.launch { preferences.removeWorldZone(zoneId) }
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? = repository.getAlarm(id)

    private fun computeStreak(logs: List<WakeProofLogEntity>): Int {
        if (logs.isEmpty()) return 0
        val distinctDays = logs.map { it.epochDay }.distinct().sortedDescending()
        val today = AlarmScheduler.currentEpochDay()
        if (distinctDays.first() < today - 1) return 0
        var streak = 1
        for (i in 0 until distinctDays.size - 1) {
            if (distinctDays[i] - distinctDays[i + 1] == 1L) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
