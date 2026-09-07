package ni.shikatu.pgupsd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.LessonKind
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.WeekParity
import ni.shikatu.pgupsd.ui.motion.sharedLessonBounds
import ni.shikatu.pgupsd.ui.motion.sharedLessonTitle
import ni.shikatu.pgupsd.ui.ruShort
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                    contentColor = scheme.onPrimaryContainer,
                ) {
                    Text(
                        text = badge.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Text(
                    text = "${pairOrdinal(pairNumber)} пара · ${formatRange(lesson.start.hour, lesson.start.minute, lesson.end.hour, lesson.end.minute)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.sharedLessonTitle(occurrence.key),
                )
                val (kindBg, kindFg) = kindContainer(lesson.kind)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InfoChip(lesson.kind.ruLabel(), container = kindBg, content = kindFg)
                    if (lesson.rooms.isNotEmpty()) {
                        InfoChip(
                            text = "ауд. ${lesson.rooms.joinToString()}",
                            icon = Icons.Outlined.MeetingRoom,
                            container = scheme.tertiaryContainer,
                            content = scheme.onTertiaryContainer,
                        )
                    }
                    if (lesson.subgroup != null) {
                        InfoChip("п/г ${lesson.subgroup}", container = scheme.surfaceContainerHighest, content = scheme.onSurface)
                    }
                    if (lesson.parity != WeekParity.BOTH) {
                        InfoChip(
                            if (lesson.parity == WeekParity.ODD) "нечётная" else "чётная",
                            container = scheme.surfaceContainerHighest,
                            content = scheme.onSurface,
                        )
                    }
                    if (lesson.valid != null) {
                        InfoChip(
                            lesson.valid.ruShort(),
                            container = scheme.tertiaryContainer,
                            content = scheme.onTertiaryContainer,
                        )
                    }
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
    index: Int,
    count: Int,
    muted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = occurrence.lesson
    val scheme = MaterialTheme.colorScheme
    val (leadBox, leadOn) = kindContainer(lesson.kind)
    val shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape
    val teacher = lesson.teachers.joinToString { it.displayName }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (muted) Modifier.alpha(0.55f) else Modifier)
            .sharedLessonBounds(occurrence.key),
        shape = shape,
        color = if (muted) scheme.surfaceContainerLow else scheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = leadBox,
                    contentColor = leadOn,
                    modifier = Modifier.width(72.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(pairOrdinal(pairNumber), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            formatClock(lesson.start.hour, lesson.start.minute),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(formatClock(lesson.end.hour, lesson.end.minute), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SubjectWithRoom(
                        subject = lesson.subject,
                        rooms = lesson.rooms,
                        parity = lesson.parity,
                        titleModifier = Modifier.sharedLessonTitle(occurrence.key),
                    )
                    Text(
                        text = metaLine(lesson),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (lesson.valid != null) {
                        Text(
                            text = "с ${lesson.valid.ruShort()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.tertiary,
                            maxLines = 1,
                        )
                    }
                    if (teacher.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = scheme.onSurfaceVariant,
                            )
                            Text(
                                text = teacher,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
        scheme.outlineVariant
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
                        start = if (compact) 12.dp else 20.dp,
                        end = if (compact) 12.dp else 20.dp,
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
    val label = "слежу"
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
private fun SubjectWithRoom(
    subject: String,
    rooms: List<String>,
    parity: WeekParity,
    titleModifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = subject,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier,
            )
        }
        if (parity != WeekParity.BOTH) {
            ParityMark(parity)
        }
        if (rooms.isNotEmpty()) {
            Text(
                text = rooms.joinToString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun ParityMark(parity: WeekParity) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surfaceContainerHighest,
        contentColor = scheme.onSurfaceVariant,
    ) {
        Text(
            text = if (parity == WeekParity.ODD) "нечёт" else "чёт",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun InfoChip(
    text: String,
    icon: ImageVector? = null,
    container: Color,
    content: Color,
) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun kindContainer(kind: LessonKind): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (kind) {
        LessonKind.LECTURE -> scheme.primaryContainer to scheme.onPrimaryContainer
        LessonKind.PRACTICE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        LessonKind.LAB -> scheme.secondaryContainer to scheme.onSecondaryContainer
        LessonKind.OTHER -> scheme.surfaceContainerHighest to scheme.onSurface
    }
}

private fun metaLine(lesson: Lesson): String = buildList {
    add(lesson.kind.ruLabel())
    if (lesson.subgroup != null) add("п/г ${lesson.subgroup}")
}.joinToString(" · ")

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
