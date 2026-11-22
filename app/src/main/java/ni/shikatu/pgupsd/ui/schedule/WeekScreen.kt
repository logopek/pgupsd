package ni.shikatu.pgupsd.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import ni.shikatu.pgupsd.domain.mondayOf
import ni.shikatu.pgupsd.domain.pairNumber
import ni.shikatu.pgupsd.ui.components.FeaturedLessonCard
import ni.shikatu.pgupsd.ui.components.LessonCard
import ni.shikatu.pgupsd.ui.formatDurationMinutes
import ni.shikatu.pgupsd.ui.formatRange
import ni.shikatu.pgupsd.ui.ru
import ni.shikatu.pgupsd.ui.ruFull
import ni.shikatu.pgupsd.ui.ruLong
import ni.shikatu.pgupsd.ui.ruShort
import ni.shikatu.pgupsd.ui.theme.Eyebrow
import ni.shikatu.pgupsd.ui.theme.cardSurface
import ni.shikatu.pgupsd.ui.theme.paperBorder
import ni.shikatu.pgupsd.ui.theme.paperSubtle
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

    Column(modifier = modifier.fillMaxSize()) {
        DayHeader(
            pagerState = pagerState,
            origin = origin,
            today = today,
            onDateChange = onDateChange,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            beyondViewportPageCount = 1,
            key = { page -> dateOf(page, origin).toString() },
        ) { page ->
            val pageDate = dateOf(page, origin)
            DayLessons(
                date = pageDate,
                occurrences = occurrencesFor(pageDate),
                isToday = pageDate == today,
                onOpen = onOpen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DayHeader(
    pagerState: PagerState,
    origin: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    val date = dateOf(pagerState.currentPage, origin)
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(
                text = "${isoWeekNumber(date)}-я неделя · ${isoWeekParity(date).ru()}",
                modifier = Modifier.weight(1f),
            )
            val showReturn = date != today
            val returnAlpha by animateFloatAsState(
                targetValue = if (showReturn) 1f else 0f,
                animationSpec = tween(160),
                label = "return-today",
            )
            val returnToday = Modifier
                .alpha(returnAlpha)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (showReturn) {
                        Modifier.clickable { onDateChange(today) }
                    } else {
                        Modifier.clearAndSetSemantics {}
                    },
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
            Text(
                text = "К сегодня",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = returnToday,
            )
        }
        DayTitle(date = date)
        WeekStrip(selected = date, today = today, onSelect = onDateChange)
    }
}

@Composable
private fun DayTitle(date: LocalDate) {
    val shift = with(LocalDensity.current) { 20.dp.roundToPx() }
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = 24.sp,
        lineHeight = 28.sp,
    )
    AnimatedContent(
        targetState = date,
        transitionSpec = {
            val forward = targetState.isAfter(initialState)
            val enterFrom = if (forward) shift else -shift
            val exitTo = if (forward) -shift else shift
            (
                fadeIn(tween(240)) + slideInHorizontally(tween(240)) { enterFrom }
            ) togetherWith (
                fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { exitTo }
            ) using SizeTransform(clip = false) { _, _ -> snap() }
        },
        contentAlignment = Alignment.TopStart,
        label = "day-title",
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = shown.dayOfWeek.ruFull(),
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
            Text(
                text = shown.ruLong(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun WeekStrip(
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val start = mondayOf(selected)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (offset in 0..6) {
            val day = start.plusDays(offset.toLong())
            DayChip(
                day = day,
                selected = day == selected,
                today = day == today,
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayChip(
    day: LocalDate,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val labelColor = when {
        selected -> scheme.onPrimary.copy(alpha = 0.72f)
        today -> scheme.primary
        else -> paperSubtle()
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primary else cardSurface(),
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
        border = if (selected) null else paperBorder(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.dayOfWeek.ruShort().lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (occurrences.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = cardSurface(),
                    border = paperBorder(),
                ) {
                    Text(
                        text = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                            "Воскресенье — пар нет"
                        } else {
                            "В этот день пар нет"
                        },
                        style = MaterialTheme.typography.titleLarge,
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
                )
            }
        } else if (isToday && split.past.isNotEmpty() && split.upcoming.isEmpty()) {
            item {
                Text(
                    text = "На сегодня пары закончились",
                    style = MaterialTheme.typography.titleLarge,
                    color = paperSubtle(),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
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
        Eyebrow(
            text = title,
            modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
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
    val minutes = java.time.temporal.ChronoUnit.MINUTES.between(start, end)
    val remaining = java.time.temporal.ChronoUnit.MINUTES.between(now, end).coerceAtLeast(0)
    val label = if (live) "Сейчас окно" else "Окно"
    val extra = if (live) "ещё ${formatDurationMinutes(remaining)}" else formatDurationMinutes(minutes)
    Text(
        text = "$label · ${formatRange(start, end)} · $extra",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .then(if (muted) Modifier.alpha(0.7f) else Modifier),
        style = MaterialTheme.typography.bodySmall,
        color = if (live) MaterialTheme.colorScheme.primary else paperSubtle(),
        textAlign = TextAlign.Center,
    )
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
