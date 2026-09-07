package ni.shikatu.pgupsd.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private val SpatialSpring = spring<Rect>(
    dampingRatio = 0.86f,
    stiffness = 380f,
)

private val TitleSpring = spring<Rect>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 520f,
)

private val SlideSpring = spring(
    dampingRatio = 0.9f,
    stiffness = 420f,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60)) +
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, SlideSpring)

fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition =
    fadeOut(tween(140)) +
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            SlideSpring,
        ) { it / 8 }

fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60)) +
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, SlideSpring)

fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition =
    fadeOut(tween(160)) +
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, SlideSpring)

fun containerEnter(): EnterTransition = fadeIn(tween(160, delayMillis = 30))

fun containerExit(): ExitTransition = fadeOut(tween(140))

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedLessonBounds(id: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val vis = LocalNavScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "lesson-$id"),
            animatedVisibilityScope = vis,
            boundsTransform = { _, _ -> SpatialSpring },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedLessonTitle(id: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val vis = LocalNavScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "lesson-title-$id"),
            animatedVisibilityScope = vis,
            boundsTransform = { _, _ -> TitleSpring },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopStart,
            ),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedTeacherBounds(id: String): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val vis = LocalNavScope.current ?: return this
    return with(shared) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = "teacher-$id"),
            animatedVisibilityScope = vis,
            boundsTransform = { _, _ -> SpatialSpring },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

fun tabEnter(forward: Boolean): EnterTransition {
    val dir = if (forward) 1 else -1
    return fadeIn(tween(220)) + slideInHorizontally(SlideSpring) { dir * it / 6 }
}

fun tabExit(forward: Boolean): ExitTransition {
    val dir = if (forward) 1 else -1
    return fadeOut(tween(140)) + slideOutHorizontally(SlideSpring) { -dir * it / 6 }
}
