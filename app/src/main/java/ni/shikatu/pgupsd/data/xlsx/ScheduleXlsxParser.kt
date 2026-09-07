package ni.shikatu.pgupsd.data.xlsx

import ni.shikatu.pgupsd.domain.DateSpan
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.WeekParity
import java.io.InputStream
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalTime

data class ParsedWorkbook(
    val title: String,
    val groups: List<String>,
    val lessons: List<Lesson>,
)

object ScheduleXlsxParser {
    const val REVISION = 2

    private val groupRegex = Regex("""^[А-ЯЁA-Z]{2,8}-\d{2,4}$""")
    private val timeRegex = Regex("""^\s*(\d{1,4}):?(\d{2})?\s*[-–—]\s*(\d{1,4}):?(\d{2})?\s*$""")
    private val days = mapOf(
        "ПОНЕДЕЛЬНИК" to DayOfWeek.MONDAY,
        "ВТОРНИК" to DayOfWeek.TUESDAY,
        "СРЕДА" to DayOfWeek.WEDNESDAY,
        "ЧЕТВЕРГ" to DayOfWeek.THURSDAY,
        "ПЯТНИЦА" to DayOfWeek.FRIDAY,
        "СУББОТА" to DayOfWeek.SATURDAY,
    )

    fun parse(input: InputStream): ParsedWorkbook {
        val grid = XlsxGrid.from(input)
        val title = (1..6).asSequence()
            .map { grid.value(it, 1) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .replace('\n', ' ')
            .trim()

        val header = findHeader(grid) ?: error("Не удалось найти строку с группами в расписании")
        val groups = header.groups
        val footnotes = parseFootnotes(grid)

        val lessons = mutableListOf<Lesson>()
        for (slot in findSlots(grid, header.row)) {
            for ((col, group) in groups) {
                lessons += parseSlotForGroup(grid, slot, col, group, footnotes)
            }
        }
        return ParsedWorkbook(
            title = title,
            groups = groups.map { it.second },
            lessons = uniquifyIds(lessons),
        )
    }

    private data class Header(val row: Int, val groups: List<Pair<Int, String>>)

    private fun findHeader(grid: XlsxGrid): Header? {
        var best: Header? = null
        for (row in 1..grid.maxRow) {
            val found = mutableListOf<Pair<Int, String>>()
            for (col in 3..grid.maxCol) {
                val text = grid.value(row, col).trim().replace(Regex("\\s+"), "")
                if (groupRegex.matches(text)) found += col to text
            }
            if (found.size >= 2 && (best == null || found.size > best.groups.size)) {
                best = Header(row, found)
            }
        }
        return best
    }

    private data class Slot(
        val day: DayOfWeek,
        val start: LocalTime,
        val end: LocalTime,
        val rows: IntRange,
    )

    private fun findSlots(grid: XlsxGrid, headerRow: Int): List<Slot> {
        data class RowInfo(val row: Int, val day: DayOfWeek?, val time: Pair<LocalTime, LocalTime>?)

        var lastDay: DayOfWeek? = null
        val infos = mutableListOf<RowInfo>()
        for (row in (headerRow + 1)..grid.maxRow) {
            // Only the top-left cell of a merge has raw text. Using the merged
            // value here would treat every continuation row as a new day/time slot
            // and duplicate lessons (same pair twice in LazyColumn).
            val dayText = grid.raw(row, 1)
                .trim()
                .uppercase()
                .replace(Regex("\\s+"), "")
            val day = days.entries.firstOrNull { dayText.startsWith(it.key) }?.value
            if (day != null) lastDay = day
            val time = parseTime(grid.raw(row, 2))
            infos += RowInfo(row, if (day != null) lastDay else null, time)
        }

        val slots = mutableListOf<Slot>()
        var i = 0
        while (i < infos.size) {
            val info = infos[i]
            val time = info.time
            if (time == null) {
                i++
                continue
            }
            val day = generateSequence(i) { it - 1 }
                .takeWhile { it >= 0 }
                .mapNotNull { infos[it].day }
                .firstOrNull()
            if (day == null) {
                i++
                continue
            }
            var j = i + 1
            while (j < infos.size && infos[j].time == null && infos[j].day == null) j++
            val endExclusive = if (j < infos.size) infos[j].row else infos.last().row + 1
            slots += Slot(day, time.first, time.second, infos[i].row until endExclusive)
            i = j
        }
        return slots
    }

    private fun parseSlotForGroup(
        grid: XlsxGrid,
        slot: Slot,
        col: Int,
        group: String,
        footnotes: Map<String, DateSpan>,
    ): List<Lesson> {
        val rows = slot.rows.toList()
        if (rows.isEmpty()) return emptyList()

        val merge = grid.mergeAt(rows.first(), col)
        val coversAll = merge != null && rows.all { merge.contains(it, col) }
        val result = mutableListOf<Lesson>()

        fun add(row: Int, parity: WeekParity, raw: String) {
            if (raw.isBlank() || LessonCellParser.isSelfStudy(raw)) return
            for (parsed in LessonCellParser.parse(raw)) {
                val span = parsed.marker?.let { footnotes[it] }
                result += Lesson(
                    id = lessonId(group, slot, parity, parsed),
                    groupName = group,
                    dayOfWeek = slot.day,
                    start = slot.start,
                    end = slot.end,
                    parity = parity,
                    subject = parsed.subject,
                    kind = parsed.kind,
                    kindLabel = parsed.kindLabel,
                    teachers = parsed.teachers,
                    rooms = parsed.rooms,
                    subgroup = parsed.subgroup,
                    valid = span,
                    raw = parsed.raw,
                )
            }
        }

        if (rows.size == 1 || coversAll) {
            add(rows.first(), WeekParity.BOTH, grid.value(rows.first(), col))
            return result
        }

        val top = grid.value(rows[0], col).trim()
        val bottom = if (rows.size > 1) grid.value(rows[1], col).trim() else ""
        val topMerge = grid.mergeAt(rows[0], col)
        val bottomMerge = if (rows.size > 1) grid.mergeAt(rows[1], col) else null
        val sameOrigin = topMerge != null && topMerge == bottomMerge

        when {
            sameOrigin || (top.isNotBlank() && top == bottom && topMerge != null) ->
                add(rows[0], WeekParity.BOTH, top)
            else -> {
                if (top.isNotBlank()) add(rows[0], WeekParity.ODD, top)
                if (bottom.isNotBlank() && bottom != top) add(rows[1], WeekParity.EVEN, bottom)
                else if (bottom.isNotBlank() && top.isBlank()) add(rows[1], WeekParity.EVEN, bottom)
            }
        }
        return result
    }

    private fun parseFootnotes(grid: XlsxGrid): Map<String, DateSpan> {
        val map = mutableMapOf<String, DateSpan>()
        for (row in 1..grid.maxRow) {
            for (col in 1..grid.maxCol) {
                val text = grid.raw(row, col)
                if (!text.trimStart().startsWith("*")) continue
                val parsed = LessonCellParser.parseFootnote(text) ?: continue
                map[parsed.first] = parsed.second
            }
        }
        return map
    }

    internal fun parseTime(raw: String): Pair<LocalTime, LocalTime>? {
        val compact = raw.trim().replace(Regex("\\s+"), "")
        val match = timeRegex.find(compact) ?: return null
        val start = parseClock(match.groupValues[1], match.groupValues[2]) ?: return null
        val end = parseClock(match.groupValues[3], match.groupValues[4]) ?: return null
        return start to end
    }

    private fun parseClock(major: String, minor: String): LocalTime? {
        return if (minor.isNotEmpty()) {
            val hour = major.toIntOrNull() ?: return null
            val minute = minor.toIntOrNull() ?: return null
            runCatching { LocalTime.of(hour, minute) }.getOrNull()
        } else {
            val digits = major.filter { it.isDigit() }
            when (digits.length) {
                3 -> runCatching { LocalTime.of(digits.take(1).toInt(), digits.takeLast(2).toInt()) }.getOrNull()
                4 -> runCatching { LocalTime.of(digits.take(2).toInt(), digits.takeLast(2).toInt()) }.getOrNull()
                else -> null
            }
        }
    }

    private fun lessonId(
        group: String,
        slot: Slot,
        parity: WeekParity,
        parsed: ParsedCellLesson,
    ): String {
        val material = listOf(
            group,
            slot.day.name,
            slot.start.toString(),
            slot.end.toString(),
            parity.name,
            parsed.subject,
            parsed.kind.name,
            parsed.kindLabel,
            parsed.teachers.joinToString(",") { it.key },
            parsed.rooms.joinToString(","),
            (parsed.subgroup ?: 0).toString(),
            parsed.marker.orEmpty(),
            parsed.raw,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun uniquifyIds(lessons: List<Lesson>): List<Lesson> {
        val seen = HashSet<String>()
        return lessons.map { lesson ->
            var id = lesson.id
            var n = 2
            while (!seen.add(id)) {
                id = "${lesson.id}-$n"
                n++
            }
            if (id == lesson.id) lesson else lesson.copy(id = id)
        }
    }
}
