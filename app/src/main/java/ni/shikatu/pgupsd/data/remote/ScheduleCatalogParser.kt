package ni.shikatu.pgupsd.data.remote

import ni.shikatu.pgupsd.domain.Faculty
import ni.shikatu.pgupsd.domain.ScheduleCatalog
import ni.shikatu.pgupsd.domain.WeekParity

object ScheduleCatalogParser {
    val fallbackFaculties: List<Faculty> = listOf(
        faculty("ait", "АИТ", "Автоматизация и интеллектуальные технологии", 1..5),
        faculty("pgs", "ПГС", "Промышленное и гражданское строительство", 1..6),
        faculty("ts", "ТС", "Транспортное строительство", 1..5),
        faculty("tes", "ТЭС", "Транспортные и энергетические системы", 1..5),
        faculty("eim", "ЭиМ", "Экономика и менеджмент", 1..4),
        faculty("upl", "УПЛ", "Управление перевозками и логистика", 1..5),
    )

    private val known = mapOf(
        "ait" to ("АИТ" to "Автоматизация и интеллектуальные технологии"),
        "pgs" to ("ПГС" to "Промышленное и гражданское строительство"),
        "ts" to ("ТС" to "Транспортное строительство"),
        "tes" to ("ТЭС" to "Транспортные и энергетические системы"),
        "eim" to ("ЭиМ" to "Экономика и менеджмент"),
        "upl" to ("УПЛ" to "Управление перевозками и логистика"),
    )

    private val fileRegex = Regex(
        """https://rasp\.pgups\.ru/files/b/(\d+)/([a-z]+)_(\d+)\.xlsx""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(html: String): ScheduleCatalog {
        val grouped = mutableMapOf<String, MutableMap<Int, String>>()
        fileRegex.findAll(html).forEach { match ->
            val year = match.groupValues[1]
            val code = match.groupValues[2].lowercase()
            val course = match.groupValues[3].toInt()
            grouped.getOrPut(code) { mutableMapOf() }[course] = year
        }
        val faculties = if (grouped.isEmpty()) {
            fallbackFaculties
        } else {
            grouped.mapNotNull { (code, courses) ->
                val names = known[code] ?: (code.uppercase() to code.uppercase())
                Faculty(
                    code = code,
                    shortName = names.first,
                    fullName = names.second,
                    courses = courses.keys.sorted(),
                    yearFolder = courses.values.maxBy { it },
                )
            }.sortedBy { facultyOrder(it.code) }
        }
        val parity = when {
            html.contains("Нечетная неделя", ignoreCase = true) ||
                html.contains("Нечётная неделя", ignoreCase = true) -> WeekParity.ODD
            html.contains("Четная неделя", ignoreCase = true) ||
                html.contains("Чётная неделя", ignoreCase = true) -> WeekParity.EVEN
            else -> null
        }
        return ScheduleCatalog(faculties = faculties, currentParityHint = parity)
    }

    fun fallback(): ScheduleCatalog = ScheduleCatalog(fallbackFaculties, null)

    private fun faculty(code: String, shortName: String, fullName: String, courses: IntRange) =
        Faculty(code, shortName, fullName, courses.toList(), yearFolder = "2025")

    private fun facultyOrder(code: String): Int =
        listOf("ait", "pgs", "ts", "tes", "eim", "upl").indexOf(code).takeIf { it >= 0 } ?: 99
}
