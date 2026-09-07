package ni.shikatu.pgupsd.data.xlsx

import ni.shikatu.pgupsd.domain.DateSpan
import ni.shikatu.pgupsd.domain.LessonKind
import ni.shikatu.pgupsd.domain.Teacher
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class ParsedCellLesson(
    val subject: String,
    val kind: LessonKind,
    val kindLabel: String,
    val teachers: List<Teacher>,
    val rooms: List<String>,
    val subgroup: Int?,
    val marker: String?,
    val raw: String,
)

internal object LessonCellParser {
    private val typePatterns = listOf(
        "лабораторное занятие" to LessonKind.LAB,
        "практическое занятие" to LessonKind.PRACTICE,
        "лабораторная работа" to LessonKind.LAB,
        "лабораторное" to LessonKind.LAB,
        "практическое" to LessonKind.PRACTICE,
        "лекция" to LessonKind.LECTURE,
        "практика" to LessonKind.PRACTICE,
    )

    private val teacherRegex = Regex(
        """(?iu)(проф|доц|асс|преп|ст\.?\s*преп)\.?\s+([А-ЯЁ][А-Яа-яЁёA-Za-z-]+)\s+([А-ЯЁ][А-Яа-яЁё]*\.\s*[А-ЯЁ]\.)""",
    )

    private val roomRegex = Regex(
        """(?iu)ауд\.?\s*([0-9A-Za-zА-Яа-яЁё./-]+(?:\s*,\s*[0-9A-Za-zА-Яа-яЁё./-]+)*)""",
    )

    private val subgroupRegex = Regex(
        """(?iu)подгруппа\s*(?:№\s*)?(\d)\s*:""",
    )

    private val footnoteRegex = Regex(
        """(?iu)^\*+\s*[-–—]?\s*занятия проводятся\s+со?\s+(\d{2}\.\d{2}\.\d{4})\.?\s*(?:г\.?)?\s*(?:по\s+(\d{2}\.\d{2}\.\d{4})\.?)?""",
    )
    private val footnoteMarker = Regex("""(?<![*])(\*{1,4})(?![*])""")

    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parseFootnote(text: String): Pair<String, DateSpan>? {
        val compact = text.trim().replace('\n', ' ')
        val stars = compact.takeWhile { it == '*' }
        if (stars.isEmpty()) return null
        val match = footnoteRegex.find(compact) ?: return null
        val from = runCatching { LocalDate.parse(match.groupValues[1], dateFmt) }.getOrNull()
            ?: return null
        val toRaw = match.groupValues.getOrNull(2).orEmpty()
        val to = if (toRaw.isBlank()) from else {
            runCatching { LocalDate.parse(toRaw, dateFmt) }.getOrNull() ?: from
        }
        return stars to DateSpan(from, to)
    }

    fun parse(raw: String): List<ParsedCellLesson> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (isSelfStudy(trimmed)) return emptyList()

        val blocks = splitBlocks(trimmed)
        return blocks.mapNotNull { parseBlock(it) }
    }

    fun isSelfStudy(text: String): Boolean =
        text.lowercase().replace(Regex("\\s+"), " ").contains("самостоятельная работа")

    private fun splitBlocks(raw: String): List<String> {
        val byStars = splitByFootnotes(raw)
        val exploded = mutableListOf<String>()
        for (part in byStars) {
            val matches = subgroupRegex.findAll(part).toList()
            if (matches.size <= 1) {
                exploded += part
            } else {
                val indices = matches.map { it.range.first } + part.length
                for (i in 0 until matches.size) {
                    val chunk = part.substring(indices[i], indices[i + 1]).trim()
                    if (chunk.isNotEmpty()) exploded += chunk
                }
            }
        }
        return exploded.ifEmpty { listOf(raw) }
    }

    private fun splitByFootnotes(raw: String): List<String> {
        val markers = footnoteMarker.findAll(raw).filter { match ->
            val i = match.range.first
            i == 0 || raw[i - 1].isWhitespace()
        }.toList()
        if (markers.isEmpty()) return listOf(raw)
        val chunks = mutableListOf<String>()
        val first = markers.first().range.first
        if (first > 0) {
            val head = raw.substring(0, first).trim()
            if (head.isNotEmpty()) chunks += head
        }
        for (index in markers.indices) {
            val from = markers[index].range.first
            val to = if (index + 1 < markers.size) markers[index + 1].range.first else raw.length
            val chunk = raw.substring(from, to).trim()
            if (chunk.isNotEmpty()) chunks += chunk
        }
        return chunks.ifEmpty { listOf(raw) }
    }

    private fun parseBlock(raw: String): ParsedCellLesson? {
        var text = raw.replace('\u00A0', ' ').trim()
        if (text.isEmpty()) return null

        val marker = text.takeWhile { it == '*' }.ifEmpty { null }
        if (marker != null) text = text.drop(marker.length).trim()

        val subgroup = subgroupRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        text = text.replace(subgroupRegex, " ").trim()

        val teachers = teacherRegex.findAll(text).map { match ->
            val title = match.groupValues[1]
                .lowercase()
                .replace(Regex("""\s+"""), " ")
                .replace("ст. преп", "ст.преп")
                .replace("ст.преп", "ст.преп")
                .let { rawTitle ->
                    when {
                        rawTitle.startsWith("проф") -> "проф."
                        rawTitle.startsWith("доц") -> "доц."
                        rawTitle.startsWith("асс") -> "асс."
                        rawTitle.startsWith("ст") -> "ст.преп."
                        else -> "преп."
                    }
                }
            val surname = match.groupValues[2].trim()
            val initials = match.groupValues[3].replace(" ", "")
            Teacher(title = title, name = "$surname $initials")
        }.toList()

        val rooms = roomRegex.findAll(text).flatMap { match ->
            match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().toList()

        var kind = LessonKind.OTHER
        var kindLabel = ""
        val lower = text.lowercase()
        for ((label, mapped) in typePatterns) {
            if (lower.contains(label)) {
                kind = mapped
                kindLabel = label
                break
            }
        }

        var subjectSource = text
        subjectSource = subjectSource.replace(teacherRegex, " ")
        subjectSource = subjectSource.replace(roomRegex, " ")
        if (kindLabel.isNotEmpty()) {
            subjectSource = subjectSource.replace(Regex("(?iu)${Regex.escape(kindLabel)}"), " ")
        }
        val subject = subjectSource
            .replace(Regex("""[|]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '–', '—', ':', ',', ';')
            .ifBlank { fallbackSubject(text) }

        if (subject.isBlank()) return null

        return ParsedCellLesson(
            subject = cleanupSubject(subject),
            kind = kind,
            kindLabel = kindLabel.ifBlank { kindFallbackLabel(kind) },
            teachers = teachers,
            rooms = rooms,
            subgroup = subgroup,
            marker = marker,
            raw = raw.trim(),
        )
    }

    private fun fallbackSubject(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotEmpty() &&
                    !line.startsWith("*") &&
                    teacherRegex.find(line) == null &&
                    roomRegex.find(line) == null &&
                    typePatterns.none { line.lowercase().contains(it.first) }
            }.orEmpty()

    private fun cleanupSubject(subject: String): String =
        subject
            .replace(Regex("""^(?:подгруппа\s*(?:№\s*)?\d\s*:)\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun kindFallbackLabel(kind: LessonKind): String = when (kind) {
        LessonKind.LECTURE -> "лекция"
        LessonKind.PRACTICE -> "практика"
        LessonKind.LAB -> "лабораторное"
        LessonKind.OTHER -> ""
    }
}
