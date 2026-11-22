package ni.shikatu.pgupsd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun toneLecture(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.lectureBgDark to Ink.lectureDark else Ink.lectureBg to Ink.lecture
}

@Composable
fun tonePractice(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.passBgDark to Ink.passDark else Ink.passBg to Ink.pass
}

@Composable
fun toneLab(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.workBgDark to Ink.workDark else Ink.workBg to Ink.work
}

@Composable
fun toneExam(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.examBgDark to Ink.examDark else Ink.examBg to Ink.exam
}

@Composable
fun tonePass(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.passBgDark to Ink.passDark else Ink.passBg to Ink.pass
}

@Composable
fun toneWork(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return if (dark) Ink.workBgDark to Ink.workDark else Ink.workBg to Ink.work
}

@Composable
fun toneMuted(): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return (if (dark) Ink.surface2Dark else Ink.surface2) to (if (dark) Ink.mutedDark else Ink.muted)
}

@Composable
fun toneTrack(): Color = if (isSystemInDarkTheme()) Ink.surface2Dark else Ink.surface2

@Composable
fun toneDoneMark(): Pair<Color, Color> = Ink.pass to Ink.primaryFg

@Composable
fun barLecture(): Color = if (isSystemInDarkTheme()) Ink.primaryDark else Ink.primary

@Composable
fun barLab(): Color = if (isSystemInDarkTheme()) Ink.lectureDark else Ink.accent

@Composable
fun barPractice(): Color = if (isSystemInDarkTheme()) Ink.passDark else Ink.pass

@Composable
fun barWork(): Color = if (isSystemInDarkTheme()) Ink.workDark else Ink.work

@Composable
fun barSelf(): Color = if (isSystemInDarkTheme()) Ink.subtleDark else Ink.border

@Composable
fun barControl(): Color = if (isSystemInDarkTheme()) Ink.examDark else Ink.exam
