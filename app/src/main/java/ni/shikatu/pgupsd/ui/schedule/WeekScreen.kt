package ni.shikatu.pgupsd.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.shikatu.pgupsd.domain.DayItem
import ni.shikatu.pgupsd.domain.Occurrence
import ni.shikatu.pgupsd.domain.currentWindow
import ni.shikatu.pgupsd.domain.dayItems
import ni.shikatu.pgupsd.domain.isoWeekNumber
import ni.shikatu.pgupsd.domain.isoWeekParity
import ni.shikatu.pgupsd.domain.pairNumber
import ni.shikatu.pgupsd.ui.components.FeaturedLessonCard
import ni.shikatu.pgupsd.ui.components.LessonCard
import ni.shikatu.pgupsd.ui.formatDurationMinutes
import ni.shikatu.pgupsd.ui.formatRange
import ni.shikatu.pgupsd.ui.ruCapital
import ni.shikatu.pgupsd.ui.ruFull
import ni.shikatu.pgupsd.ui.ruLong
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun WeekScreen(
    date: LocalDate,
    occurrencesFor: (LocalDate) -> List<Occurrence>,
    onDateChange: (LocalDate) -> Unit,
    onOpen: (Occurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val originIso = rememberSaveable { today.toString() }
    val origin = LocalDate.parse(originIso)
    val todayOccurrences = occurrencesFor(today)
    var autoOpenedNext by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(todayOccurrences, date) {
        if (autoOpenedNext || todayOccurrences.isEmpty()) return@LaunchedEffect
        val split = splitDay(todayOccurrences, isToday = true, now = LocalTime.now())
        if (split.featured != null || split.upcoming.isNotEmpty()) return@LaunchedEffect
        autoOpenedNext = true
        if (date == today) onDateChange(today.plusDays(1))
    }

    val pagerState = rememberPagerState(
        initialPage = pageOf(date, origin),
        pageCount = { PagerDayCount },
    )
    LaunchedEffect(date) {
        val target = pageOf(date, origin)
        if (pagerState.currentPage == target) return@LaunchedEffect
        if (kotlin.math.abs(pagerState.currentPage - target) > 1) {
            pagerState.scrollToPage(target)
        } else {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        val swiped = dateOf(pagerState.settledPage, origin)
        if (swiped != date) onDateChange(swiped)
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { page -> dateOf(page, origin).toString() },
        ) { page ->
            val pageDate = dateOf(page, origin)
            Column(modifier = Modifier.fillMaxSize()) {
                DayHeaderCenter(
                    date = pageDate,
                    isToday = pageDate == today,
                    onToday = { onDateChange(today) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 52.dp, top = 2.dp, bottom = 2.dp),
                )
                DayLessons(
                    date = pageDate,
                    occurrences = occurrencesFor(pageDate),
                    isToday = pageDate == today,
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Предыдущий день")
            }
            IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Следующий день")
            }
        }
    }
}

@Composable
private fun DayHeaderCenter(
    date: LocalDate,
    isToday: Boolean,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parity = isoWeekParity(date)
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                onClick = { if (!isToday) onToday() },
                shape = RoundedCornerShape(50),
                color = if (isToday) scheme.secondaryContainer else scheme.primaryContainer,
                contentColor = if (isToday) scheme.onSecondaryContainer else scheme.onPrimaryContainer,
                modifier = Modifier.width(96.dp),
            ) {
                Text(
                    text = if (isToday) "Сегодня" else "К сегодня",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                text = date.dayOfWeek.ruFull(),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f, fill = false),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 14.sp,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                ),
            )
        }
        Text(
            text = "${date.ruLong()} · ${isoWeekNumber(date)}-я, ${parity.ruCapital().lowercase()}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 11.sp,
                maxFontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ),
        )
    }
}

