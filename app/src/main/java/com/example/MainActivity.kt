package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassFull
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AlarmEntity
import com.example.ui.screens.AlarmEditorScreen
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.screens.copyToClipboard
import com.example.ui.theme.WakeProofTheme
import com.example.ui.viewmodel.AlarmsViewModel
import com.example.ui.viewmodel.TimerStopwatchViewModel
import kotlinx.coroutines.launch

object Routes {
    const val ALARMS = "alarms"
    const val CLOCK = "clock"
    const val TIMER = "timer"
    const val STOPWATCH = "stopwatch"
    const val EDITOR = "alarm_editor/{alarmId}"

    fun editor(alarmId: Long): String = "alarm_editor/$alarmId"
}

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeProofTheme {
                WakeProofRootApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeProofRootApp(
    alarmsViewModel: AlarmsViewModel = viewModel(),
    timerStopwatchViewModel: TimerStopwatchViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.ALARMS

    val alarmsUiState by alarmsViewModel.uiState.collectAsStateWithLifecycle()
    val savedWorldZones by alarmsViewModel.savedWorldZones.collectAsStateWithLifecycle()
    val defaultReRingSeconds by alarmsViewModel.defaultReRingSeconds.collectAsStateWithLifecycle()
    val selectedProofModalAlarm by alarmsViewModel.selectedAlarmForProofModal.collectAsStateWithLifecycle()
    val timerState by timerStopwatchViewModel.timerState.collectAsStateWithLifecycle()
    val stopwatchState by timerStopwatchViewModel.stopwatchState.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabs = remember {
        listOf(
            NavTab(Routes.ALARMS, R.string.nav_alarms, Icons.Rounded.Alarm, Icons.Outlined.Alarm),
            NavTab(Routes.CLOCK, R.string.nav_clock, Icons.Rounded.Schedule, Icons.Outlined.Schedule),
            NavTab(Routes.TIMER, R.string.nav_timer, Icons.Rounded.HourglassFull, Icons.Outlined.HourglassEmpty),
            NavTab(Routes.STOPWATCH, R.string.nav_stopwatch, Icons.Rounded.Timer, Icons.Outlined.Timer)
        )
    }

    val isTopLevelRoute = tabs.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            val titleText = when (currentRoute) {
                                Routes.ALARMS -> "WakeProof"
                                Routes.CLOCK -> "World Clock"
                                Routes.TIMER -> "Timer"
                                Routes.STOPWATCH -> "Stopwatch"
                                else -> "WakeProof"
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        },
                        actions = {
                            if (currentRoute == Routes.ALARMS) {
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.testTag("btn_open_settings")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "WakeProof Settings and Guide"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.ALARMS) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == Routes.ALARMS) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Routes.editor(-1L)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_add_alarm)
                            )
                        },
                        text = { Text("New Alarm") },
                        modifier = Modifier.testTag("fab_add_alarm")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.ALARMS) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.ALARMS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.ALARMS) {
                        AlarmsScreen(
                            uiState = alarmsUiState,
                            canScheduleExactAlarms = alarmsViewModel.canScheduleExactAlarms(),
                            selectedProofModalAlarm = selectedProofModalAlarm,
                            snackbarHostState = snackbarHostState,
                            onCreateAlarm = { navController.navigate(Routes.editor(-1L)) },
                            onQuickCreateFajr = {
                                alarmsViewModel.createQuickFajrAlarm { created ->
                                    copyToClipboard(context, created.proofPhrase)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Fajr Alarm set! Awake Proof copied: ${created.proofPhrase}"
                                        )
                                    }
                                }
                            },
                            onEditAlarm = { alarmId ->
                                navController.navigate(Routes.editor(alarmId))
                            },
                            onToggleAlarm = alarmsViewModel::toggleAlarm,
                            onDeleteAlarm = alarmsViewModel::deleteAlarm,
                            onTestRing = alarmsViewModel::testRingAlarm,
                            onToggle24Hour = alarmsViewModel::setUse24Hour,
                            onOpenProofModal = alarmsViewModel::openProofModal,
                            onCloseProofModal = alarmsViewModel::closeProofModal,
                            onSubmitProofInApp = alarmsViewModel::verifyProofInApp
                        )
                    }

                    composable(Routes.CLOCK) {
                        ClockScreen(
                            use24Hour = alarmsUiState.use24Hour,
                            savedZones = savedWorldZones,
                            onAddZone = alarmsViewModel::addWorldZone,
                            onRemoveZone = alarmsViewModel::removeWorldZone
                        )
                    }

                    composable(Routes.TIMER) {
                        TimerScreen(
                            state = timerState,
                            onSetDuration = timerStopwatchViewModel::setTimerDuration,
                            onStartOrPause = timerStopwatchViewModel::startOrPauseTimer,
                            onAddOneMinute = timerStopwatchViewModel::addOneMinuteToTimer,
                            onReset = timerStopwatchViewModel::resetTimer
                        )
                    }

                    composable(Routes.STOPWATCH) {
                        StopwatchScreen(
                            state = stopwatchState,
                            onStartOrPause = timerStopwatchViewModel::startOrPauseStopwatch,
                            onLap = timerStopwatchViewModel::recordLap,
                            onReset = timerStopwatchViewModel::resetStopwatch
                        )
                    }

                    composable(
                        route = Routes.EDITOR,
                        arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
                    ) { entry ->
                        val alarmId = entry.arguments?.getLong("alarmId") ?: -1L
                        var existingAlarm by remember(alarmId) { mutableStateOf<AlarmEntity?>(null) }
                        var isLoaded by remember(alarmId) { mutableStateOf(alarmId <= 0L) }

                        LaunchedEffect(alarmId) {
                            existingAlarm = if (alarmId > 0L) {
                                alarmsViewModel.getAlarmById(alarmId)
                            } else {
                                null
                            }
                            isLoaded = true
                        }

                        if (isLoaded) {
                            AlarmEditorScreen(
                                existingAlarm = existingAlarm,
                                use24Hour = alarmsUiState.use24Hour,
                                defaultReRingSeconds = defaultReRingSeconds,
                                onSave = { savedEntity ->
                                    alarmsViewModel.saveAlarm(savedEntity) {
                                        navController.popBackStack()
                                        scope.launch {
                                            val msg = if (savedEntity.awakeProofEnabled) {
                                                "Alarm saved! Awake Proof copied: ${savedEntity.proofPhrase}"
                                            } else {
                                                "Alarm saved!"
                                            }
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                },
                                onDelete = { alarmToDelete ->
                                    alarmsViewModel.deleteAlarm(alarmToDelete)
                                    navController.popBackStack()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Alarm deleted")
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        WakeProofSettingsDialog(
            use24Hour = alarmsUiState.use24Hour,
            defaultReRingSeconds = defaultReRingSeconds,
            onToggle24Hour = alarmsViewModel::setUse24Hour,
            onChangeReRingSeconds = alarmsViewModel::setDefaultReRingSeconds,
            onOpenAppInfo = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { context.startActivity(intent) }
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WakeProofSettingsDialog(
    use24Hour: Boolean,
    defaultReRingSeconds: Int,
    onToggle24Hour: (Boolean) -> Unit,
    onChangeReRingSeconds: (Int) -> Unit,
    onOpenAppInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WakeProof Settings & Guide") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("24-Hour Clock Format", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = use24Hour, onCheckedChange = onToggle24Hour)
                }

                Column {
                    Text(
                        text = "Default Re-Ring Loop Interval",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30s", 60 to "1 min", 120 to "2 min", 180 to "3 min")
                            .forEach { (sec, label) ->
                                FilterChip(
                                    selected = defaultReRingSeconds == sec,
                                    onClick = { onChangeReRingSeconds(sec) },
                                    label = { Text(label) }
                                )
                            }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("How Awake Proof Works", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "1. Create an alarm and copy your Awake Proof phrase.\n2. When it rings, swiping or pressing volume silences it for only 1 minute.\n3. After 1 minute it rings again until you enter & submit the Awake Proof text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Battery & Lock-Screen Settings")
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
