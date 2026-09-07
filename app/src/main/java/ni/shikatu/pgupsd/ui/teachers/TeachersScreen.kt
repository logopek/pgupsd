package ni.shikatu.pgupsd.ui.teachers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import ni.shikatu.pgupsd.domain.GlobalNote
import ni.shikatu.pgupsd.domain.TeacherCard
import ni.shikatu.pgupsd.domain.normalizeKey
import ni.shikatu.pgupsd.ui.dialPhone
import ni.shikatu.pgupsd.ui.motion.sharedTeacherBounds
import ni.shikatu.pgupsd.ui.sendEmail

@Composable
fun TeachersScreen(
    teachers: List<TeacherCard>,
    onOpen: (TeacherCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, teachers) {
        val q = query.trim()
        if (q.isEmpty()) teachers
        else teachers.filter { card ->
            card.displayName.contains(q, true) ||
                card.teacher.name.contains(q, true) ||
                card.subjects.any { it.contains(q, true) } ||
                card.staff?.department.orEmpty().contains(q, true)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Преподаватели", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Карточки с сайта ПГУПС · из твоего расписания",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Имя, кафедра, предмет") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = if (teachers.isEmpty()) "В расписании пока нет преподавателей" else "Никого не нашлось",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
        items(filtered, key = { it.teacher.key }) { card ->
            TeacherListCard(card = card, onClick = { onOpen(card) })
        }
    }
}

@Composable
private fun TeacherListCard(
    card: TeacherCard,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val staff = card.staff
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .sharedTeacherBounds(card.teacher.key),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeacherPhoto(name = card.displayName, url = staff?.photoUrl, size = 72)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = card.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = staff?.department ?: card.subjects.firstOrNull()
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val chips = listOfNotNull(staff?.degree, staff?.rank)
                if (chips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        chips.forEach { MetaPill(it) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TeacherPhoto(
    name: String,
    url: String?,
    size: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier.size(size.dp),
        shape = shape,
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = name,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surfaceContainerHighest,
        contentColor = scheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TeacherDetailScreen(
    card: TeacherCard,
    globalNotes: List<GlobalNote>,
    onBack: () -> Unit,
    onSave: (subject: String, text: String) -> Unit,
    onLoadDetails: (slug: String) -> Unit = {},
) {
    val staff = card.staff
    val context = LocalContext.current
    LaunchedEffect(staff?.slug) {
        val slug = staff?.slug ?: return@LaunchedEffect
        if (staff.disciplines.isEmpty()) onLoadDetails(slug)
    }
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.sharedTeacherBounds(card.teacher.key),
        topBar = {
            TopAppBar(
                title = { Text("Преподаватель") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        TeacherPhoto(name = card.displayName, url = staff?.photoUrl, size = 96)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = card.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (staff == null) {
                                Text(
                                    text = card.teacher.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            val chips = listOfNotNull(staff?.degree, staff?.rank)
                            if (chips.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    chips.forEach { chip ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                                            contentColor = scheme.onPrimaryContainer,
                                        ) {
                                            Text(
                                                text = chip,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val posts = staff?.posts.orEmpty()
            if (posts.isNotEmpty()) {
                item {
                    InfoBlock(title = "Должность") {
                        posts.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }

            val contacts = buildList {
                staff?.phones?.forEach { phone ->
                    add(
                        ContactRowData(
                            icon = Icons.Outlined.Call,
                            value = phone,
                            description = "Позвонить",
                            onClick = { dialPhone(context, phone) },
                        ),
                    )
                }
                staff?.emails?.forEach { email ->
                    add(
                        ContactRowData(
                            icon = Icons.Outlined.MailOutline,
                            value = email,
                            description = "Написать",
                            onClick = { sendEmail(context, email) },
                        ),
                    )
                }
                staff?.office?.let { office ->
                    add(
                        ContactRowData(
                            icon = Icons.Outlined.Place,
                            value = office,
                            description = "Кабинет",
                            onClick = null,
                        ),
                    )
                }
            }
            if (contacts.isNotEmpty()) {
                item {
                    InfoBlock(title = "Контакты") {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (contact.onClick != null) {
                                            Modifier.clickable(onClick = contact.onClick)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    contact.icon,
                                    contentDescription = contact.description,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(contact.value, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            val disciplines = staff?.disciplines.orEmpty()
            if (disciplines.isNotEmpty()) {
                item {
                    InfoBlock(title = "Преподаёт") {
                        Text(disciplines.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (card.subjects.isNotEmpty() || card.rooms.isNotEmpty()) {
                item {
                    InfoBlock(
                        title = "В твоём расписании",
                        icon = Icons.Outlined.School,
                    ) {
                        if (card.subjects.isNotEmpty()) {
                            Text(card.subjects.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                        }
                        if (card.rooms.isNotEmpty()) {
                            Text("Ауд. ${card.rooms.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (card.subjects.isNotEmpty()) {
                item {
                    Text(
                        "Заметки по предмету",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(card.subjects, key = { it }) { subject ->
                    val existing = globalNotes.find {
                        it.teacherKey == card.teacher.key &&
                            it.subjectKey == normalizeKey(subject)
                    }
                    SubjectNoteField(
                        subject = subject,
                        initial = existing?.text.orEmpty(),
                        onSave = { onSave(subject, it) },
                    )
                }
            }
        }
    }
}

private data class ContactRowData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val value: String,
    val description: String,
    val onClick: (() -> Unit)?,
)

@Composable
private fun InfoBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.primary)
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun SubjectNoteField(
    subject: String,
    initial: String,
    onSave: (String) -> Unit,
) {
    var text by remember(subject, initial) { mutableStateOf(initial) }
    LaunchedEffect(subject) {
        snapshotFlow { text }.debounce(400).collect(onSave)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
}