@Composable
private fun DayLessons(
    date: LocalDate,
    occurrences: List<Occurrence>,
    isToday: Boolean,
    onOpen: (Occurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember { LocalTime.now() }
    val split = remember(occurrences, isToday, date) { splitDay(occurrences, isToday, now) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (occurrences.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                            "Воскресенье — пар нет"
                        } else {
                            "В этот день пар нет"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            return@LazyColumn
        }

        val pairNumbers = occurrences.associate { it.key to pairNumber(it.lesson.start) }
        val liveWindow = if (isToday) currentWindow(occurrences, now) else null

        if (liveWindow != null) {
            item(key = "live-window") {
                WindowCard(start = liveWindow.start, end = liveWindow.end, live = true, now = now)
            }
        }

        if (split.featured != null) {
            item(key = "featured-${split.featured.key}") {
                FeaturedLessonCard(
                    occurrence = split.featured,
                    pairNumber = pairNumbers[split.featured.key] ?: 1,
                    badge = split.featuredBadge,
                    onClick = { onOpen(split.featured) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        } else if (isToday && split.past.isNotEmpty() && split.upcoming.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = "На сегодня пары закончились",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (isToday) {
            if (split.upcoming.isNotEmpty()) {
                sectionTitle("Дальше")
                lessonItems(
                    items = split.upcoming,
                    pairNumbers = pairNumbers,
                    muted = false,
                    onOpen = onOpen,
                    previous = split.featured,
                    keyPrefix = "up-",
                )
            }
            if (split.past.isNotEmpty()) {
                sectionTitle("Прошли")
                lessonItems(
                    items = split.past,
                    pairNumbers = pairNumbers,
                    muted = true,
                    onOpen = onOpen,
                    keyPrefix = "past-",
                )
            }
        } else {
            lessonItems(split.upcoming, pairNumbers, muted = false, onOpen)
        }
    }
}

private fun LazyListScope.sectionTitle(title: String) {
    item(key = "section-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.lessonItems(
    items: List<Occurrence>,
    pairNumbers: Map<String, Int>,
    muted: Boolean,
    onOpen: (Occurrence) -> Unit,
    previous: Occurrence? = null,
    keyPrefix: String = "",
) {
    val rows = dayItems(items, previous)
    var index = 0
    while (index < rows.size) {
        when (val row = rows[index]) {
            is DayItem.Window -> {
                item(key = "${keyPrefix}window-${row.start}-${row.end}") {
                    WindowCard(start = row.start, end = row.end, muted = muted)
                }
                index++
            }
            is DayItem.Pair -> {
                val start = index
                while (index < rows.size && rows[index] is DayItem.Pair) index++
                val group = rows.subList(start, index).map { (it as DayItem.Pair).occurrence }
                group.forEachIndexed { i, occ ->
                    item(key = occ.key) {
                        LessonCard(
                            occurrence = occ,
                            pairNumber = pairNumbers[occ.key] ?: (i + 1),
                            index = i,
                            count = group.size,
                            muted = muted,
                            onClick = { onOpen(occ) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowCard(
    start: LocalTime,
    end: LocalTime,
    live: Boolean = false,
    muted: Boolean = false,
    now: LocalTime = LocalTime.now(),
) {
    val scheme = MaterialTheme.colorScheme
    val minutes = java.time.temporal.ChronoUnit.MINUTES.between(start, end)
    val remaining = java.time.temporal.ChronoUnit.MINUTES.between(now, end).coerceAtLeast(0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .then(if (muted) Modifier.alpha(0.55f) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = if (live) scheme.secondaryContainer else scheme.surfaceContainer,
        contentColor = if (live) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (live) "Сейчас окно" else "Окно",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatRange(start, end),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = if (live) "ещё ${formatDurationMinutes(remaining)}" else formatDurationMinutes(minutes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private const val PagerDayCount = 10_000
private const val PagerDayCenter = PagerDayCount / 2

private fun pageOf(date: LocalDate, origin: LocalDate): Int {
    val delta = ChronoUnit.DAYS.between(origin, date)
    return (PagerDayCenter + delta).toInt().coerceIn(0, PagerDayCount - 1)
}

private fun dateOf(page: Int, origin: LocalDate): LocalDate =
    origin.plusDays((page - PagerDayCenter).toLong())

private data class DaySplit(
    val featured: Occurrence?,
    val featuredBadge: String,
    val past: List<Occurrence>,
    val upcoming: List<Occurrence>,
)

private fun splitDay(items: List<Occurrence>, isToday: Boolean, now: LocalTime): DaySplit {
    if (items.isEmpty()) return DaySplit(null, "", emptyList(), emptyList())
    if (!isToday) return DaySplit(null, "", emptyList(), items)

    val current = items.indexOfFirst { occ ->
        !now.isBefore(occ.lesson.start) && !now.isAfter(occ.lesson.end)
    }
    if (current >= 0) {
        return DaySplit(
            featured = items[current],
            featuredBadge = "Сейчас",
            past = items.take(current),
            upcoming = items.drop(current + 1),
        )
    }
    val next = items.indexOfFirst { it.lesson.start.isAfter(now) }
    if (next >= 0) {
        val minutes = now.until(items[next].lesson.start, ChronoUnit.MINUTES)
        return DaySplit(
            featured = items[next],
            featuredBadge = when {
                minutes <= 0 -> "Дальше"
                minutes < 60 -> "Через $minutes мин"
                else -> "Через ${minutes / 60} ч ${minutes % 60} мин"
            },
            past = items.take(next),
            upcoming = items.drop(next + 1),
        )
    }
    return DaySplit(null, "", items, emptyList())
}
