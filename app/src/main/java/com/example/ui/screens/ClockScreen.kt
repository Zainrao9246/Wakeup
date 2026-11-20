package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.WorldClockCatalog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockScreen(
    use24Hour: Boolean,
    savedZones: Set<String>,
    onAddZone: (String) -> Unit,
    onRemoveZone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCityPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
    }

    val localCalendar = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val timePattern = if (use24Hour) "HH:mm:ss" else "h:mm:ss a"
    val timeText = remember(nowMillis, use24Hour) {
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(nowMillis))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnalogClockFace(
                            calendar = localCalendar,
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = timeText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORLD CLOCKS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showCityPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_add_world_city")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add City")
                    }
                }
            }

            items(savedZones.toList().sorted(), key = { it }) { zoneId ->
                WorldCityCard(
                    zoneId = zoneId,
                    nowMillis = nowMillis,
                    use24Hour = use24Hour,
                    onRemove = { onRemoveZone(zoneId) }
                )
            }
        }
    }

    if (showCityPicker) {
        val available = WorldClockCatalog.cities.filter { it.zoneId !in savedZones }
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = { Text("Add World Clock") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (available.isEmpty()) {
                        item {
                            Text(
                                text = "All curated cities are already added.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(available, key = { it.zoneId }) { city ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddZone(city.zoneId)
                                        showCityPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(city.city, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        city.country,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AnalogClockFace(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.outline
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val hourHandColor = MaterialTheme.colorScheme.onSurface
    val minuteHandColor = MaterialTheme.colorScheme.secondary
    val secondHandColor = MaterialTheme.colorScheme.primary
    val centerBg = MaterialTheme.colorScheme.surface

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = centerBg,
            radius = radius
        )
        drawCircle(
            color = ringColor,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30.0) - 90.0)
            val inner = if (i % 3 == 0) radius * 0.78f else radius * 0.85f
            val outer = radius * 0.92f
            drawLine(
                color = if (i % 3 == 0) secondHandColor else tickColor.copy(alpha = 0.6f),
                start = Offset(
                    center.x + (cos(angleRad) * inner).toFloat(),
                    center.y + (sin(angleRad) * inner).toFloat()
                ),
                end = Offset(
                    center.x + (cos(angleRad) * outer).toFloat(),
                    center.y + (sin(angleRad) * outer).toFloat()
                ),
                strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val hourAngle = Math.toRadians(((hours + minutes / 60.0) * 30.0) - 90.0)
        drawLine(
            color = hourHandColor,
            start = center,
            end = Offset(
                center.x + (cos(hourAngle) * radius * 0.50f).toFloat(),
                center.y + (sin(hourAngle) * radius * 0.50f).toFloat()
            ),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )

        val minuteAngle = Math.toRadians(((minutes + seconds / 60.0) * 6.0) - 90.0)
        drawLine(
            color = minuteHandColor,
            start = center,
            end = Offset(
                center.x + (cos(minuteAngle) * radius * 0.72f).toFloat(),
                center.y + (sin(minuteAngle) * radius * 0.72f).toFloat()
            ),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        val secondAngle = Math.toRadians((seconds * 6.0) - 90.0)
        drawLine(
            color = secondHandColor,
            start = center,
            end = Offset(
                center.x + (cos(secondAngle) * radius * 0.82f).toFloat(),
                center.y + (sin(secondAngle) * radius * 0.82f).toFloat()
            ),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = secondHandColor,
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun WorldCityCard(
    zoneId: String,
    nowMillis: Long,
    use24Hour: Boolean,
    onRemove: () -> Unit
) {
    val cityInfo = remember(zoneId) { WorldClockCatalog.findByZone(zoneId) }
    val targetTz = remember(zoneId) { TimeZone.getTimeZone(zoneId) }
    val localTz = TimeZone.getDefault()

    val timeFmt = remember(zoneId, use24Hour) {
        SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).apply {
            timeZone = targetTz
        }
    }
    val dayFmt = remember(zoneId) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
            timeZone = targetTz
        }
    }
    val diffHours = (targetTz.getOffset(nowMillis) - localTz.getOffset(nowMillis)) / 3_600_000.0
    val diffText = when {
        diffHours == 0.0 -> "Same time"
        diffHours > 0 -> "+${if (diffHours % 1.0 == 0.0) diffHours.toInt() else diffHours}h"
        else -> "${if (diffHours % 1.0 == 0.0) diffHours.toInt() else diffHours}h"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cityInfo.city,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${dayFmt.format(Date(nowMillis))} • $diffText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFmt.format(Date(nowMillis)),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove ${cityInfo.city}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
