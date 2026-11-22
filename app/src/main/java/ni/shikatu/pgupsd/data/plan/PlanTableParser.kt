package ni.shikatu.pgupsd.data.plan

import ni.shikatu.pgupsd.domain.Curriculum
import ni.shikatu.pgupsd.domain.PlanBlock
import ni.shikatu.pgupsd.domain.PlanCourse
import ni.shikatu.pgupsd.domain.PlanHours
import ni.shikatu.pgupsd.domain.PlanMeta
import ni.shikatu.pgupsd.domain.PlanOffering
import kotlin.math.abs

data class PlanGlyph(
    val x: Float,
    val y: Float,
    val text: String,
)

object PlanTableParser {
    private val indexPattern = Regex("""^(?:Б\d|ФТД)(?:\.[0-9A-Za-zА-Яа-яЁё]+)+$""")
    private val semesterInline = Regex("""^Семестр\s*(\d{1,2})$""")
    private val fieldLabels = listOf(
        "Направление",
        "Профиль",
        "Кафедра",
        "Факультет",
        "Квалификация",
        "Год начала",
        "Форма обучения",
        "Образовательный",
        "Срок получения образования",
    )

    fun parse(pages: List<List<PlanGlyph>>): Curriculum {
        val plain = pages.joinToString("\n") { page ->
            page.sortedWith(compareBy<PlanGlyph> { it.y }.thenBy { it.x }).joinToString(" ") { it.text }
        }
        val courses = linkedMapOf<String, MutableCourse>()
        for (page in pages) {
            parsePage(page, courses)
        }
        val built = courses.values.map { it.build() }.filter { it.name.isNotBlank() || it.offerings.isNotEmpty() }
        val ids = built.map { it.id }.toSet()
        val leaves = built.filter { course ->
            ids.none { other -> other != course.id && other.startsWith(course.id + ".") }
        }
        return Curriculum(meta = parseMeta(plain), courses = leaves)
    }

    private fun parsePage(glyphs: List<PlanGlyph>, courses: LinkedHashMap<String, MutableCourse>) {
        if (glyphs.isEmpty()) return
        val lines = clusterLines(glyphs)
        val header = lines.firstOrNull { line ->
            line.items.count { it.text == "Лек" || it.text == "Лек." } >= 2 && line.items.any { isZe(it.text) }
        } ?: return
        val anchors = anchors(header, lines)
        val columns = semesterColumns(header, lines)
        if (columns.isEmpty()) return
        val indexLines = lines.mapNotNull { line ->
            line.items.firstOrNull { indexPattern.matches(it.text) && it.x < 120f }?.let { line to it }
        }
        for ((line, index) in indexLines) {
            val course = courses.getOrPut(index.text) { MutableCourse(index.text) }
            val mark = line.items.firstOrNull { it.text in MARKS && it.x < 42f }
            if (mark != null) course.inPlan = mark.text == "+"
            val name = textNear(lines, indexLines, line, 68f, 170f)
            if (name.isNotBlank() && course.name.isBlank()) course.name = name
            val deptX = anchors.dept
            if (deptX != null) {
                val dept = textNear(lines, indexLines, line, deptX - 8f, deptX + 220f)
                if (dept.isNotBlank()) course.department = dept
            }
            for (token in line.items) {
                if (token.text.isEmpty() || !token.text.all { it.isDigit() }) continue
                if (assignSummary(course, token, anchors, columns)) continue
                assignSemester(course, token, columns)
            }
        }
    }

    private fun semesterColumns(header: Line, lines: List<Line>): List<Column> {
        val raw = header.items.filter { isZe(it.text) }.map { it.x }.sorted()
        val withTotal = raw.filter { mark ->
            header.items.any { it.text == "Итого" && it.x > mark && it.x - mark < 30f }
        }
        val ze = if (withTotal.size >= 2) withTotal else raw
        val numbers = semesterNumbers(lines, header.y)
        val controls = lines.flatMap { it.items }.filter { it.text == "роль" || it.text == "Конт" || it.text == "Контроль" }
        return ze.mapIndexed { index, mark ->
            val next = ze.getOrNull(index + 1) ?: (mark + 130f)
            val inBand = header.items.filter { it.x >= mark - 4f && it.x < next - 6f }
            val semester = numbers.getOrNull(index) ?: ((numbers.firstOrNull() ?: 1) + index)
            buildList {
                add(Column(mark, semester, Field.ZE))
                fun take(label: String, field: Field) {
                    inBand.firstOrNull { it.text == label }?.let { add(Column(it.x, semester, field)) }
                }
                take("Итого", Field.TOTAL)
                take("Лек", Field.LEC)
                take("Лаб", Field.LAB)
                take("Пр", Field.PR)
                take("СРП", Field.SRP)
                take("СР", Field.SR)
                controls.filter { it.x >= mark && it.x < next }.minByOrNull { it.x }
                    ?.let { add(Column(it.x, semester, Field.CTRL)) }
            }
        }.flatten()
    }

