package ni.shikatu.pgupsd.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.TeacherCard
import ni.shikatu.pgupsd.ui.motion.tabEnter
import ni.shikatu.pgupsd.ui.motion.tabExit
import ni.shikatu.pgupsd.ui.notes.DeadlinesScreen
import ni.shikatu.pgupsd.ui.schedule.WeekScreen
import ni.shikatu.pgupsd.ui.teachers.TeachersScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: DiaryUiState,
    viewModel: DiaryViewModel,
    onOpenLesson: (Occurrence) -> Unit,
    onOpenTeacher: (TeacherCard) -> Unit,
    onSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selected = LocalDate.parse(selectedDay)
    val scope = rememberCoroutineScope()
    var pullRefreshing by remember { mutableStateOf(false) }
    val deadlines = remember(state) { viewModel.deadlines(state) }
    val trackedCount = deadlines.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.profile?.groupName ?: "Дневник")
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    label = { Text("Расписание") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (trackedCount > 0) {
                                    Badge(modifier = Modifier.offset(x = 8.dp, y = (-2).dp)) {
                                        Text(if (trackedCount > 99) "99+" else "$trackedCount")
                                    }
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Flag, contentDescription = null)
                        }
                    },
                    label = { Text("Отслеживание") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Outlined.People, contentDescription = null) },
                    label = { Text("Преподаватели") },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                viewModel.refresh(force = false)
                pullRefreshing = true
                scope.launch {
                    delay(450)
                    pullRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val forward = targetState > initialState
                    tabEnter(forward) togetherWith tabExit(forward)
                },
                label = "home-tab",
            ) { current ->
                when (current) {
                    0 -> WeekScreen(
                        date = selected,
                        occurrencesFor = { day -> viewModel.occurrences(day, state) },
                        onDateChange = { selectedDay = it.toString() },
                        onOpen = onOpenLesson,
                    )
                    1 -> DeadlinesScreen(
                        items = deadlines,
                        onOpen = onOpenLesson,
                        onClear = { viewModel.saveLocalDeadline(it, false) },
                    )
                    else -> TeachersScreen(
                        teachers = viewModel.teachers(state),
                        onOpen = onOpenTeacher,
                    )
                }
            }
        }
    }
}
