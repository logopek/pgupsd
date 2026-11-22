package ni.shikatu.pgupsd.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.ui.formatRange
import ni.shikatu.pgupsd.ui.ruFull
import ni.shikatu.pgupsd.ui.ruLong
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperSubtle
import java.time.LocalDate

@Composable
fun DeadlinesScreen(
    items: List<Occurrence>,
    onOpen: (Occurrence) -> Unit,
    onClear: (Occurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val overdue = remember(items, today) { items.filter { it.date.isBefore(today) } }
    val todayItems = remember(items, today) { items.filter { it.date == today } }
    val upcoming = remember(items, today) { items.filter { it.date.isAfter(today) } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Отслеживание", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Включи тумблер на паре — она появится здесь",
                style = MaterialTheme.typography.bodyMedium,
                color = paperSubtle(),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        if (items.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = cardSurface(),
                    border = paperBorder(),
                ) {
                    Text(
                        text = "Пока пусто. Открой нужный день, зайди в пару и включи отслеживание.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        } else {
            deadlineSection("Прошли", overdue, onOpen, onClear, overdue = true)
            deadlineSection("Сегодня", todayItems, onOpen, onClear, overdue = false)
            deadlineSection("Дальше", upcoming, onOpen, onClear, overdue = false)
        }
    }
}

private fun LazyListScope.deadlineSection(
    title: String,
    items: List<Occurrence>,
    onOpen: (Occurrence) -> Unit,
    onClear: (Occurrence) -> Unit,
    overdue: Boolean,
) {
    if (items.isEmpty()) return
    item(key = "section-$title") {
        Eyebrow(
            text = title,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        )
    }
    items(items, key = { it.key }) { occ ->
        DeadlineCard(occurrence = occ, overdue = overdue, onOpen = { onOpen(occ) }, onClear = { onClear(occ) })
    }
}

@Composable
private fun DeadlineCard(
    occurrence: Occurrence,
    overdue: Boolean,
    onOpen: () -> Unit,
    onClear: () -> Unit,
) {
    val lesson = occurrence.lesson
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (overdue) scheme.errorContainer else cardSurface(),
        border = if (overdue) null else paperBorder(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (overdue) scheme.errorContainer else scheme.tertiaryContainer,
                contentColor = if (overdue) scheme.onErrorContainer else scheme.onTertiaryContainer,
            ) {
                Icon(
                    Icons.Outlined.Flag,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${occurrence.date.dayOfWeek.ruFull()}, ${occurrence.date.ruLong()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(formatRange(lesson.start, lesson.end))
                        if (lesson.rooms.isNotEmpty()) {
                            append(" · ")
                            append(lesson.rooms.joinToString())
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val note = occurrence.localNote?.text?.trim().orEmpty()
                if (note.isNotEmpty()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = "Не отслеживать")
            }
        }
    }
}