    private fun semesterNumbers(lines: List<Line>, headerY: Float): List<Int> {
        val found = mutableListOf<Pair<Float, Int>>()
        for (line in lines) {
            if (abs(line.y - headerY) > 20f) continue
            val items = line.items.sortedBy { it.x }
            var i = 0
            while (i < items.size) {
                val text = items[i].text
                val inline = semesterInline.matchEntire(text)
                if (inline != null) {
                    found += items[i].x to inline.groupValues[1].toInt()
                    i++
                    continue
                }
                val next = items.getOrNull(i + 1)
                if (text == "Семестр" && next != null && next.text.all { it.isDigit() }) {
                    found += items[i].x to next.text.toInt()
                    i += 2
                    continue
                }
                i++
            }
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    private fun anchors(header: Line, lines: List<Line>): SummaryAnchors {
        val near = lines.flatMap { it.items }.filter { abs(it.y - header.y) <= 8f }
        fun xOf(predicate: (String) -> Boolean): Float? =
            near.filter { predicate(it.text) }.minByOrNull { it.x }?.x
        return SummaryAnchors(
            exam = xOf { it == "Экза" || it == "мен" || it.startsWith("Экза") },
            zachet = xOf { it.startsWith("Зачет") || it.startsWith("Зачёт") },
            kp = xOf { it == "КП" },
            kr = xOf { it == "КР" },
            dept = xOf { it == "Код" },
        )
    }

    private fun assignSummary(
        course: MutableCourse,
        token: PlanGlyph,
        anchors: SummaryAnchors,
        columns: List<Column>,
    ): Boolean {
        val options = listOfNotNull(
            anchors.exam?.let { it to Summary.EXAM },
            anchors.zachet?.let { it to Summary.ZACHET },
            anchors.kp?.let { it to Summary.KP },
            anchors.kr?.let { it to Summary.KR },
        )
        if (options.isEmpty()) return false
        val (anchor, kind) = options.minBy { abs(it.first - token.x) }
        val distance = abs(anchor - token.x)
        val semesterDistance = columns.minOfOrNull { abs(it.x - token.x) } ?: Float.MAX_VALUE
        if (distance > 9f || distance >= semesterDistance) return false
        val semesters = semesters(token.text)
        when (kind) {
            Summary.EXAM -> course.exams += semesters
            Summary.ZACHET -> course.zachets += semesters
            Summary.KP -> course.projects += semesters
            Summary.KR -> course.works += semesters
        }
        return true
    }

    private fun assignSemester(course: MutableCourse, token: PlanGlyph, columns: List<Column>) {
        val column = columns.minByOrNull { abs(it.x - token.x) } ?: return
        if (abs(column.x - token.x) > 11f) return
        val number = token.text.toIntOrNull() ?: return
        val offering = course.offerings.getOrPut(column.semester) { MutableOffering(column.semester) }
        when (column.field) {
            Field.ZE -> offering.credits = number
            Field.LEC -> offering.lecture = number
            Field.LAB -> offering.lab = number
            Field.PR -> offering.practice = number
            Field.SRP -> offering.srp = number
            Field.SR -> offering.selfStudy = number
            Field.CTRL -> offering.control = number
            Field.TOTAL -> Unit
        }
    }

    private fun textNear(
        lines: List<Line>,
        indexLines: List<Pair<Line, PlanGlyph>>,
        indexLine: Line,
        xMin: Float,
        xMax: Float,
    ): String {
        val chunks = mutableListOf<PlanGlyph>()
        fun take(line: Line) {
            line.items.filter { glyph ->
                glyph.x in xMin..xMax &&
                    glyph.text.any { it.isLetter() } &&
                    glyph.text !in HEADER_WORDS &&
                    !indexPattern.matches(glyph.text)
            }.forEach { chunks += it }
        }
        take(indexLine)
        for (line in lines) {
            if (line === indexLine) continue
            if (line.items.any { indexPattern.matches(it.text) && it.x < 120f }) continue
            val nearest = indexLines.minByOrNull { abs(it.first.y - line.y) } ?: continue
            if (nearest.first !== indexLine || abs(nearest.first.y - line.y) > 4.8f) continue
            take(line)
        }
        return chunks.sortedWith(compareBy({ it.y }, { it.x }))
            .joinToString(" ") { it.text }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Attestation cells concatenate semester digits: «12» is semesters 1 and 2,
     * «17» is 1 and 7. The only two-digit value in these plans is semester 10.
     */
    private fun semesters(raw: String): List<Int> {
        val out = mutableListOf<Int>()
        var i = 0
        while (i < raw.length) {
            val char = raw[i]
            if (char == '1' && i + 1 < raw.length && raw[i + 1] == '0') {
                out += 10
                i += 2
            } else if (char in '1'..'9') {
                out += char.digitToInt()
                i++
            } else {
                i++
            }
        }
        return out
    }

    private fun clusterLines(glyphs: List<PlanGlyph>): List<Line> {
        val sorted = glyphs.sortedBy { it.y }
        val lines = mutableListOf<Line>()
        for (glyph in sorted) {
            val last = lines.lastOrNull()
            if (last != null && glyph.y - last.y <= 2.4f) {
                last.items += glyph
                last.y = (last.y + glyph.y) / 2f
            } else {
                lines += Line(glyph.y, mutableListOf(glyph))
            }
        }
        return lines
    }

    private fun parseMeta(text: String): PlanMeta {
        fun field(label: String): String {
            val match = Regex("$label\\s*[:.]?\\s*").find(text) ?: return ""
            val start = match.range.last + 1
            var end = text.length
            for (other in fieldLabels) {
                if (other == label) continue
                val index = text.indexOf(other, start)
                if (index in start until end) end = index
            }
            return text.substring(start, end)
                .replace(Regex("\\s+"), " ")
                .trim()
                .trim('«', '»', '"', ':', '.')
                .trim()
        }
        val code = Regex("""(\d{2}\.\d{2}\.\d{2})""").find(text)?.groupValues?.get(1).orEmpty()
        val direction = field("Направление").removePrefix(code).trim(' ', '«', '»', '"', '.', ':')
        val year = Regex("""Год начала подготовки(?:(?!Форма|Срок|Квалификация).){0,90}?(\d(?:\s?\d){3})""")
            .find(text)?.groupValues?.get(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val standard = Regex("""ФГОС\)?\s*(№\s*\d+\s*от\s*\d{2}\.\d{2}\.\d{4})""")
            .find(text)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        return PlanMeta(
            code = code,
            direction = direction,
            profile = field("Профиль"),
            faculty = field("Факультет"),
            chair = field("Кафедра"),
            year = year,
            qualification = field("Квалификация"),
            form = field("Форма обучения"),
            duration = field("Срок получения образования"),
            standard = standard,
        )
    }

    private fun isZe(text: String) = text == "з.е." || text == "з.е"

    private class Line(var y: Float, val items: MutableList<PlanGlyph>)

    private class SummaryAnchors(
        val exam: Float?,
        val zachet: Float?,
        val kp: Float?,
        val kr: Float?,
        val dept: Float?,
    )

    private enum class Summary { EXAM, ZACHET, KP, KR }

    private enum class Field { ZE, TOTAL, LEC, LAB, PR, SRP, SR, CTRL }

    private data class Column(val x: Float, val semester: Int, val field: Field)

    private class MutableOffering(val semester: Int) {
        var credits: Int = 0
        var lecture: Int = 0
        var lab: Int = 0
        var practice: Int = 0
        var srp: Int = 0
        var selfStudy: Int = 0
        var control: Int = 0

        fun build(): PlanOffering? {
            if (credits == 0 && lecture == 0 && lab == 0 && practice == 0 && srp == 0 && selfStudy == 0 && control == 0) {
                return null
            }
            return PlanOffering(
                semester = semester,
                credits = credits,
                hours = PlanHours(lecture, lab, practice, srp, selfStudy, control),
            )
        }
    }

    private class MutableCourse(val id: String) {
        var name: String = ""
        var inPlan: Boolean = true
        var department: String = ""
        val exams = mutableListOf<Int>()
        val zachets = mutableListOf<Int>()
        val projects = mutableListOf<Int>()
        val works = mutableListOf<Int>()
        val offerings = linkedMapOf<Int, MutableOffering>()

        fun build(): PlanCourse = PlanCourse(
            id = id,
            name = name.replace(Regex("\\s+"), " ").trim(),
            block = blockOf(id),
            inPlan = inPlan,
            exams = exams.distinct().sorted(),
            zachets = zachets.distinct().sorted(),
            courseProjects = projects.distinct().sorted(),
            courseWorks = works.distinct().sorted(),
            offerings = offerings.values.mapNotNull { it.build() }.sortedBy { it.semester },
            department = department,
        )
    }

    private fun blockOf(id: String): PlanBlock = when {
        id.contains(".ДВ.") -> PlanBlock.ELECTIVE
        id.startsWith("Б1.О") -> PlanBlock.CORE
        id.startsWith("Б1.В") -> PlanBlock.VARIABLE
        id.startsWith("Б2") -> PlanBlock.PRACTICE
        id.startsWith("Б3") -> PlanBlock.GIA
        id.startsWith("ФТД") -> PlanBlock.EXTRA
        else -> PlanBlock.OTHER
    }

    private val MARKS = setOf("+", "-", "–", "−")
    private val HEADER_WORDS = setOf("Код", "Наименование", "Индекс", "Экзамен", "Зачет", "Зачёт")
}
