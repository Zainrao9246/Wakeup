package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.AlarmTonePlayer
import com.example.alarm.BuiltInAlarmTones
import com.example.alarm.ProofPhraseGenerator
import com.example.data.AlarmEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditorScreen(
    existingAlarm: AlarmEntity?,
    use24Hour: Boolean,
    defaultReRingSeconds: Int = 60,
    onSave: (AlarmEntity) -> Unit,
    onDelete: ((AlarmEntity) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val initialHour = existingAlarm?.hour ?: 6
    val initialMinute = existingAlarm?.minute ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = use24Hour
    )
    var useKeyboardTimeInput by remember { mutableStateOf(false) }

    var label by remember(existingAlarm) {
        mutableStateOf(existingAlarm?.label ?: "Fajr Namaz")
    }
    var repeatMask by remember(existingAlarm) {
        mutableIntStateOf(existingAlarm?.repeatDaysMask ?: AlarmEntity.MASK_EVERY_DAY)
    }
    var awakeProofEnabled by remember(existingAlarm) {
        mutableStateOf(existingAlarm?.awakeProofEnabled ?: true)
    }
    var proofPhrase by remember(existingAlarm) {
        mutableStateOf(
            existingAlarm?.proofPhrase?.takeIf { it.isNotBlank() }
                ?: ProofPhraseGenerator.generate("Fajr Namaz")
        )
    }
    var editingCustomPhrase by remember { mutableStateOf(false) }
    var reRingIntervalSeconds by remember(existingAlarm, defaultReRingSeconds) {
        mutableIntStateOf(existingAlarm?.reRingIntervalSeconds ?: defaultReRingSeconds)
    }
    var strictHidePhrase by remember(existingAlarm) {
        mutableStateOf(existingAlarm?.strictHidePhrase ?: true)
    }
    var ringtoneUri by remember(existingAlarm) {
        mutableStateOf<String?>(existingAlarm?.ringtoneUri ?: BuiltInAlarmTones.URI_DAWN_CHIME)
    }
    var ringtoneTitle by remember(existingAlarm) {
        mutableStateOf(
            existingAlarm?.ringtoneTitle
                ?: BuiltInAlarmTones.findTitle(BuiltInAlarmTones.URI_DAWN_CHIME)
                ?: "Dawn Chimes"
        )
    }
    var vibrate by remember(existingAlarm) {
        mutableStateOf(existingAlarm?.vibrate ?: true)
    }
    var gradualVolume by remember(existingAlarm) {
        mutableStateOf(existingAlarm?.gradualVolume ?: true)
    }

    var showTonePickerDialog by remember { mutableStateOf(false) }
    val previewPlayer = remember { AlarmTonePlayer(context) }
    var playingPreviewUri by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.stop()
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                ringtoneUri = pickedUri.toString()
                val title = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull()
                ringtoneTitle = title ?: "Custom Sound"
            } else {
                ringtoneUri = null
                ringtoneTitle = "Default Alarm"
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (existingAlarm == null) "New Alarm" else "Edit Alarm")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_editor_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalPhrase = if (awakeProofEnabled && proofPhrase.isBlank()) {
                                ProofPhraseGenerator.generate(label)
                            } else {
                                proofPhrase.trim()
                            }
                            if (awakeProofEnabled) {
                                copyToClipboard(context, finalPhrase)
                            }
                            onSave(
                                AlarmEntity(
                                    id = existingAlarm?.id ?: 0L,
                                    hour = timePickerState.hour,
                                    minute = timePickerState.minute,
                                    label = label.trim().ifBlank { "Alarm" },
                                    isEnabled = true,
                                    repeatDaysMask = repeatMask,
                                    awakeProofEnabled = awakeProofEnabled,
                                    proofPhrase = finalPhrase,
                                    reRingIntervalSeconds = reRingIntervalSeconds,
                                    strictHidePhrase = strictHidePhrase,
                                    ringtoneUri = ringtoneUri,
                                    ringtoneTitle = ringtoneTitle,
                                    vibrate = vibrate,
                                    gradualVolume = gradualVolume,
                                    createdAtMillis = existingAlarm?.createdAtMillis
                                        ?: System.currentTimeMillis()
                                )
                            )
                        },
                        modifier = Modifier.testTag("btn_editor_save_top")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Picker Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alarm Time",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { useKeyboardTimeInput = !useKeyboardTimeInput },
                                modifier = Modifier.testTag("btn_toggle_time_input_mode")
                            ) {
                                Icon(
                                    imageVector = if (useKeyboardTimeInput) Icons.Rounded.Schedule else Icons.Rounded.Keyboard,
                                    contentDescription = "Switch time input mode"
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (useKeyboardTimeInput) {
                            TimeInput(state = timePickerState)
                        } else {
                            TimePicker(state = timePickerState)
                        }
                    }
                }

                // Label & Quick Presets
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Alarm Label") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_alarm_label")
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Fajr Namaz", "Tahajjud", "Morning Wake", "Work", "Workout").forEach { preset ->
                                FilterChip(
                                    selected = label == preset,
                                    onClick = { label = preset },
                                    label = { Text(preset) }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Repeat Days", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayLetters.forEachIndexed { idx, letter ->
                                val selected = (repeatMask and (1 shl idx)) != 0
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                        .clickable {
                                            repeatMask = repeatMask xor (1 shl idx)
                                        }
                                        .testTag("day_chip_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = repeatMask == 0,
                                onClick = { repeatMask = 0 },
                                label = { Text("Once") }
                            )
                            FilterChip(
                                selected = repeatMask == AlarmEntity.MASK_EVERY_DAY,
                                onClick = { repeatMask = AlarmEntity.MASK_EVERY_DAY },
                                label = { Text("Every day") }
                            )
                            FilterChip(
                                selected = repeatMask == AlarmEntity.MASK_WEEKDAYS,
                                onClick = { repeatMask = AlarmEntity.MASK_WEEKDAYS },
                                label = { Text("Weekdays") }
                            )
                        }
                    }
                }

                // Awake Proof Shield Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Awake Proof Mode",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Require verification text to turn off",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = awakeProofEnabled,
                                onCheckedChange = { awakeProofEnabled = it },
                                modifier = Modifier.testTag("switch_awake_proof")
                            )
                        }

                        AnimatedVisibility(visible = awakeProofEnabled) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    text = "Your Awake Proof Text (copy & save it to paste when waking up):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))

                                if (editingCustomPhrase) {
                                    OutlinedTextField(
                                        value = proofPhrase,
                                        onValueChange = { proofPhrase = it },
                                        label = { Text("Custom Awake Proof Text") },
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { editingCustomPhrase = false }) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Done")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = proofPhrase,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        copyToClipboard(context, proofPhrase)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Copied Awake Proof text!")
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("btn_editor_copy_phrase")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = stringResource(R.string.cd_copy_proof)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        proofPhrase = ProofPhraseGenerator.generate(label)
                                                    },
                                                    modifier = Modifier.testTag("btn_editor_regen_phrase")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = stringResource(R.string.cd_regenerate_proof)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { editingCustomPhrase = true },
                                                    modifier = Modifier.testTag("btn_editor_custom_phrase")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Edit custom phrase"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Re-Ring Interval if Swiped or Volume Silenced",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(30 to "30 sec", 60 to "1 min (Default)", 120 to "2 min", 180 to "3 min")
                                        .forEach { (sec, title) ->
                                            FilterChip(
                                                selected = reRingIntervalSeconds == sec,
                                                onClick = { reRingIntervalSeconds = sec },
                                                label = { Text(title) }
                                            )
                                        }
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hide Phrase on Alarm Screen",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = if (strictHidePhrase) {
                                                "Strict: paste/type saved text (5s hold hint if lost)"
                                            } else {
                                                "Typing test: shows phrase on screen to type"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = strictHidePhrase,
                                        onCheckedChange = { strictHidePhrase = it },
                                        modifier = Modifier.testTag("switch_strict_hide")
                                    )
                                }
                            }
                        }
                    }
                }

                // Sound & Vibration Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewPlayer.stop()
                                        playingPreviewUri = null
                                        showTonePickerDialog = true
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("btn_pick_ringtone"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Alarm Sound", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = ringtoneTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val isCurrentPlaying = playingPreviewUri != null
                            IconButton(
                                onClick = {
                                    if (isCurrentPlaying) {
                                        previewPlayer.stop()
                                        playingPreviewUri = null
                                    } else {
                                        val targetUri = ringtoneUri ?: "system_default"
                                        previewPlayer.start(scope, ringtoneUri, gradualVolume = false)
                                        playingPreviewUri = targetUri
                                    }
                                },
                                modifier = Modifier.testTag("btn_preview_ringtone")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isCurrentPlaying) "Stop sound preview" else "Preview alarm sound",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Vibration, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = vibrate,
                                onCheckedChange = { vibrate = it },
                                modifier = Modifier.testTag("switch_vibrate")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Gradual Volume Ramp", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Starts gentle, reaches full volume in 10s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = gradualVolume,
                                onCheckedChange = { gradualVolume = it },
                                modifier = Modifier.testTag("switch_gradual_volume")
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val finalPhrase = if (awakeProofEnabled && proofPhrase.isBlank()) {
                            ProofPhraseGenerator.generate(label)
                        } else {
                            proofPhrase.trim()
                        }
                        if (awakeProofEnabled) {
                            copyToClipboard(context, finalPhrase)
                        }
                        onSave(
                            AlarmEntity(
                                id = existingAlarm?.id ?: 0L,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                label = label.trim().ifBlank { "Alarm" },
                                isEnabled = true,
                                repeatDaysMask = repeatMask,
                                awakeProofEnabled = awakeProofEnabled,
                                proofPhrase = finalPhrase,
                                reRingIntervalSeconds = reRingIntervalSeconds,
                                strictHidePhrase = strictHidePhrase,
                                ringtoneUri = ringtoneUri,
                                ringtoneTitle = ringtoneTitle,
                                vibrate = vibrate,
                                gradualVolume = gradualVolume,
                                createdAtMillis = existingAlarm?.createdAtMillis
                                    ?: System.currentTimeMillis()
                            )
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_editor_save_bottom")
                ) {
                    Text(
                        text = if (awakeProofEnabled) "Save Alarm & Copy Awake Proof" else "Save Alarm",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (existingAlarm != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(existingAlarm) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_editor_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_alarm),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Delete Alarm",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTonePickerDialog) {
        AlertDialog(
            onDismissRequest = {
                previewPlayer.stop()
                playingPreviewUri = null
                showTonePickerDialog = false
            },
            title = { Text("Choose Alarm Sound") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "BUILT-IN WAKEPROOF TONES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    BuiltInAlarmTones.allTones.forEach { tone ->
                        val isSelected = ringtoneUri == tone.uri
                        val isPlaying = playingPreviewUri == tone.uri
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    ringtoneUri = tone.uri
                                    ringtoneTitle = tone.title
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    ringtoneUri = tone.uri
                                    ringtoneTitle = tone.title
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tone.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    tone.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        previewPlayer.stop()
                                        playingPreviewUri = null
                                    } else {
                                        previewPlayer.start(scope, tone.uri, gradualVolume = false)
                                        playingPreviewUri = tone.uri
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop ${tone.title}" else "Preview ${tone.title}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "DEVICE RINGTONES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ringtoneUri = null
                                ringtoneTitle = "System Default Alarm"
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ringtoneUri == null,
                            onClick = {
                                ringtoneUri = null
                                ringtoneTitle = "System Default Alarm"
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System Default Alarm", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Uses your phone's default alarm sound",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            previewPlayer.stop()
                            playingPreviewUri = null
                            showTonePickerDialog = false
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                if (ringtoneUri != null && !BuiltInAlarmTones.isBuiltIn(ringtoneUri)) {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        Uri.parse(ringtoneUri)
                                    )
                                }
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Phone Ringtones...")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        previewPlayer.stop()
                        playingPreviewUri = null
                        showTonePickerDialog = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
