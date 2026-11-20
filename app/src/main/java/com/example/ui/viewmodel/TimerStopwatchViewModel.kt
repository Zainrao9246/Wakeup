package com.example.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    val totalMillis: Long = 5 * 60 * 1000L,
    val remainingMillis: Long = 5 * 60 * 1000L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

data class LapEntry(
    val number: Int,
    val lapMillis: Long,
    val totalMillis: Long
)

data class StopwatchUiState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<LapEntry> = emptyList()
)

class TimerStopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private val _stopwatchState = MutableStateFlow(StopwatchUiState())
    val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()

    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null
    private var stopwatchLastTick = 0L
    private var lastLapTotalMillis = 0L
    private var activeTimerRingtone: Ringtone? = null

    fun setTimerDuration(minutes: Int, seconds: Int) {
        stopTimerSound()
        timerJob?.cancel()
        val millis = ((minutes * 60L) + seconds).coerceAtLeast(1L) * 1000L
        _timerState.value = TimerUiState(
            totalMillis = millis,
            remainingMillis = millis,
            isRunning = false,
            isFinished = false
        )
    }

    fun startOrPauseTimer() {
        val current = _timerState.value
        if (current.isFinished) {
            resetTimer()
            return
        }
        if (current.isRunning) {
            timerJob?.cancel()
            _timerState.update { it.copy(isRunning = false) }
        } else {
            _timerState.update { it.copy(isRunning = true, isFinished = false) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                var last = SystemClock.elapsedRealtime()
                while (true) {
                    delay(50L)
                    val now = SystemClock.elapsedRealtime()
                    val delta = now - last
                    last = now
                    val rem = (_timerState.value.remainingMillis - delta).coerceAtLeast(0L)
                    if (rem == 0L) {
                        _timerState.update {
                            it.copy(remainingMillis = 0L, isRunning = false, isFinished = true)
                        }
                        playTimerDoneSound()
                        break
                    } else {
                        _timerState.update { it.copy(remainingMillis = rem) }
                    }
                }
            }
        }
    }

    fun addOneMinuteToTimer() {
        stopTimerSound()
        _timerState.update { state ->
            val added = 60_000L
            state.copy(
                totalMillis = state.totalMillis + added,
                remainingMillis = state.remainingMillis + added,
                isFinished = false
            )
        }
    }

    fun resetTimer() {
        stopTimerSound()
        timerJob?.cancel()
        _timerState.update {
            it.copy(remainingMillis = it.totalMillis, isRunning = false, isFinished = false)
        }
    }

    fun startOrPauseStopwatch() {
        val current = _stopwatchState.value
        if (current.isRunning) {
            stopwatchJob?.cancel()
            _stopwatchState.update { it.copy(isRunning = false) }
        } else {
            _stopwatchState.update { it.copy(isRunning = true) }
            stopwatchJob?.cancel()
            stopwatchLastTick = SystemClock.elapsedRealtime()
            stopwatchJob = viewModelScope.launch {
                while (true) {
                    delay(20L)
                    val now = SystemClock.elapsedRealtime()
                    val delta = now - stopwatchLastTick
                    stopwatchLastTick = now
                    _stopwatchState.update { it.copy(elapsedMillis = it.elapsedMillis + delta) }
                }
            }
        }
    }

    fun recordLap() {
        val current = _stopwatchState.value
        if (!current.isRunning && current.elapsedMillis == 0L) return
        val total = current.elapsedMillis
        val lapTime = (total - lastLapTotalMillis).coerceAtLeast(0L)
        lastLapTotalMillis = total
        val entry = LapEntry(
            number = current.laps.size + 1,
            lapMillis = lapTime,
            totalMillis = total
        )
        _stopwatchState.update { it.copy(laps = listOf(entry) + it.laps) }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        lastLapTotalMillis = 0L
        _stopwatchState.value = StopwatchUiState()
    }

    private fun playTimerDoneSound() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val rt = RingtoneManager.getRingtone(getApplication(), uri)
            rt?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            rt?.play()
            activeTimerRingtone = rt
        }
    }

    fun stopTimerSound() {
        runCatching { activeTimerRingtone?.stop() }
        activeTimerRingtone = null
    }

    override fun onCleared() {
        stopTimerSound()
        timerJob?.cancel()
        stopwatchJob?.cancel()
        super.onCleared()
    }
}
