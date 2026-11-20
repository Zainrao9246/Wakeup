package com.example.alarm

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.WakeProofApplication
import com.example.data.AlarmEntity
import com.example.data.ProofVerificationResult
import com.example.ui.theme.DawnAmber
import com.example.ui.theme.DawnAmberBright
import com.example.ui.theme.DawnCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.ui.theme.TwilightTeal
import com.example.ui.theme.WakeProofTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AlarmRingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var openProofInitially by mutableStateOf(false)
    private var loadedAlarm by mutableStateOf<AlarmEntity?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()
        extractExtras(intent)
        loadAlarmFromDb()

        setContent {
            WakeProofTheme(darkTheme = true) {
                val activeState by AlarmStateHolder.activeState.collectAsStateWithLifecycle()
                AlarmRingScreen(
                    alarmId = alarmId,
                    fallbackAlarm = loadedAlarm,
                    activeState = activeState,
                    openProofInitially = openProofInitially,
                    onSilence1Min = { reason ->
                        triggerSilence1Min(reason)
                    },
                    onPauseAudioForProof = {
                        sendServiceAction(AlarmService.ACTION_PAUSE_FOR_PROOF)
                    },
                    onDismissNormal = {
                        lifecycleScope.launch {
                            val app = applicationContext as WakeProofApplication
                            app.repository.dismissNormalAlarm(alarmId)
                            sendServiceAction(AlarmService.ACTION_DISMISS_COMPLETE)
                            finish()
                        }
                    },
                    onSubmitProof = { text, onResult ->
                        lifecycleScope.launch {
                            val app = applicationContext as WakeProofApplication
                            when (val res = app.repository.verifyAndDismissForToday(alarmId, text)) {
                                is ProofVerificationResult.Success -> {
                                    sendServiceAction(AlarmService.ACTION_DISMISS_COMPLETE)
                                    onResult(true, null)
                                }
                                is ProofVerificationResult.Mismatch -> {
                                    onResult(false, res.message)
                                }
                                ProofVerificationResult.NotFound -> {
                                    sendServiceAction(AlarmService.ACTION_DISMISS_COMPLETE)
                                    onResult(true, null)
                                }
                            }
                        }
                    },
                    onCloseActivity = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractExtras(intent)
        loadAlarmFromDb()
    }

    private fun extractExtras(intent: Intent?) {
        alarmId = intent?.getLongExtra(AlarmService.EXTRA_ALARM_ID, -1L) ?: -1L
        openProofInitially = intent?.getBooleanExtra(AlarmService.EXTRA_OPEN_PROOF, false) ?: false
    }

    private fun loadAlarmFromDb() {
        if (alarmId == -1L) return
        lifecycleScope.launch {
            val app = applicationContext as WakeProofApplication
            loadedAlarm = app.repository.getAlarm(alarmId)
        }
    }

    private fun triggerSilence1Min(reason: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SILENCE_1_MIN
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmService.EXTRA_SILENCE_REASON, reason)
        }
        startService(intent)
    }

    private fun sendServiceAction(actionName: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = actionName
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
        }
        startService(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            val state = AlarmStateHolder.activeState.value
            val proofEnabled = state?.awakeProofEnabled ?: loadedAlarm?.awakeProofEnabled ?: true
            val reRingSec = state?.reRingIntervalSeconds ?: loadedAlarm?.reRingIntervalSeconds ?: 60
            if (proofEnabled) {
                triggerSilence1Min("Volume button pressed — Re-ringing in ${reRingSec}s!")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("DEPRECATION")
    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private enum class RingStage {
    RINGING_OR_SILENCED,
    PROOF_INPUT,
    VERIFIED_DONE
}

@Composable
private fun AlarmRingScreen(
    alarmId: Long,
    fallbackAlarm: AlarmEntity?,
    activeState: ActiveRingState?,
    openProofInitially: Boolean,
    onSilence1Min: (String) -> Unit,
    onPauseAudioForProof: () -> Unit,
    onDismissNormal: () -> Unit,
    onSubmitProof: (String, (Boolean, String?) -> Unit) -> Unit,
    onCloseActivity: () -> Unit
) {
    val label = activeState?.label ?: fallbackAlarm?.label?.ifBlank { "Alarm" } ?: "Fajr Namaz"
    val awakeProofEnabled = activeState?.awakeProofEnabled ?: fallbackAlarm?.awakeProofEnabled ?: true
    val proofPhrase = activeState?.proofPhrase ?: fallbackAlarm?.proofPhrase ?: ""
    val strictHidePhrase = activeState?.strictHidePhrase ?: fallbackAlarm?.strictHidePhrase ?: true
    val reRingSec = activeState?.reRingIntervalSeconds ?: fallbackAlarm?.reRingIntervalSeconds ?: 60
    val attempts = activeState?.reRingAttemptCount ?: fallbackAlarm?.reRingAttemptCount ?: 0
    val isRinging = activeState?.isCurrentlyRinging ?: (fallbackAlarm != null && !fallbackAlarm.isPendingReRing)
    val nextReRingAt = activeState?.nextReRingAtMillis ?: fallbackAlarm?.nextReRingAtMillis
    val silenceReason = activeState?.silenceReason

    var stage by remember(openProofInitially) {
        mutableStateOf(if (openProofInitially && awakeProofEnabled) RingStage.PROOF_INPUT else RingStage.RINGING_OR_SILENCED)
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
    }

    val currentTimeText = remember(nowMillis) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val currentAmPmText = remember(nowMillis) {
        SimpleDateFormat("a", Locale.getDefault()).format(Date(nowMillis))
    }
    val currentDateText = remember(nowMillis) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }

    val remainingSeconds = remember(nowMillis, nextReRingAt) {
        if (nextReRingAt == null) 0
        else ((nextReRingAt - nowMillis).coerceAtLeast(0L) / 1000L).toInt()
    }

    BackHandler(enabled = awakeProofEnabled && stage != RingStage.VERIFIED_DONE) {
        if (stage == RingStage.PROOF_INPUT) {
            stage = RingStage.RINGING_OR_SILENCED
        } else if (!isRinging && nextReRingAt != null) {
            onCloseActivity()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060A15),
                        MidnightBg,
                        Color(0xFF1A140B)
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = stage,
            label = "ring_stage_transition",
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) { currentStage ->
            when (currentStage) {
                RingStage.RINGING_OR_SILENCED -> {
                    RingingOrCountdownContent(
                        label = label,
                        timeText = currentTimeText,
                        amPmText = currentAmPmText,
                        dateText = currentDateText,
                        awakeProofEnabled = awakeProofEnabled,
                        reRingSec = reRingSec,
                        attempts = attempts,
                        isCurrentlyRinging = isRinging,
                        remainingSeconds = remainingSeconds,
                        silenceReason = silenceReason,
                        onSwipeSilence = {
                            if (awakeProofEnabled) {
                                onSilence1Min("Swiped to silence — Re-ringing in ${reRingSec}s")
                            } else {
                                onDismissNormal()
                            }
                        },
                        onEnterProofClick = {
                            if (isRinging) onPauseAudioForProof()
                            stage = RingStage.PROOF_INPUT
                        },
                        onDismissNormal = onDismissNormal,
                        onMinimizeWhileArmed = onCloseActivity
                    )
                }

                RingStage.PROOF_INPUT -> {
                    AwakeProofInputContent(
                        label = label,
                        proofPhrase = proofPhrase,
                        strictHidePhrase = strictHidePhrase,
                        attempts = attempts,
                        remainingSeconds = remainingSeconds,
                        onBack = { stage = RingStage.RINGING_OR_SILENCED },
                        onSubmit = { entered, onValidation ->
                            onSubmitProof(entered) { ok, err ->
                                if (ok) {
                                    stage = RingStage.VERIFIED_DONE
                                } else {
                                    onValidation(err)
                                }
                            }
                        }
                    )
                }

                RingStage.VERIFIED_DONE -> {
                    VerifiedSuccessContent(
                        label = label,
                        timeText = "$currentTimeText $currentAmPmText",
                        attempts = attempts,
                        onDone = onCloseActivity
                    )
                }
            }
        }
    }
}

