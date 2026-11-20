package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.alarm.MotivationalMessage
import com.example.alarm.RomanUrduMotivation
import com.example.data.AlarmEntity
import com.example.data.ProofVerificationResult
import com.example.data.WakeProofLogEntity
import com.example.ui.theme.DawnAmber
import com.example.ui.theme.DawnAmberBright
import com.example.ui.theme.DawnCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TwilightTeal
import com.example.ui.viewmodel.AlarmsUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmsScreen(
    uiState: AlarmsUiState,
    canScheduleExactAlarms: Boolean,
    selectedProofModalAlarm: AlarmEntity?,
    snackbarHostState: SnackbarHostState,
    onCreateAlarm: () -> Unit,
    onQuickCreateFajr: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onTestRing: (Long) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onOpenProofModal: (AlarmEntity) -> Unit,
    onCloseProofModal: () -> Unit,
    onSubmitProofInApp: (Long, String, (ProofVerificationResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var celebrationMessage by remember { mutableStateOf<MotivationalMessage?>(null) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var alarmVolumeLow by remember {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        mutableStateOf(current * 100 / max < 20)
    }

    val todayEpoch = remember(uiState.currentTimeMillis) {
        AlarmScheduler.currentEpochDay(uiState.currentTimeMillis)
    }

    val pendingReRingAlarms = remember(uiState.alarms) {
        uiState.alarms.filter { it.isPendingReRing }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DawnHeroCard(
                    uiState = uiState,
                    onToggle24Hour = onToggle24Hour,
                    onOpenHistory = { showHistoryDialog = true }
                )
            }

            if (alarmVolumeLow && uiState.alarms.any { it.isEnabled }) {
                item {
                    PermissionBannerCard(
                        title = "Alarm Volume is Low or Muted",
                        body = "Raise your device's alarm stream so Fajr and 1-minute re-ring alerts can wake you.",
                        buttonLabel = "Boost Alarm Volume (85%)",
                        onClick = {
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                            val target = (maxVol * 0.85f).toInt().coerceAtLeast(1)
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                            alarmVolumeLow = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Device alarm volume boosted to 85%!")
                            }
                        }
                    )
                }
            }

            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionBannerCard(
                        title = "Enable Alarm Notifications",
                        body = "Required so full-screen alarms and 1-minute re-ring alerts can wake you reliably.",
                        buttonLabel = "Allow Notifications",
                        onClick = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }

            if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PermissionBannerCard(
                        title = "Allow Exact Alarms",
                        body = "Grant exact alarm access so Fajr and 1-minute re-ring alarms fire on the exact second.",
                        buttonLabel = "Open Settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }

            items(pendingReRingAlarms, key = { "pending_${it.id}" }) { armedAlarm ->
                PendingReRingUrgentCard(
                    alarm = armedAlarm,
                    nowMillis = uiState.currentTimeMillis,
                    onEnterProof = { onOpenProofModal(armedAlarm) }
                )
            }

            if (uiState.alarms.isEmpty()) {
                item {
                    EmptyAlarmsCard(
                        onQuickCreateFajr = onQuickCreateFajr,
                        onCreateCustom = onCreateAlarm
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR ALARMS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.alarms.count { it.isEnabled }} active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(uiState.alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        use24Hour = uiState.use24Hour,
                        todayEpoch = todayEpoch,
                        nowMillis = uiState.currentTimeMillis,
                        onClick = {
                            if (alarm.isPendingReRing) {
                                onOpenProofModal(alarm)
                            } else {
                                onEditAlarm(alarm.id)
                            }
                        },
                        onEdit = {
                            if (alarm.isPendingReRing) {
                                onOpenProofModal(alarm)
                            } else {
                                onEditAlarm(alarm.id)
                            }
                        },
                        onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                        onCopyPhrase = {
                            if (alarm.proofPhrase.isNotBlank()) {
                                copyToClipboard(context, alarm.proofPhrase)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied Awake Proof: ${alarm.proofPhrase}")
                                }
                            }
                        },
                        onTestRing = {
                            onTestRing(alarm.id)
                        },
                        onDelete = { onDeleteAlarm(alarm) }
                    )
                }
            }
        }
    }

    if (selectedProofModalAlarm != null) {
        InAppProofVerificationDialog(
            alarm = selectedProofModalAlarm,
            onDismiss = onCloseProofModal,
            onVerify = { entered, onError ->
                onSubmitProofInApp(selectedProofModalAlarm.id, entered) { res ->
                    when (res) {
                        is ProofVerificationResult.Success -> {
                            celebrationMessage = RomanUrduMotivation.forTime(
                                alarmLabel = res.alarmLabel
                            )
                        }
                        is ProofVerificationResult.Mismatch -> onError(res.message)
                        ProofVerificationResult.NotFound -> onCloseProofModal()
                    }
                }
            }
        )
    }

    if (celebrationMessage != null) {
        val msg = celebrationMessage!!
        AlertDialog(
            onDismissRequest = { celebrationMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = DawnAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = msg.greeting,
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = msg.headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { celebrationMessage = null },
                    modifier = Modifier.testTag("btn_celebration_ok")
                ) {
                    Text("Shuru Karein!")
                }
            }
        )
    }

    if (showHistoryDialog) {
        WakeHistoryDialog(
            logs = uiState.recentLogs,
            streakDays = uiState.wakeStreakDays,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
private fun DawnHeroCard(
    uiState: AlarmsUiState,
    onToggle24Hour: (Boolean) -> Unit,
    onOpenHistory: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(28.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_fajr_dawn_banner),
                contentDescription = stringResource(R.string.cd_dawn_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA080D1B),
                                Color(0xCC090E1C),
                                Color(0xF0090E1C)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DawnAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, DawnAmber.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .clickable(onClick = onOpenHistory)
                            .testTag("btn_open_wake_history")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.wakeStreakDays > 0) Icons.Rounded.LocalFireDepartment else Icons.Rounded.History,
                                contentDescription = "View wake-up log",
                                tint = DawnAmberBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.wakeStreakDays > 0) {
                                    "${uiState.wakeStreakDays}-day Streak • Log"
                                } else {
                                    "Wake-Up Log"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = DawnAmberBright
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onToggle24Hour(!uiState.use24Hour) }
                            .testTag("btn_toggle_24h")
                    ) {
                        Text(
                            text = if (uiState.use24Hour) "24H" else "12H",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = RomanUrduMotivation.shortGreeting(),
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnAmberBright
                    )
                    Spacer(Modifier.height(2.dp))
                    val triggerMillis = uiState.nextAlarmTriggerMillis
                    if (triggerMillis != null) {
                        val diffMs = (triggerMillis - uiState.currentTimeMillis).coerceAtLeast(0L)
                        val totalMinutes = (diffMs / 60_000L).toInt()
                        val hours = totalMinutes / 60
                        val mins = totalMinutes % 60
                        val secs = ((diffMs % 60_000L) / 1000L).toInt()

                        val countdownLabel = when {
                            hours > 0 -> "in ${hours}h ${mins}m"
                            mins > 0 -> "in ${mins}m ${secs}s"
                            else -> "in ${secs}s"
                        }
                        Text(
                            text = "NEXT: ${uiState.nextAlarmLabel ?: "Alarm"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TwilightTeal
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Rings $countdownLabel",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "NO ACTIVE ALARM",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Never oversleep Fajr again",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WakeHistoryDialog(
    logs: List<WakeProofLogEntity>,
    streakDays: Int,
    onDismiss: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = DawnAmber
                )
                Spacer(Modifier.width(8.dp))
                Text("Wake-Up Proof Log (${streakDays}d Streak)")
            }
        },
        text = {
            if (logs.isEmpty()) {
                Text(
                    text = "Abhi tak koi Awake Proof record nahi hai. Jab aap apna alarm text submit kar ke band karein ge, aapka record yahan show hoga!",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.alarmLabel,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Verified ✓",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = dateFmt.format(Date(log.verifiedAtMillis)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (log.reRingAttempts > 0) {
                                    Text(
                                        text = "Beat ${log.reRingAttempts} re-ring attempt(s)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PendingReRingUrgentCard(
    alarm: AlarmEntity,
    nowMillis: Long,
    onEnterProof: () -> Unit
) {
    val remSec = ((alarm.nextReRingAtMillis ?: nowMillis) - nowMillis)
        .coerceAtLeast(0L)
        .div(1000L)
        .toInt()
    val mm = remSec / 60
    val ss = remSec % 60

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, DawnCoral, RoundedCornerShape(22.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${alarm.label.ifBlank { "Alarm" }} • 1-Min Loop Active",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "Re-ringing in %02d:%02d", mm, ss),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Button(
                onClick = onEnterProof,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DawnAmber,
                    contentColor = Color(0xFF1F1200)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_urgent_enter_proof_${alarm.id}")
            ) {
                Text("Enter Proof", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionBannerCard(
    title: String,
    body: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.height(48.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun EmptyAlarmsCard(
    onQuickCreateFajr: () -> Unit,
    onCreateCustom: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Set Your First WakeProof Alarm",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "When you create an alarm, you get a unique Awake Proof text. Swiping or pressing volume only silences it for 1 minute—paste the text to turn it off for the day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onQuickCreateFajr,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_quick_fajr")
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Quick Setup: 6:00 AM Fajr Alarm")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCreateCustom,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_empty_create_custom")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Custom Alarm")
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    use24Hour: Boolean,
    todayEpoch: Long,
    nowMillis: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onCopyPhrase: () -> Unit,
    onTestRing: () -> Unit,
    onDelete: () -> Unit
) {
    val timeParts = remember(alarm.hour, alarm.minute, use24Hour) {
        if (use24Hour) {
            String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute) to ""
        } else {
            val amPm = if (alarm.hour < 12) "AM" else "PM"
            val h = when {
                alarm.hour == 0 -> 12
                alarm.hour > 12 -> alarm.hour - 12
                else -> alarm.hour
            }
            String.format(Locale.US, "%d:%02d", h, alarm.minute) to amPm
        }
    }

    val verifiedToday = alarm.lastDismissedEpochDay == todayEpoch

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (alarm.isPendingReRing) 1.5.dp else 1.dp,
                color = if (alarm.isPendingReRing) {
                    DawnCoral
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = if (alarm.isEnabled) 0.5f else 0.25f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = alarm.label.ifBlank { "Alarm" },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (verifiedToday) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Verified Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeParts.first,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (timeParts.second.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = timeParts.second,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("switch_alarm_${alarm.id}")
                )
            }

            Spacer(Modifier.height(10.dp))

            // Repeat & Awake Proof badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = alarm.repeatSummary(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (alarm.awakeProofEnabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Awake Proof • ${alarm.reRingIntervalSeconds}s loop",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (alarm.isPendingReRing) {
                val remSec = ((alarm.nextReRingAtMillis ?: nowMillis) - nowMillis)
                    .coerceAtLeast(0L)
                    .div(1000L)
                    .toInt()
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = DawnCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Re-ringing in ${remSec}s (Attempt #${alarm.reRingAttemptCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = DawnCoral
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick action bar: Copy Proof Phrase | Test Ring | Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alarm.awakeProofEnabled && alarm.proofPhrase.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCopyPhrase)
                            .testTag("btn_copy_phrase_${alarm.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy_proof),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = alarm.proofPhrase,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onTestRing,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_test_ring_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_test_ring),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Test", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_edit_alarm_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_edit_alarm),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_delete_alarm_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_alarm),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppProofVerificationDialog(
    alarm: AlarmEntity,
    onDismiss: () -> Unit,
    onVerify: (String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Awake Proof") },
        text = {
            Column {
                Text(
                    text = "Enter or paste the Awake Proof text for \"${alarm.label.ifBlank { "Alarm" }}\" to turn off the 1-minute re-ring loop for today.",
                    style = MaterialTheme.typography.bodyMedium
                )
                AnimatedVisibility(visible = !alarm.strictHidePhrase) {
                    Text(
                        text = "Phrase: ${alarm.proofPhrase}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text("Awake Proof Text") },
                    isError = error != null,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            if (!clip.isNullOrBlank()) {
                                input = clip.trim()
                                error = null
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = stringResource(R.string.cd_paste_proof)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_proof")
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(input) { msg -> error = msg } },
                modifier = Modifier.testTag("dialog_btn_submit_proof")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Awake Proof", text))
}
