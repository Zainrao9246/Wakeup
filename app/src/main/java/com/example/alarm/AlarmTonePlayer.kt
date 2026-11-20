package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

data class BuiltInTone(
    val uri: String,
    val title: String,
    val subtitle: String
)

object BuiltInAlarmTones {
    const val URI_DAWN_CHIME = "tone://dawn_chime"
    const val URI_FAJR_ARPEGGIO = "tone://fajr_arpeggio"
    const val URI_MORNING_HARP = "tone://morning_harp"
    const val URI_DIGITAL_PULSE = "tone://digital_pulse"
    const val URI_HEAVY_SIREN = "tone://heavy_siren"

    val allTones = listOf(
        BuiltInTone(
            uri = URI_DAWN_CHIME,
            title = "Dawn Chimes",
            subtitle = "Gentle ascending bells for Fajr"
        ),
        BuiltInTone(
            uri = URI_FAJR_ARPEGGIO,
            title = "Serene Horizon",
            subtitle = "Warm resonant dawn chord sequence"
        ),
        BuiltInTone(
            uri = URI_MORNING_HARP,
            title = "Morning Harp",
            subtitle = "Soft melodic plucking pattern"
        ),
        BuiltInTone(
            uri = URI_DIGITAL_PULSE,
            title = "Crisp Digital Pulse",
            subtitle = "Sharp classic alarm beeps"
        ),
        BuiltInTone(
            uri = URI_HEAVY_SIREN,
            title = "Heavy Sleeper Alert",
            subtitle = "High-contrast dual-tone wake siren"
        )
    )

    fun isBuiltIn(uri: String?): Boolean = uri?.startsWith("tone://") == true

    fun findTitle(uri: String?): String? = allTones.firstOrNull { it.uri == uri }?.title
}

class AlarmTonePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private var fadeJob: Job? = null

    fun start(
        scope: CoroutineScope,
        toneUri: String?,
        gradualVolume: Boolean
    ) {
        stop()

        if (BuiltInAlarmTones.isBuiltIn(toneUri)) {
            startSynthesizedTone(scope, toneUri!!, gradualVolume)
        } else {
            startMediaPlayerTone(scope, toneUri, gradualVolume)
        }
    }

    fun stop() {
        fadeJob?.cancel()
        fadeJob = null
        synthJob?.cancel()
        synthJob = null

        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            }
        }
        mediaPlayer = null

        audioTrack?.let { track ->
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
                track.release()
            }
        }
        audioTrack = null
    }

    private fun startMediaPlayerTone(
        scope: CoroutineScope,
        toneUri: String?,
        gradualVolume: Boolean
    ) {
        val resolvedUri = toneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (resolvedUri == null) {
            startSynthesizedTone(scope, BuiltInAlarmTones.URI_DAWN_CHIME, gradualVolume)
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, resolvedUri)
                isLooping = true
                prepare()
                val startVol = if (gradualVolume) 0.15f else 1.0f
                setVolume(startVol, startVol)
                start()
            }
            mediaPlayer = mp

            if (gradualVolume) {
                fadeJob = scope.launch {
                    val steps = 12
                    for (step in 1..steps) {
                        delay(800L)
                        val vol = 0.15f + (0.85f * step / steps)
                        runCatching { mediaPlayer?.setVolume(vol, vol) }
                    }
                }
            }
        } catch (_: Exception) {
            startSynthesizedTone(scope, BuiltInAlarmTones.URI_DAWN_CHIME, gradualVolume)
        }
    }

    private fun startSynthesizedTone(
        scope: CoroutineScope,
        toneUri: String,
        gradualVolume: Boolean
    ) {
        val sampleRate = 22050
        val pcmLoop = generatePatternPcm(toneUri, sampleRate)
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val initialGain = if (gradualVolume) 0.20f else 1.0f
        track.setVolume(initialGain)
        track.play()
        audioTrack = track

        if (gradualVolume) {
            fadeJob = scope.launch {
                val steps = 12
                for (step in 1..steps) {
                    delay(800L)
                    val vol = 0.20f + (0.80f * step / steps)
                    runCatching { audioTrack?.setVolume(vol) }
                }
            }
        }

        synthJob = scope.launch(Dispatchers.IO) {
            val chunkSize = 2048
            var offset = 0
            while (isActive) {
                val remaining = pcmLoop.size - offset
                val count = minOf(chunkSize, remaining)
                val written = runCatching {
                    track.write(pcmLoop, offset, count)
                }.getOrDefault(-1)
                if (written < 0) break
                offset = (offset + count) % pcmLoop.size
            }
        }
    }

    private fun generatePatternPcm(toneUri: String, sampleRate: Int): ShortArray {
        return when (toneUri) {
            BuiltInAlarmTones.URI_DAWN_CHIME -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(523.25, 0.35, 0.55, harmonic = true),
                    NoteSpec(659.25, 0.35, 0.55, harmonic = true),
                    NoteSpec(783.99, 0.45, 0.65, harmonic = true),
                    NoteSpec(1046.50, 0.60, 0.70, harmonic = true),
                    NoteSpec(0.0, 0.65, 0.0, harmonic = false)
                )
            )

            BuiltInAlarmTones.URI_FAJR_ARPEGGIO -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(440.0, 0.30, 0.55, harmonic = true),
                    NoteSpec(554.37, 0.30, 0.55, harmonic = true),
                    NoteSpec(659.25, 0.30, 0.60, harmonic = true),
                    NoteSpec(880.0, 0.55, 0.65, harmonic = true),
                    NoteSpec(659.25, 0.30, 0.50, harmonic = true),
                    NoteSpec(0.0, 0.75, 0.0, harmonic = false)
                )
            )

            BuiltInAlarmTones.URI_MORNING_HARP -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(587.33, 0.25, 0.55, harmonic = true),
                    NoteSpec(739.99, 0.25, 0.55, harmonic = true),
                    NoteSpec(880.00, 0.25, 0.60, harmonic = true),
                    NoteSpec(1174.66, 0.45, 0.65, harmonic = true),
                    NoteSpec(880.00, 0.25, 0.50, harmonic = true),
                    NoteSpec(739.99, 0.25, 0.50, harmonic = true),
                    NoteSpec(0.0, 0.60, 0.0, harmonic = false)
                )
            )

            BuiltInAlarmTones.URI_DIGITAL_PULSE -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(1000.0, 0.14, 0.75, harmonic = false),
                    NoteSpec(0.0, 0.10, 0.0, harmonic = false),
                    NoteSpec(1000.0, 0.14, 0.75, harmonic = false),
                    NoteSpec(0.0, 0.10, 0.0, harmonic = false),
                    NoteSpec(1000.0, 0.14, 0.75, harmonic = false),
                    NoteSpec(0.0, 0.65, 0.0, harmonic = false)
                )
            )

            BuiltInAlarmTones.URI_HEAVY_SIREN -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(880.0, 0.22, 0.85, harmonic = false),
                    NoteSpec(1174.66, 0.22, 0.85, harmonic = false),
                    NoteSpec(880.0, 0.22, 0.85, harmonic = false),
                    NoteSpec(1174.66, 0.22, 0.85, harmonic = false),
                    NoteSpec(0.0, 0.32, 0.0, harmonic = false)
                )
            )

            else -> renderSequence(
                sampleRate = sampleRate,
                notes = listOf(
                    NoteSpec(523.25, 0.35, 0.60, harmonic = true),
                    NoteSpec(659.25, 0.35, 0.60, harmonic = true),
                    NoteSpec(783.99, 0.45, 0.65, harmonic = true),
                    NoteSpec(0.0, 0.65, 0.0, harmonic = false)
                )
            )
        }
    }

    private data class NoteSpec(
        val freqHz: Double,
        val durationSec: Double,
        val amplitude: Double,
        val harmonic: Boolean
    )

    private fun renderSequence(sampleRate: Int, notes: List<NoteSpec>): ShortArray {
        val totalSamples = notes.sumOf { (it.durationSec * sampleRate).toInt() }
        val buffer = ShortArray(totalSamples)
        var cursor = 0

        for (note in notes) {
            val count = (note.durationSec * sampleRate).toInt()
            val attack = (0.02 * sampleRate).toInt().coerceAtLeast(1)
            val release = (0.08 * sampleRate).toInt().coerceAtLeast(1)

            for (i in 0 until count) {
                val sample = if (note.freqHz <= 0.0) {
                    0.0
                } else {
                    val t = i.toDouble() / sampleRate
                    val env = when {
                        i < attack -> i.toDouble() / attack
                        i > count - release -> ((count - i).toDouble() / release).coerceAtLeast(0.0)
                        else -> if (note.harmonic) {
                            kotlin.math.exp(-2.5 * t)
                        } else {
                            1.0
                        }
                    }
                    val base = sin(2.0 * PI * note.freqHz * t)
                    val overtone = if (note.harmonic) {
                        0.32 * sin(2.0 * PI * (note.freqHz * 2.0) * t) +
                            0.12 * sin(2.0 * PI * (note.freqHz * 3.0) * t)
                    } else {
                        0.20 * sin(2.0 * PI * (note.freqHz * 2.0) * t)
                    }
                    (base + overtone) * 0.72 * note.amplitude * env
                }
                buffer[cursor + i] = (sample * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            cursor += count
        }
        return buffer
    }
}
