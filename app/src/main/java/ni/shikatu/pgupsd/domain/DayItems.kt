package ni.shikatu.pgupsd.domain

import java.time.LocalTime
import java.time.temporal.ChronoUnit

sealed class DayItem {
    data class Pair(val occurrence: Occurrence) : DayItem()
    data class Window(val start: LocalTime, val end: LocalTime) : DayItem() {
        val minutes: Long get() = ChronoUnit.MINUTES.between(start, end)
    }
}

fun dayItems(
    occurrences: List<Occurrence>,
    previous: Occurrence? = null,
): List<DayItem> {
    val rows = ArrayList<DayItem>(occurrences.size)
    var last = previous
    for (occurrence in occurrences) {
        if (last != null && skippedPairSlot(last, occurrence)) {
            rows += DayItem.Window(last.lesson.end, occurrence.lesson.start)
        }
        rows += DayItem.Pair(occurrence)
        last = laterSlot(last, occurrence)
    }
    return rows
}

fun currentWindow(occurrences: List<Occurrence>, now: LocalTime): DayItem.Window? {
    if (occurrences.any { occ ->
            !now.isBefore(occ.lesson.start) && !now.isAfter(occ.lesson.end)
        }
    ) {
        return null
    }
    val previous = occurrences
        .filter { now.isAfter(it.lesson.end) }
        .maxWithOrNull(slotComparator)
        ?: return null
    val next = occurrences
        .filter { it.lesson.start.isAfter(now) }
        .minWithOrNull(slotComparator)
        ?: return null
    if (!skippedPairSlot(previous, next)) return null
    return DayItem.Window(previous.lesson.end, next.lesson.start)
}

private fun skippedPairSlot(previous: Occurrence, next: Occurrence): Boolean =
    pairNumber(next.lesson.start) > pairNumber(previous.lesson.start) + 1

private fun laterSlot(current: Occurrence?, candidate: Occurrence): Occurrence {
    if (current == null) return candidate
    return if (slotComparator.compare(candidate, current) >= 0) candidate else current
}

private val slotComparator = compareBy<Occurrence>(
    { pairNumber(it.lesson.start) },
    { it.lesson.end },
)
