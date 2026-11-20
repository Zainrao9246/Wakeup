package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.WakeProofApplication
import com.example.data.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tonePlayer: AlarmTonePlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, currentAlarmId)

        when (action) {
            ACTION_START_RINGING -> {
                val isReRing = intent.getBooleanExtra(EXTRA_IS_RE_RING, false)
                if (alarmId != -1L) {
                    startRinging(alarmId, isReRing)
                }
            }

            ACTION_SILENCE_1_MIN -> {
                val reason = intent.getStringExtra(EXTRA_SILENCE_REASON)
                    ?: "Silenced — Re-ringing in 1 min"
                silenceAndScheduleReRing(alarmId, reason)
            }

            ACTION_PAUSE_FOR_PROOF -> {
                pauseAudioWhileEnteringProof(alarmId)
            }

            ACTION_DISMISS_COMPLETE -> {
                dismissCompletely(alarmId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Long, isReRing: Boolean) {
        currentAlarmId = alarmId
        val app = applicationContext as WakeProofApplication
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ARMED_NOTIFICATION_ID)

        // Post immediate foreground notification to satisfy startForegroundService contract within 5s
        val placeholderIntent = buildRingActivityIntent(alarmId, openProofDirectly = false)
        val placeholderPi = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            placeholderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val initialNotification = NotificationCompat.Builder(this, CHANNEL_RINGING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Alarm ringing...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(placeholderPi, true)
            .build()
        startForeground(RINGING_NOTIFICATION_ID, initialNotification)

        serviceScope.launch {
            val alarm = app.repository.getAlarm(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }

            acquireWakeLock()
            startAlarmAudio(alarm)
            if (alarm.vibrate) startVibration()

            val timeText = formatTimeLabel(alarm.hour, alarm.minute)
            val title = if (isReRing) {
                "Re-Ring #${alarm.reRingAttemptCount}: ${alarm.label.ifBlank { "Alarm" }}"
            } else {
                "${alarm.label.ifBlank { "Alarm" }} • $timeText"
            }
            val body = if (alarm.awakeProofEnabled) {
                "Enter Awake Proof text to turn off for today. Silencing re-rings in ${alarm.reRingIntervalSeconds}s."
            } else {
                "Tap to dismiss alarm"
            }

            val fullScreenPi = PendingIntent.getActivity(
                this@AlarmService,
                alarm.id.toInt(),
                buildRingActivityIntent(alarm.id, openProofDirectly = false),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val proofPi = PendingIntent.getActivity(
                this@AlarmService,
                (200_000L + alarm.id).toInt(),
                buildRingActivityIntent(alarm.id, openProofDirectly = true),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val silenceIntent = Intent(this@AlarmService, AlarmReceiver::class.java).apply {
                this.action = AlarmReceiver.ACTION_SILENCE_1_MIN
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                putExtra(
                    AlarmReceiver.EXTRA_SILENCE_REASON,
                    "Notification silenced — Re-ringing in ${alarm.reRingIntervalSeconds}s"
                )
            }
            val silencePi = PendingIntent.getBroadcast(
                this@AlarmService,
                (300_000L + alarm.id).toInt(),
                silenceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this@AlarmService, CHANNEL_RINGING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPi, true)
                .setContentIntent(fullScreenPi)

            if (alarm.awakeProofEnabled) {
                builder.setDeleteIntent(silencePi)
                builder.addAction(0, "Enter Proof", proofPi)
                builder.addAction(0, "Silence (${alarm.reRingIntervalSeconds}s)", silencePi)
            } else {
                builder.addAction(0, "Open", fullScreenPi)
            }

            notificationManager.notify(RINGING_NOTIFICATION_ID, builder.build())

            AlarmStateHolder.updateState(
                ActiveRingState(
                    alarmId = alarm.id,
                    label = alarm.label.ifBlank { "Alarm" },
                    hour = alarm.hour,
                    minute = alarm.minute,
                    awakeProofEnabled = alarm.awakeProofEnabled,
                    proofPhrase = alarm.proofPhrase,
                    strictHidePhrase = alarm.strictHidePhrase,
                    reRingIntervalSeconds = alarm.reRingIntervalSeconds,
                    reRingAttemptCount = alarm.reRingAttemptCount,
                    isCurrentlyRinging = true,
                    nextReRingAtMillis = null,
                    silenceReason = null
                )
            )

            runCatching {
                startActivity(buildRingActivityIntent(alarm.id, openProofDirectly = false))
            }
        }
    }

    private fun pauseAudioWhileEnteringProof(alarmId: Long) {
        stopAudioAndVibration()
        serviceScope.launch {
            val app = applicationContext as WakeProofApplication
            val alarm = app.repository.getAlarm(alarmId) ?: return@launch
            if (alarm.awakeProofEnabled) {
                val result = app.repository.markSilencedAndScheduleReRing(alarmId)
                if (result != null) {
                    val (updated, triggerAt) = result
                    AlarmStateHolder.updateState(
                        ActiveRingState(
                            alarmId = updated.id,
                            label = updated.label.ifBlank { "Alarm" },
                            hour = updated.hour,
                            minute = updated.minute,
                            awakeProofEnabled = updated.awakeProofEnabled,
                            proofPhrase = updated.proofPhrase,
                            strictHidePhrase = updated.strictHidePhrase,
                            reRingIntervalSeconds = updated.reRingIntervalSeconds,
                            reRingAttemptCount = updated.reRingAttemptCount,
                            isCurrentlyRinging = false,
                            nextReRingAtMillis = triggerAt,
                            silenceReason = "Audio paused while entering Awake Proof — Re-rings if not submitted"
                        )
                    )
                }
            }
        }
    }

    private fun silenceAndScheduleReRing(alarmId: Long, reason: String) {
        stopAudioAndVibration()
        releaseWakeLock()

        serviceScope.launch {
            val app = applicationContext as WakeProofApplication
            val result = app.repository.markSilencedAndScheduleReRing(alarmId)
            if (result != null) {
                val (updated, triggerAt) = result
                AlarmStateHolder.updateState(
                    ActiveRingState(
                        alarmId = updated.id,
                        label = updated.label.ifBlank { "Alarm" },
                        hour = updated.hour,
                        minute = updated.minute,
                        awakeProofEnabled = updated.awakeProofEnabled,
                        proofPhrase = updated.proofPhrase,
                        strictHidePhrase = updated.strictHidePhrase,
                        reRingIntervalSeconds = updated.reRingIntervalSeconds,
                        reRingAttemptCount = updated.reRingAttemptCount,
                        isCurrentlyRinging = false,
                        nextReRingAtMillis = triggerAt,
                        silenceReason = reason
                    )
                )
                postArmedCountdownNotification(updated)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun dismissCompletely(alarmId: Long) {
        stopAudioAndVibration()
        releaseWakeLock()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(RINGING_NOTIFICATION_ID)
        notificationManager.cancel(ARMED_NOTIFICATION_ID)
        AlarmStateHolder.updateState(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun postArmedCountdownNotification(alarm: AlarmEntity) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openProofPi = PendingIntent.getActivity(
            this,
            (200_000L + alarm.id).toInt(),
            buildRingActivityIntent(alarm.id, openProofDirectly = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ARMED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WakeProof Active: ${alarm.label.ifBlank { "Alarm" }}")
            .setContentText("Re-ringing in ${alarm.reRingIntervalSeconds}s. Tap to submit Awake Proof.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(openProofPi)
            .addAction(0, "Enter Awake Proof", openProofPi)
            .build()

        notificationManager.notify(ARMED_NOTIFICATION_ID, notification)
    }

    private fun startAlarmAudio(alarm: AlarmEntity) {
        stopAudioOnly()
        val player = AlarmTonePlayer(this)
        tonePlayer = player
        player.start(
            scope = serviceScope,
            toneUri = alarm.ringtoneUri,
            gradualVolume = alarm.gradualVolume
        )
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib
        if (!vib.hasVibrator()) return

        val pattern = longArrayOf(0L, 700L, 450L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, 0)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            vib.vibrate(pattern, 0)
        }
    }

    private fun stopAudioOnly() {
        tonePlayer?.stop()
        tonePlayer = null
    }

    private fun stopAudioAndVibration() {
        stopAudioOnly()
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeProof:AlarmWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { wl ->
            runCatching { if (wl.isHeld) wl.release() }
        }
        wakeLock = null
    }

    private fun buildRingActivityIntent(alarmId: Long, openProofDirectly: Boolean): Intent {
        return Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_OPEN_PROOF, openProofDirectly)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ringChannel = NotificationChannel(
                CHANNEL_RINGING,
                "Ringing Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm alerts"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val armedChannel = NotificationChannel(
                CHANNEL_ARMED,
                "Active Re-Ring Countdown",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows 1-minute re-ring countdown until Awake Proof is submitted"
            }
            nm.createNotificationChannel(ringChannel)
            nm.createNotificationChannel(armedChannel)
        }
    }

    private fun formatTimeLabel(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }

    override fun onDestroy() {
        stopAudioAndVibration()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_RINGING = "wakeproof_alarm_ringing"
        const val CHANNEL_ARMED = "wakeproof_alarm_armed"
        const val RINGING_NOTIFICATION_ID = 7001
        const val ARMED_NOTIFICATION_ID = 7002

        const val ACTION_START_RINGING = "com.example.alarm.SERVICE_START"
        const val ACTION_SILENCE_1_MIN = "com.example.alarm.SERVICE_SILENCE_1_MIN"
        const val ACTION_PAUSE_FOR_PROOF = "com.example.alarm.SERVICE_PAUSE_FOR_PROOF"
        const val ACTION_DISMISS_COMPLETE = "com.example.alarm.SERVICE_DISMISS_COMPLETE"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_RE_RING = "extra_is_re_ring"
        const val EXTRA_SILENCE_REASON = "extra_silence_reason"
        const val EXTRA_OPEN_PROOF = "extra_open_proof"
    }
}