@Composable
private fun RingingOrCountdownContent(
    label: String,
    timeText: String,
    amPmText: String,
    dateText: String,
    awakeProofEnabled: Boolean,
    reRingSec: Int,
    attempts: Int,
    isCurrentlyRinging: Boolean,
    remainingSeconds: Int,
    silenceReason: String?,
    onSwipeSilence: () -> Unit,
    onEnterProofClick: () -> Unit,
    onDismissNormal: () -> Unit,
    onMinimizeWhileArmed: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status pill
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = if (isCurrentlyRinging) DawnAmber.copy(alpha = 0.16f) else DawnCoral.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(
                    1.dp,
                    if (isCurrentlyRinging) DawnAmber.copy(alpha = 0.5f) else DawnCoral.copy(alpha = 0.5f),
                    RoundedCornerShape(50)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCurrentlyRinging) Icons.Rounded.NotificationsActive else Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (isCurrentlyRinging) DawnAmberBright else DawnCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            isCurrentlyRinging && attempts > 0 -> "RE-RING ATTEMPT #$attempts"
                            isCurrentlyRinging -> "WAKEPROOF ALARM RINGING"
                            else -> "1-MIN RE-RING LOOP ARMED"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCurrentlyRinging) DawnAmberBright else DawnCoral
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Center pulsing clock & label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrentlyRinging) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        DawnAmber.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = amPmText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = DawnAmber,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = DawnAmberBright,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!isCurrentlyRinging && awakeProofEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceHigh),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DawnCoral.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = DawnCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = silenceReason ?: "Silenced temporarily",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        val mins = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = DawnCoral
                        )
                        Text(
                            text = "Until alarm rings again automatically",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (awakeProofEnabled) {
                Surface(
                    color = MidnightSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = TwilightTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Swiping or pressing volume silences for ${reRingSec}s only. Submit Awake Proof to turn off for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Bottom actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (awakeProofEnabled) {
                Button(
                    onClick = onEnterProofClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DawnAmber,
                        contentColor = Color(0xFF1F1200)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("btn_enter_awake_proof")
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Turn Off for Today (Enter Proof)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isCurrentlyRinging) {
                    SwipeToSilenceTrack(
                        label = "Swipe to Silence (Re-rings in ${reRingSec}s)",
                        onSwiped = onSwipeSilence
                    )
                } else {
                    TextButton(
                        onClick = onMinimizeWhileArmed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_minimize_armed")
                    ) {
                        Text(
                            text = "Hide Screen (Re-ring stays armed)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                SwipeToSilenceTrack(
                    label = "Swipe to Turn Off Alarm",
                    onSwiped = onDismissNormal
                )
            }
        }
    }
}

@Composable
private fun SwipeToSilenceTrack(
    label: String,
    onSwiped: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MidnightSurfaceHigh)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
            .testTag("swipe_to_silence_track"),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val thumbSizePx = with(density) { 54.dp.toPx() }
        val maxOffsetPx = (constraints.maxWidth.toFloat() - thumbSizePx - with(density) { 10.dp.toPx() })
            .coerceAtLeast(1f)
        var offsetX by remember { mutableFloatStateOf(0f) }

        val progress = (offsetX / maxOffsetPx).coerceIn(0f, 1f)

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = (1f - progress * 1.4f).coerceIn(0.15f, 1f)),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 16.dp)
        )

        Box(
            modifier = Modifier
                .padding(start = 5.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(54.dp)
                .clip(CircleShape)
                .background(DawnCoral)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (offsetX >= maxOffsetPx * 0.78f) {
                            onSwiped()
                        } else {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun AwakeProofInputContent(
    label: String,
    proofPhrase: String,
    strictHidePhrase: Boolean,
    attempts: Int,
    remainingSeconds: Int,
    onBack: () -> Unit,
    onSubmit: (String, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var revealProgress by remember { mutableFloatStateOf(0f) }
    var emergencyRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_proof_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to alarm screen",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (remainingSeconds > 0) {
                    Surface(
                        color = DawnCoral.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        val m = remainingSeconds / 60
                        val s = remainingSeconds % 60
                        Text(
                            text = String.format(Locale.US, "Re-rings in %02d:%02d", m, s),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = DawnCoral,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = DawnAmber,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Prove You're Awake",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Paste or type the Awake Proof text for \"$label\" to turn off this alarm for today.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (attempts > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Silenced $attempts time(s) already — submit proof to break the 1-minute loop.",
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnAmberBright
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!strictHidePhrase || emergencyRevealed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceHigh),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TwilightTeal.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (emergencyRevealed) "EMERGENCY REVEALED PHRASE" else "TYPE THIS EXACT PHRASE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TwilightTeal
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = proofPhrase,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                label = { Text("Awake Proof Text") },
                placeholder = { Text("Paste or type phrase (e.g. FAJR-AWAKE-1234)") },
                isError = errorMessage != null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (!clipText.isNullOrBlank()) {
                                inputText = clipText.trim()
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.testTag("btn_paste_proof")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = "Paste from clipboard",
                            tint = DawnAmber
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSubmit(inputText) { err -> errorMessage = err }
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_awake_proof")
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnCoral,
                    modifier = Modifier.testTag("text_proof_error")
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onSubmit(inputText) { err -> errorMessage = err }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawnAmber,
                    contentColor = Color(0xFF1F1200)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("btn_submit_proof")
            ) {
                Text(
                    text = "Submit & Turn Off for Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (strictHidePhrase && !emergencyRevealed) {
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MidnightSurface)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    var holding = true
                                    val job = scope.launch {
                                        val totalSteps = 50
                                        for (i in 1..totalSteps) {
                                            if (!holding) break
                                            revealProgress = i / totalSteps.toFloat()
                                            delay(100L)
                                        }
                                        if (holding && revealProgress >= 0.99f) {
                                            emergencyRevealed = true
                                        }
                                    }
                                    tryAwaitRelease()
                                    holding = false
                                    job.cancel()
                                    if (!emergencyRevealed) revealProgress = 0f
                                }
                            )
                        }
                        .padding(14.dp)
                        .testTag("btn_emergency_reveal_hold"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Forgot saved text? Hold 5 seconds for Emergency Hint",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (revealProgress > 0f) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { revealProgress },
                                color = DawnAmber,
                                trackColor = MidnightSurfaceHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VerifiedSuccessContent(
    label: String,
    timeText: String,
    attempts: Int,
    onDone: () -> Unit
) {
    val motivation = remember(label) {
        RomanUrduMotivation.forTime(alarmLabel = label)
    }

    LaunchedEffect(Unit) {
        delay(20_000L)
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnAmber.copy(alpha = 0.28f),
                            TwilightTeal.copy(alpha = 0.14f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Awake Verified",
                tint = TwilightTeal,
                modifier = Modifier.size(58.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            color = DawnAmber.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(1.dp, DawnAmber.copy(alpha = 0.45f), RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = DawnAmberBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = motivation.timePeriodTag,
                    style = MaterialTheme.typography.labelMedium,
                    color = DawnAmberBright
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = motivation.greeting,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceHigh),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DawnAmber.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"${motivation.headline}\"",
                    style = MaterialTheme.typography.titleLarge,
                    color = DawnAmberBright,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = motivation.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "\"$label\" switched off for today ($timeText).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (attempts > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "1-minute re-ring loop stopped after $attempts attempt(s)!",
                style = MaterialTheme.typography.labelMedium,
                color = TwilightTeal
            )
        }

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = DawnAmber,
                contentColor = Color(0xFF1F1200)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_verified_done")
        ) {
            Text(
                text = "Shuru Karein • Start My Day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
