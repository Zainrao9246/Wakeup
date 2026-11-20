package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.TimerUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    state: TimerUiState,
    onSetDuration: (Int, Int) -> Unit,
    onStartOrPause: () -> Unit,
    onAddOneMinute: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    val totalSec = (state.remainingMillis / 1000L).toInt()
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    val formatted = if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    val progress = if (state.totalMillis > 0L) {
        (state.remainingMillis.toFloat() / state.totalMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1, 5, 10, 15, 30, 45).forEach { m ->
                    FilterChip(
                        selected = state.totalMillis == m * 60_000L && !state.isRunning,
                        onClick = { onSetDuration(m, 0) },
                        label = { Text("${m}m") }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showCustomDialog = true },
                    leadingIcon = {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Custom") }
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                val activeColor = if (state.isFinished) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isFinished) "TIME UP" else "REMAINING",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.isFinished) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatted,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_timer_reset")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = onStartOrPause,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp)
                        .testTag("btn_timer_start_pause")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            state.isFinished -> "Stop"
                            state.isRunning -> "Pause"
                            else -> "Start"
                        }
                    )
                }

                OutlinedButton(
                    onClick = onAddOneMinute,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_timer_plus_1m")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("1m")
                }
            }
        }
    }

    if (showCustomDialog) {
        var minsText by remember { mutableStateOf("10") }
        var secsText by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Timer") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secsText,
                        onValueChange = { secsText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Seconds") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = minsText.toIntOrNull() ?: 0
                    val s = (secsText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    if (m > 0 || s > 0) {
                        onSetDuration(m, s)
                    }
                    showCustomDialog = false
                }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
