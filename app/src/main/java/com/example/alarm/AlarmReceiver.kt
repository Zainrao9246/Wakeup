package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        when (intent.action) {
            ACTION_TRIGGER_ALARM, ACTION_RE_RING, ACTION_TEST_RING -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START_RINGING
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                    putExtra(
                        AlarmService.EXTRA_IS_RE_RING,
                        intent.action == ACTION_RE_RING
                    )
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }

            ACTION_SILENCE_1_MIN -> {
                val reason = intent.getStringExtra(EXTRA_SILENCE_REASON)
                    ?: "Notification dismissed — Re-ringing in 1 min"
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_SILENCE_1_MIN
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmService.EXTRA_SILENCE_REASON, reason)
                }
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.alarm.ACTION_TRIGGER_ALARM"
        const val ACTION_RE_RING = "com.example.alarm.ACTION_RE_RING"
        const val ACTION_TEST_RING = "com.example.alarm.ACTION_TEST_RING"
        const val ACTION_SILENCE_1_MIN = "com.example.alarm.ACTION_SILENCE_1_MIN"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_SILENCE_REASON = "extra_silence_reason"
    }
}
