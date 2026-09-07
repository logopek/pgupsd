package ni.shikatu.pgupsd.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MeetingRoom
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
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

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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
    var localText by remember(occurrence.key) { mutableStateOf(occurrence.localNote?.text.orEmpty()) }
    var deadline by remember(occurrence.key) { mutableStateOf(occurrence.localNote?.deadline == true) }

    LaunchedEffect(occurrence.key) {
        snapshotFlow { localText }
            .debounce(400)
            .collect { onSaveLocal(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пара") },
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                        contentColor = scheme.onPrimaryContainer,
                    ) {
                        Text(
                            text = "${occurrence.date.dayOfWeek.ruFull()} · ${occurrence.date.ruLong()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        text = formatRange(lesson.start, lesson.end),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = lesson.subject,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.sharedLessonTitle(occurrence.key),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val chipBg = scheme.onPrimaryContainer.copy(alpha = 0.12f)
                        val chipFg = scheme.onPrimaryContainer
                        MetaChip(lesson.kind.ru(), container = chipBg, content = chipFg)
                        if (lesson.rooms.isNotEmpty()) {
                            MetaChip(
                                text = lesson.rooms.joinToString(),
                                icon = Icons.Outlined.MeetingRoom,
                                container = scheme.tertiaryContainer,
                                content = scheme.onTertiaryContainer,
                            )
                        }
                        if (lesson.subgroup != null) {
                            MetaChip("п/г ${lesson.subgroup}", container = chipBg, content = chipFg)
                        }
                        if (lesson.parity != WeekParity.BOTH) {
                            MetaChip(lesson.parity.ruCapital(), container = chipBg, content = chipFg)
                        }
                    }
                    if (lesson.valid != null) {
                        Text(
                            text = "Только с ${lesson.valid.from} по ${lesson.valid.to}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.tertiary,
                        )
                    }
                }
            }

            if (lesson.teachers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Преподаватель")
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
                    value = localText,
                    onValueChange = { localText = it },
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
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
        color = scheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = teacher.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
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
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun GlobalNoteEditor(
    teacher: Teacher,
    subject: String,
    existing: GlobalNote?,
    onSave: (String) -> Unit,
) {
    var text by remember(teacher.key, subject, existing?.text) {
        mutableStateOf(existing?.text.orEmpty())
    }
    LaunchedEffect(teacher.key, subject) {
        snapshotFlow { text }.debounce(400).collect(onSave)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            teacher.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = MaterialTheme.shapes.large,
            placeholder = { Text("Общая заметка по этому преподавателю") },
        )
    }
}

@Composable
private fun MetaChip(
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
