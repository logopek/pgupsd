package ni.shikatu.pgupsd.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ni.shikatu.pgupsd.domain.normalizeKey
import ni.shikatu.pgupsd.ui.motion.LocalNavScope
import ni.shikatu.pgupsd.ui.motion.LocalSharedTransition
import ni.shikatu.pgupsd.ui.motion.backEnter
import ni.shikatu.pgupsd.ui.motion.backExit
import ni.shikatu.pgupsd.ui.motion.containerEnter
import ni.shikatu.pgupsd.ui.motion.containerExit
import ni.shikatu.pgupsd.ui.motion.forwardEnter
import ni.shikatu.pgupsd.ui.motion.forwardExit
import ni.shikatu.pgupsd.ui.onboarding.OnboardingScreen
import ni.shikatu.pgupsd.ui.onboarding.OnboardingViewModel
import ni.shikatu.pgupsd.ui.schedule.LessonDetailScreen
import ni.shikatu.pgupsd.ui.settings.SettingsScreen
import ni.shikatu.pgupsd.ui.teachers.TeacherDetailScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppNav(
    factory: ViewModelProvider.Factory,
    pendingLessonKey: String? = null,
    onPendingConsumed: () -> Unit = {},
) {
    val diary: DiaryViewModel = viewModel(factory = factory)
    val state by diary.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (state.loading) {
            CircularWavyProgressIndicator(Modifier.align(Alignment.Center))
            return
        }

        val start = if (state.onboarded) "home" else "onboarding"
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalSharedTransition provides this) {
                NavHost(
                    navController = nav,
                    startDestination = start,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    enterTransition = { forwardEnter() },
                    exitTransition = { forwardExit() },
                    popEnterTransition = { backEnter() },
                    popExitTransition = { backExit() },
                ) {
                    appScreen("onboarding") {
                        val vm: OnboardingViewModel = viewModel(factory = factory)
                        OnboardingScreen(vm)
                        LaunchedEffect(state.onboarded) {
                            if (state.onboarded) {
                                nav.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        }
                    }
                    appScreen("home") {
                        HomeScreen(
                            state = state,
                            viewModel = diary,
                            onOpenLesson = { occ ->
                                nav.navigate("lesson/${android.net.Uri.encode(occ.key)}")
                            },
                            onOpenTeacher = { card ->
                                nav.navigate("teacher/${android.net.Uri.encode(card.teacher.key)}")
                            },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    appScreen(
                        route = "lesson/{key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType }),
                        enter = { containerEnter() },
                        exit = { containerExit() },
                        popEnter = { containerEnter() },
                        popExit = { containerExit() },
                    ) { entry ->
                        val key = android.net.Uri.decode(entry.arguments?.getString("key").orEmpty())
                        val occurrence = findOccurrence(state, diary, key)
                        if (occurrence == null) {
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            LessonDetailScreen(
                                occurrence = occurrence,
                                globalNotes = state.globalNotes,
                                onBack = { nav.popBackStack() },
                                onSaveLocal = { diary.saveLocal(occurrence, it) },
                                onSaveDeadline = { diary.saveLocalDeadline(occurrence, it) },
                                onOpenTeacher = { teacher ->
                                    nav.navigate("teacher/${android.net.Uri.encode(teacher.key)}")
                                },
                                onSaveGlobal = { teacher, text ->
                                    diary.saveGlobal(
                                        subjectKey = occurrence.lesson.subjectKey,
                                        teacherKey = teacher.key,
                                        subjectLabel = occurrence.lesson.subject,
                                        teacherLabel = teacher.displayName,
                                        text = text,
                                    )
                                },
                            )
                        }
                    }
                    appScreen(
                        route = "teacher/{key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType }),
                    ) { entry ->
                        val key = android.net.Uri.decode(entry.arguments?.getString("key").orEmpty())
                        val card = diary.teachers(state).find { it.teacher.key == key }
                        if (card == null) {
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            TeacherDetailScreen(
                                card = card,
                                globalNotes = state.globalNotes,
                                onBack = { nav.popBackStack() },
                                onLoadDetails = diary::loadStaffDetails,
                                onSave = { subject, text ->
                                    diary.saveGlobal(
                                        subjectKey = normalizeKey(subject),
                                        teacherKey = card.teacher.key,
                                        subjectLabel = subject,
                                        teacherLabel = card.displayName,
                                        text = text,
                                    )
                                },
                            )
                        }
                    }
                    appScreen("settings") {
                        var groups by remember { mutableStateOf(emptyList<String>()) }
                        LaunchedEffect(state.profile?.facultyCode, state.profile?.course) {
                            groups = diary.storedGroups()
                        }
                        SettingsScreen(
                            state = state,
                            groups = groups,
                            onBack = { nav.popBackStack() },
                            onChangeGroup = diary::changeGroup,
                            onRefresh = { diary.refresh(true) },
                            onReset = {
                                diary.reset()
                                nav.navigate("onboarding") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(state.onboarded) {
        val current = nav.currentDestination?.route
        if (!state.loading && !state.onboarded && current != "onboarding" && current != null) {
            nav.navigate("onboarding") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(pendingLessonKey, state.loading, state.onboarded) {
        val key = pendingLessonKey ?: return@LaunchedEffect
        if (state.loading || !state.onboarded) return@LaunchedEffect
        nav.navigate("lesson/${android.net.Uri.encode(key)}") {
            launchSingleTop = true
            popUpTo("home") { inclusive = false }
        }
        onPendingConsumed()
    }
}

private fun NavGraphBuilder.appScreen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    enter: androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition? = { forwardEnter() },
    exit: androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition? = { forwardExit() },
    popEnter: androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition? = { backEnter() },
    popExit: androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition? = { backExit() },
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) { entry ->
        CompositionLocalProvider(LocalNavScope provides this) {
            content(entry)
        }
    }
}

private fun findOccurrence(state: DiaryUiState, diary: DiaryViewModel, key: String): ni.shikatu.pgupsd.domain.Occurrence? {
    val epoch = key.substringBefore('|').toLongOrNull() ?: return null
    val date = LocalDate.ofEpochDay(epoch)
    return diary.occurrences(date, state).find { it.key == key }
}
