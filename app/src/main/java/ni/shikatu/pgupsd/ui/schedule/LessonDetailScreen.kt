package ni.shikatu.pgupsd.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.ui.notes.rememberNoteDraft
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperTopBarColors
import ni.shikatu.pgupsd.domain.GlobalNote
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.Teacher
import ni.shikatu.pgupsd.domain.WeekParity
import ni.shikatu.pgupsd.ui.addLessonToCalendar
import ni.shikatu.pgupsd.ui.formatRange
import ni.shikatu.pgupsd.ui.motion.sharedLessonBounds
import ni.shikatu.pgupsd.ui.motion.sharedLessonTitle
import ni.shikatu.pgupsd.ui.ru
import ni.shikatu.pgupsd.ui.ruCapital
import ni.shikatu.pgupsd.ui.ruFull
import ni.shikatu.pgupsd.ui.ruLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    occurrence: Occurrence,
    globalNotes: List<GlobalNote>,
    onBack: () -> Unit,
    onSaveLocal: (String) -> Unit,
    onSaveDeadline: (Boolean) -> Unit,
    onOpenTeacher: (Teacher) -> Unit,
    onSaveGlobal: (Teacher, String) -> Unit,
) {
    val lesson = occurrence.lesson
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val localDraft = rememberNoteDraft(
        identity = occurrence.key,
        saved = occurrence.localNote?.text.orEmpty(),
        onSave = onSaveLocal,
    )
    var deadline by remember(occurrence.key) { mutableStateOf(occurrence.localNote?.deadline == true) }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = paperTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { addLessonToCalendar(context, occurrence) }) {
                        Icon(Icons.Outlined.Event, contentDescription = "В календарь")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .sharedLessonBounds(occurrence.key),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Eyebrow(
                        text = "${occurrence.date.dayOfWeek.ruFull()} · ${occurrence.date.ruLong()}",
                        color = scheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                    Text(
                        text = formatRange(lesson.start, lesson.end),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = lesson.subject,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.sharedLessonTitle(occurrence.key),
                    )
                    val meta = buildList {
                        add(lesson.kind.ru().lowercase())
                        if (lesson.rooms.isNotEmpty()) add("ауд. ${lesson.rooms.joinToString()}")
                        if (lesson.subgroup != null) add("п/г ${lesson.subgroup}")
                        if (lesson.parity != WeekParity.BOTH) add(lesson.parity.ruCapital().lowercase())
                    }.joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.78f),
                        )
                    }
                    if (lesson.valid != null) {
                        Text(
                            text = "Только с ${lesson.valid.from.ruLong()} по ${lesson.valid.to.ruLong()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            if (lesson.teachers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("Преподаватель", modifier = Modifier.padding(start = 4.dp))
                    lesson.teachers.forEach { teacher ->
                        TeacherRow(teacher = teacher, onClick = { onOpenTeacher(teacher) })
                    }
                }
            }

            NoteCard(
                icon = Icons.AutoMirrored.Outlined.Notes,
                title = "К этой паре",
                subtitle = "Только на ${occurrence.date.ruLong()}",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Отслеживать", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Появится во вкладке «Отслеживание»",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = deadline,
                        onCheckedChange = {
                            deadline = it
                            onSaveDeadline(it)
                        },
                    )
                }
                OutlinedTextField(
                    value = localDraft.text,
                    onValueChange = localDraft.onChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text("Что сдать, что повторить…") },
                )
            }

            if (lesson.teachers.isNotEmpty()) {
                NoteCard(
                    icon = Icons.Outlined.School,
                    title = "По предмету",
                    subtitle = "Для всех пар с этим преподавателем",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        lesson.teachers.forEach { teacher ->
                            GlobalNoteEditor(
                                teacher = teacher,
                                subject = lesson.subject,
                                existing = globalNotes.find {
                                    it.subjectKey == lesson.subjectKey && it.teacherKey == teacher.key
                                },
                                onSave = { onSaveGlobal(teacher, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherRow(
    teacher: Teacher,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        border = paperBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = scheme.surfaceVariant,
                contentColor = scheme.onSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = teacher.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(teacher.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Карточка преподавателя",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cardSurface(),
        border = paperBorder(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun GlobalNoteEditor(
    teacher: Teacher,
    subject: String,
    existing: GlobalNote?,
    onSave: (String) -> Unit,
) {
    val draft = rememberNoteDraft(
        identity = teacher.key to subject,
        saved = existing?.text.orEmpty(),
        onSave = onSave,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            teacher.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = draft.text,
            onValueChange = draft.onChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = MaterialTheme.shapes.medium,
            placeholder = { Text("Общая заметка по этому преподавателю") },
        )
    }
}
