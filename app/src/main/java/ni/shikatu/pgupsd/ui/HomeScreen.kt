package ni.shikatu.pgupsd.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.TeacherCard
import ni.shikatu.pgupsd.ui.motion.tabEnter
import ni.shikatu.pgupsd.ui.motion.tabExit
import ni.shikatu.pgupsd.ui.notes.DeadlinesScreen
import ni.shikatu.pgupsd.ui.plan.PlanScreen
import ni.shikatu.pgupsd.ui.schedule.WeekScreen
import ni.shikatu.pgupsd.ui.teachers.TeachersScreen
import ni.shikatu.pgupsd.ui.theme.paperSubtle
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

    val subtle = paperSubtle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 20.dp, end = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (state.profile?.groupName ?: "ПГУПС").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtle,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Настройки",
                        tint = subtle,
                    )
                }
            }
        },
        bottomBar = {
            PaperBottomBar(tab = tab, trackedCount = trackedCount, onTab = { tab = it })
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
                    2 -> TeachersScreen(
                        teachers = viewModel.teachers(state),
                        onOpen = onOpenTeacher,
                    )
                    else -> PlanScreen()
                }
            }
        }
    }
}

@Composable
private fun PaperBottomBar(
    tab: Int,
    trackedCount: Int,
    onTab: (Int) -> Unit,
) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = paperSubtle(),
        unselectedTextColor = paperSubtle(),
        indicatorColor = Color.Transparent,
    )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            // The component always reserves the tall label height. Without labels, drop that extra.
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val trim = 16.dp.roundToPx()
                    val height = (placeable.height - trim).coerceAtLeast(0)
                    layout(placeable.width, height) {
                        placeable.place(0, -trim)
                    }
                },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            NavigationBarItem(
                selected = tab == 0,
                onClick = { onTab(0) },
                colors = colors,
                icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Расписание") },
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { onTab(1) },
                colors = colors,
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
                        Icon(Icons.Outlined.Flag, contentDescription = "Отслеживание")
                    }
                },
            )
            NavigationBarItem(
                selected = tab == 2,
                onClick = { onTab(2) },
                colors = colors,
                icon = { Icon(Icons.Outlined.People, contentDescription = "Преподаватели") },
            )
            NavigationBarItem(
                selected = tab == 3,
                onClick = { onTab(3) },
                colors = colors,
                icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = "План") },
            )
        }
    }
}
