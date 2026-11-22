package ni.shikatu.pgupsd.data.plan

import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.WebProgram
import java.net.URLDecoder
import java.time.LocalDate

fun intakeYear(course: Int, today: LocalDate): Int {
    val academicStart = if (today.monthValue >= 9) today.year else today.year - 1
    return academicStart - course + 1
}

fun suggestPlan(
    programs: List<WebProgram>,
    groupName: String,
    course: Int,
    today: LocalDate,
): PlanLink? {
    val year = intakeYear(course, today)
    val sameYear = programs.flatMap { it.plans }.filter {
        it.year == year && it.url.isNotBlank() && it.form.contains("очн", ignoreCase = true)
    }
    val fullTime = sameYear.filter { !it.form.contains("заоч", ignoreCase = true) }.ifEmpty { sameYear }
    if (fullTime.isEmpty()) return null
    val prefix = groupName.substringBefore('-').substringBefore(' ').filter { it.isLetter() }
    if (prefix.isEmpty()) return fullTime.singleOrNull()
    val latin = translit(prefix)
    val named = fullTime.filter { link ->
        val file = fileName(link.url)
        containsToken(file, prefix) || (latin.isNotEmpty() && containsToken(file, latin))
    }
    return named.singleOrNull()
}

private fun fileName(url: String): String {
    val raw = url.substringAfterLast('/').substringBefore('?')
    return try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Exception) {
        raw
    }
}

private fun containsToken(file: String, token: String): Boolean {
    val haystack = file.uppercase()
    val needle = token.uppercase()
    var start = 0
    while (true) {
        val index = haystack.indexOf(needle, start)
        if (index < 0) return false
        val before = haystack.getOrNull(index - 1)
        val after = haystack.getOrNull(index + needle.length)
        if ((before == null || !before.isLetter()) && (after == null || !after.isLetter())) return true
        start = index + 1
    }
}

private fun translit(value: String): String = buildString {
    value.uppercase().forEach { char ->
        append(
            when (char) {
                'А' -> "A"
                'Б' -> "B"
                'В' -> "V"
                'Г' -> "G"
                'Д' -> "D"
                'Е', 'Ё' -> "E"
                'Ж' -> "ZH"
                'З' -> "Z"
                'И', 'Й' -> "I"
                'К' -> "K"
                'Л' -> "L"
                'М' -> "M"
                'Н' -> "N"
                'О' -> "O"
                'П' -> "P"
                'Р' -> "R"
                'С' -> "S"
                'Т' -> "T"
                'У' -> "U"
                'Ф' -> "F"
                'Х' -> "H"
                'Ц' -> "C"
                'Ч' -> "CH"
                'Ш', 'Щ' -> "SH"
                'Ы' -> "Y"
                'Э' -> "E"
                'Ю' -> "YU"
                'Я' -> "YA"
                else -> ""
            },
        )
    }
}
