package ni.shikatu.pgupsd.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palette of the study-plan viewer: warm paper, ink navy, and the three
 * attestation tones (exam / pass / work).
 */
internal object Ink {
    val bg = Color(0xFFF3EFE6)
    val bgWarm = Color(0xFFE8E0D0)
    val surface = Color(0xFFFBF8F1)
    val surface2 = Color(0xFFEFE8D8)
    val fg = Color(0xFF1C1915)
    val muted = Color(0xFF6B645A)
    val subtle = Color(0xFF948C80)
    val border = Color(0xFFDDD4C4)
    val primary = Color(0xFF243044)
    val primaryFg = Color(0xFFF3EFE6)
    val accent = Color(0xFF3D4F66)
    val exam = Color(0xFF8C3A32)
    val examBg = Color(0xFFF3E4E0)
    val pass = Color(0xFF355E4E)
    val passBg = Color(0xFFE1ECE6)
    val work = Color(0xFF6B542C)
    val workBg = Color(0xFFEFE4CF)
    val lecture = Color(0xFF243044)
    val lectureBg = Color(0xFFE4E7EE)

    val bgDark = Color(0xFF141210)
    val surfaceDark = Color(0xFF1C1916)
    val surface2Dark = Color(0xFF2A261F)
    val fgDark = Color(0xFFF3EFE6)
    val mutedDark = Color(0xFFB7AEA0)
    val subtleDark = Color(0xFF8F877C)
    val borderDark = Color(0xFF3A342C)
    val primaryDark = Color(0xFFE8E0D0)
    val primaryFgDark = Color(0xFF1C1915)
    val examDark = Color(0xFFF0B2AA)
    val examBgDark = Color(0xFF3D2420)
    val passDark = Color(0xFF9FCBB6)
    val passBgDark = Color(0xFF1E332A)
    val workDark = Color(0xFFE4D2A8)
    val workBgDark = Color(0xFF3A3220)
    val lectureDark = Color(0xFFE4E7EE)
    val lectureBgDark = Color(0xFF2A3140)
}

internal val LightColors = lightColorScheme(
    primary = Ink.primary,
    onPrimary = Ink.primaryFg,
    primaryContainer = Ink.primary,
    onPrimaryContainer = Ink.primaryFg,
    inversePrimary = Ink.accent,
    secondary = Ink.pass,
    onSecondary = Ink.primaryFg,
    secondaryContainer = Ink.passBg,
    onSecondaryContainer = Ink.pass,
    tertiary = Ink.work,
    onTertiary = Ink.primaryFg,
    tertiaryContainer = Ink.workBg,
    onTertiaryContainer = Ink.work,
    background = Ink.bg,
    onBackground = Ink.fg,
    surface = Ink.surface,
    onSurface = Ink.fg,
    surfaceVariant = Ink.surface2,
    onSurfaceVariant = Ink.muted,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink.primary,
    inverseOnSurface = Ink.primaryFg,
    error = Ink.exam,
    onError = Ink.primaryFg,
    errorContainer = Ink.examBg,
    onErrorContainer = Ink.exam,
    outline = Ink.border,
    outlineVariant = Ink.border,
    scrim = Ink.fg,
    surfaceBright = Ink.surface,
    surfaceDim = Ink.bgWarm,
    surfaceContainerLowest = Ink.surface,
    surfaceContainerLow = Ink.surface,
    surfaceContainer = Ink.surface,
    surfaceContainerHigh = Ink.surface,
    surfaceContainerHighest = Ink.surface2,
    primaryFixed = Ink.lectureBg,
    primaryFixedDim = Color(0xFFD5DAE3),
    onPrimaryFixed = Ink.primary,
    onPrimaryFixedVariant = Ink.accent,
    secondaryFixed = Ink.passBg,
    secondaryFixedDim = Color(0xFFD3E4DC),
    onSecondaryFixed = Ink.pass,
    onSecondaryFixedVariant = Color(0xFF2A4C3E),
    tertiaryFixed = Ink.workBg,
    tertiaryFixedDim = Color(0xFFE4D5B8),
    onTertiaryFixed = Ink.work,
    onTertiaryFixedVariant = Color(0xFF4E3D1F),
)

internal val DarkColors = darkColorScheme(
    primary = Ink.primaryDark,
    onPrimary = Ink.primaryFgDark,
    primaryContainer = Ink.primaryDark,
    onPrimaryContainer = Ink.primaryFgDark,
    inversePrimary = Ink.accent,
    secondary = Ink.passDark,
    onSecondary = Ink.primaryFgDark,
    secondaryContainer = Ink.passBgDark,
    onSecondaryContainer = Ink.passDark,
    tertiary = Ink.workDark,
    onTertiary = Ink.primaryFgDark,
    tertiaryContainer = Ink.workBgDark,
    onTertiaryContainer = Ink.workDark,
    background = Ink.bgDark,
    onBackground = Ink.fgDark,
    surface = Ink.surfaceDark,
    onSurface = Ink.fgDark,
    surfaceVariant = Ink.surface2Dark,
    onSurfaceVariant = Ink.mutedDark,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink.primaryFg,
    inverseOnSurface = Ink.primary,
    error = Ink.examDark,
    onError = Ink.primaryFgDark,
    errorContainer = Ink.examBgDark,
    onErrorContainer = Ink.examDark,
    outline = Ink.borderDark,
    outlineVariant = Ink.borderDark,
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A261F),
    surfaceDim = Ink.bgDark,
    surfaceContainerLowest = Color(0xFF100E0C),
    surfaceContainerLow = Ink.bgDark,
    surfaceContainer = Ink.surfaceDark,
    surfaceContainerHigh = Ink.surfaceDark,
    surfaceContainerHighest = Ink.surface2Dark,
    primaryFixed = Ink.lectureBgDark,
    primaryFixedDim = Color(0xFF1E2430),
    onPrimaryFixed = Ink.lectureDark,
    onPrimaryFixedVariant = Ink.primaryDark,
    secondaryFixed = Ink.passBgDark,
    secondaryFixedDim = Color(0xFF162820),
    onSecondaryFixed = Ink.passDark,
    onSecondaryFixedVariant = Ink.passDark,
    tertiaryFixed = Ink.workBgDark,
    tertiaryFixedDim = Color(0xFF2C2618),
    onTertiaryFixed = Ink.workDark,
    onTertiaryFixedVariant = Ink.workDark,
)
