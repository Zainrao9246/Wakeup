package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DawnCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TwilightTeal
import com.example.ui.viewmodel.StopwatchUiState
import java.util.Locale

@Composable
fun StopwatchScreen(
    state: StopwatchUiState,
    onStartOrPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fastestLap = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.minOfOrNull { it.lapMillis } else null
    }
    val slowestLap = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.maxOfOrNull { it.lapMillis } else null
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ELAPSED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatStopwatch(state.elapsedMillis),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = if (state.isRunning) onLap else onReset,
                    enabled = state.elapsedMillis > 0L || state.isRunning,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_stopwatch_lap_reset")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Flag else Icons.Rounded.Refresh,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isRunning) "Lap" else "Reset")
                }

                Button(
                    onClick = onStartOrPause,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp)
                        .testTag("btn_stopwatch_start_pause")
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isRunning) "Pause" else "Start")
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.laps, key = { it.number }) { lap ->
                    val badgeColor = when (lap.lapMillis) {
                        fastestLap -> TwilightTeal
                        slowestLap -> DawnCoral
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lap #${lap.number}",
                                style = MaterialTheme.typography.titleMedium,
                                color = badgeColor
                            )
                            Text(
                                text = "+${formatStopwatch(lap.lapMillis)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = badgeColor
                            )
                            Text(
                                text = formatStopwatch(lap.totalMillis),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatStopwatch(millis: Long): String {
    val totalCs = (millis / 10L)
    val cs = (totalCs % 100L).toInt()
    val totalSec = totalCs / 100L
    val sec = (totalSec % 60L).toInt()
    val min = ((totalSec / 60L) % 60L).toInt()
    val hr = (totalSec / 3600L).toInt()
    return if (hr > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%02d", hr, min, sec, cs)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", min, sec, cs)
    }
}
