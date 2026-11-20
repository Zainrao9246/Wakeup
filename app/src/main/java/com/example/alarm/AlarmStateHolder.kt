package com.example.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveRingState(
    val alarmId: Long,
    val label: String,
    val hour: Int,
    val minute: Int,
    val awakeProofEnabled: Boolean,
    val proofPhrase: String,
    val strictHidePhrase: Boolean,
    val reRingIntervalSeconds: Int,
    val reRingAttemptCount: Int,
    val isCurrentlyRinging: Boolean,
    val nextReRingAtMillis: Long? = null,
    val silenceReason: String? = null
)

object AlarmStateHolder {
    private val _activeState = MutableStateFlow<ActiveRingState?>(null)
    val activeState: StateFlow<ActiveRingState?> = _activeState.asStateFlow()

    fun updateState(state: ActiveRingState?) {
        _activeState.value = state
    }
}
