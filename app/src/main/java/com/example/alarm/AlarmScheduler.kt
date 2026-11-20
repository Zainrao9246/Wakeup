package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.AlarmEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleMainAlarm(alarm: AlarmEntity): Long? {
        cancelMainAlarm(alarm.id)
        if (!alarm.isEnabled) return null

        val triggerAtMillis = calculateNextTriggerMillis(alarm) ?: return null
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrClock(triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    fun scheduleReRing(alarm: AlarmEntity, delaySeconds: Int = alarm.reRingIntervalSeconds): Long {
        cancelReRing(alarm.id)
        val safeDelay = delaySeconds.coerceAtLeast(15)
        val triggerAtMillis = System.currentTimeMillis() + safeDelay * 1000L

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RE_RING
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reRingRequestCode(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrClock(triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    fun cancelMainAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pi = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun cancelReRing(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_RE_RING
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reRingRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun cancelAllForAlarm(alarmId: Long) {
        cancelMainAlarm(alarmId)
        cancelReRing(alarmId)
    }

    private fun scheduleExactOrClock(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val showAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExactAlarms()) {
            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun calculateNextTriggerMillis(
        alarm: AlarmEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!alarm.isEnabled) return null

        val todayEpoch = currentEpochDay(nowMillis)
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDaysMask == 0) {
            if (cal.timeInMillis <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }
            if (candidate.timeInMillis <= nowMillis) continue
            if (offset == 0 && alarm.lastDismissedEpochDay == todayEpoch) continue

            val monZeroIndex = AlarmEntity.calendarDayToMonZeroIndex(candidate.get(Calendar.DAY_OF_WEEK))
            if (alarm.isDaySelected(monZeroIndex)) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    companion object {
        fun reRingRequestCode(alarmId: Long): Int = (100_000L + alarmId).toInt()

        fun currentEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val offsetMillis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
            return TimeUnit.MILLISECONDS.toDays(nowMillis + offsetMillis)
        }
    }
}
