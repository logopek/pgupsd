package ni.shikatu.pgupsd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.LessonKind
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.WeekParity
import ni.shikatu.pgupsd.ui.motion.sharedLessonBounds
import ni.shikatu.pgupsd.ui.motion.sharedLessonTitle
import ni.shikatu.pgupsd.ui.ruShort
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.toneLab
import ni.shikatu.pgupsd.ui.theme.toneLecture
import ni.shikatu.pgupsd.ui.theme.toneMuted
import ni.shikatu.pgupsd.ui.theme.tonePractice
import java.time.LocalDate

@Composable
fun FeaturedLessonCard(
    occurrence: Occurrence,
    pairNumber: Int,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = occurrence.lesson
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .sharedLessonBounds(occurrence.key),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Eyebrow(
                    text = "$badge · ${pairOrdinal(pairNumber)} пара",
                    color = scheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
                Text(
                    text = formatRange(lesson.start.hour, lesson.start.minute, lesson.end.hour, lesson.end.minute),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.sharedLessonTitle(occurrence.key),
                )
                val meta = lessonMeta(lesson)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
                val teacher = lesson.teachers.joinToString { it.displayName }
                if (teacher.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(teacher, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            CardNotes(
                localText = occurrence.localNote?.text,
                deadline = occurrence.localNote?.deadline == true,
                deadlineOverdue = occurrence.date.isBefore(LocalDate.now()),
                globalTexts = occurrence.globalNotes.map { it.text },
                compact = false,
            )
        }
    }
}

@Composable
fun LessonCard(
    occurrence: Occurrence,
    pairNumber: Int,
    muted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = occurrence.lesson
    val scheme = MaterialTheme.colorScheme
    val teacher = lesson.teachers.joinToString { it.displayName }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (muted) Modifier.alpha(0.7f) else Modifier)
            .sharedLessonBounds(occurrence.key),
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        contentColor = scheme.onSurface,
        border = paperBorder(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                LessonTime(
                    pairNumber = pairNumber,
                    lesson = lesson,
                    labelColor = scheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 58.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = lesson.subject,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .sharedLessonTitle(occurrence.key),
                        )
                        if (lesson.rooms.isNotEmpty()) {
                            Text(
                                text = lesson.rooms.joinToString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        KindBadge(lesson.kind)
                        if (lesson.parity != WeekParity.BOTH) {
                            QuietBadge(if (lesson.parity == WeekParity.ODD) "нечёт" else "чёт")
                        }
                        if (lesson.subgroup != null) {
                            Text(
                                text = "п/г ${lesson.subgroup}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (lesson.valid != null) {
                        Text(
                            text = "с ${lesson.valid.ruShort()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.tertiary,
                            maxLines = 1,
                        )
                    }
                    if (teacher.isNotBlank()) {
                        Text(
                            text = teacher,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            CardNotes(
                localText = occurrence.localNote?.text,
                deadline = occurrence.localNote?.deadline == true,
                deadlineOverdue = occurrence.date.isBefore(LocalDate.now()),
                globalTexts = occurrence.globalNotes.map { it.text },
                compact = true,
            )
        }
    }
}

private data class NoteLine(val text: String, val local: Boolean, val deadline: Boolean = false, val overdue: Boolean = false)

@Composable
private fun CardNotes(
    localText: String?,
    deadline: Boolean,
    deadlineOverdue: Boolean,
    globalTexts: List<String>,
    compact: Boolean,
) {
    val lines = buildList {
        val local = localText?.trim().orEmpty()
        if (local.isNotEmpty() || deadline) {
            add(NoteLine(local, local = true, deadline = deadline, overdue = deadlineOverdue))
        }
        globalTexts.map { it.trim() }.filter { it.isNotEmpty() }.forEach { text ->
            add(NoteLine(text, local = false))
        }
    }
    if (lines.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val divider = if (compact) {
        scheme.outline
    } else {
        scheme.onPrimaryContainer.copy(alpha = 0.18f)
    }
    val iconTint = if (compact) scheme.onSurfaceVariant else scheme.onPrimaryContainer
    val textStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            HorizontalDivider(color = divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (compact) 14.dp else 20.dp,
                        end = if (compact) 14.dp else 20.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (line.local) Icons.AutoMirrored.Outlined.Notes else Icons.Outlined.School,
                    contentDescription = if (line.local) "Заметка к паре" else "Заметка по предмету",
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                    tint = iconTint,
                )
                if (line.text.isNotEmpty()) {
                    Text(
                        text = line.text,
                        style = textStyle,
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (line.deadline) {
                    DueMark(overdue = line.overdue, compact = compact)
                }
            }
        }
    }
}

@Composable
private fun DueMark(overdue: Boolean, compact: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val container = when {
        overdue -> scheme.errorContainer
        compact -> scheme.tertiaryContainer
        else -> scheme.onPrimaryContainer.copy(alpha = 0.12f)
    }
    val content = when {
        overdue -> scheme.onErrorContainer
        compact -> scheme.onTertiaryContainer
        else -> scheme.onPrimaryContainer
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = "Отслеживание",
            modifier = Modifier
                .padding(horizontal = 7.dp, vertical = 5.dp)
                .size(if (compact) 14.dp else 16.dp),
        )
    }
}

@Composable
private fun LessonTime(
    pairNumber: Int,
    lesson: Lesson,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = pairOrdinal(pairNumber),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = formatClock(lesson.start.hour, lesson.start.minute),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = formatClock(lesson.end.hour, lesson.end.minute),
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun KindBadge(kind: LessonKind) {
    val (bg, fg) = kindTone(kind)
    Surface(shape = RoundedCornerShape(50), color = bg, contentColor = fg) {
        Text(
            text = kind.badgeLabel(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun QuietBadge(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surfaceVariant,
        contentColor = scheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun kindTone(kind: LessonKind): Pair<Color, Color> = when (kind) {
    LessonKind.LECTURE -> toneLecture()
    LessonKind.PRACTICE -> tonePractice()
    LessonKind.LAB -> toneLab()
    LessonKind.OTHER -> toneMuted()
}

private fun lessonMeta(lesson: Lesson): String = buildList {
    add(lesson.kind.badgeLabel())
    if (lesson.rooms.isNotEmpty()) add("ауд. ${lesson.rooms.joinToString()}")
    if (lesson.subgroup != null) add("п/г ${lesson.subgroup}")
    if (lesson.parity != WeekParity.BOTH) {
        add(if (lesson.parity == WeekParity.ODD) "нечётная" else "чётная")
    }
    if (lesson.valid != null) add("с ${lesson.valid.ruShort()}")
}.joinToString(" · ")

private fun LessonKind.badgeLabel(): String = when (this) {
    LessonKind.LECTURE -> "лекция"
    LessonKind.PRACTICE -> "практика"
    LessonKind.LAB -> "лабораторная"
    LessonKind.OTHER -> "занятие"
}

internal fun LessonKind.ruLabel(): String = when (this) {
    LessonKind.LECTURE -> "Лекция"
    LessonKind.PRACTICE -> "Практика"
    LessonKind.LAB -> "Лабораторная"
    LessonKind.OTHER -> "Занятие"
}

internal fun formatClock(hour: Int, minute: Int): String =
    String.format("%02d:%02d", hour, minute)

internal fun formatRange(sh: Int, sm: Int, eh: Int, em: Int): String =
    "${formatClock(sh, sm)}–${formatClock(eh, em)}"

internal fun pairOrdinal(n: Int): String = "$n-я"
