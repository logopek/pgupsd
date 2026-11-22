package ni.shikatu.pgupsd.data.plan

import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.WebFaculty
import ni.shikatu.pgupsd.domain.WebProgram

object PlanCatalogParser {
    private val facultyLink = Regex(
        """<a\s[^>]*href="(/struct/[^"#?/]+/?)"[^>]*>([\s\S]*?)</a>""",
        RegexOption.IGNORE_CASE,
    )
    private val cardSplit = Regex("""<div class="education_card\b""")
    private val yearSplit = Regex("""Для набора\s+(\d{4})\s+года""")

    fun faculties(html: String): List<WebFaculty> {
        val cut = html.substringAfter("id=\"content-col\"", html)
            .substringBefore("id=\"aside-col\"")
            .substringBefore("Полезная информация")
        val found = LinkedHashMap<String, WebFaculty>()
        for (match in facultyLink.findAll(cut)) {
            val name = clean(match.groupValues[2])
            val path = match.groupValues[1].let { if (it.endsWith("/")) it else "$it/" }
            if (name.isBlank() || name.startsWith("Кафедра") || name == "Факультеты") continue
            if (path.count { it == '/' } != 3) continue
            found.putIfAbsent(path, WebFaculty(name = name, path = path))
        }
        return found.values.toList()
    }

    fun programs(html: String): List<WebProgram> {
        val body = html.substringAfter("id=\"content-col\"", html)
        return cardSplit.split(body).drop(1).flatMap { card -> parseCard(card) }
    }

    private fun parseCard(card: String): List<WebProgram> {
        val code = Regex("""itemprop="EduCode"[^>]*>([^<]+)""")
            .find(card)?.groupValues?.get(1)?.let(::clean).orEmpty()
        if (code.isBlank()) return emptyList()
        val heading = Regex("""<h4[^>]*>([\s\S]*?)</h4>""")
            .find(card)?.groupValues?.get(1)?.let(::clean).orEmpty()
        val direction = heading.removePrefix(code).trim().ifBlank { heading }
        val level = Regex("""edu-lvl-cardstyle">\s*([^<]+)""")
            .find(card)?.groupValues?.get(1)?.let(::clean).orEmpty()
        val profiles = Regex("""<div class="education_name">""").split(card).drop(1)
        val chunks = profiles.ifEmpty { listOf(card) }
        return chunks.mapNotNull { chunk ->
            val profile = Regex("""<h7>([\s\S]*?)</h7>""")
                .find(chunk)?.groupValues?.get(1)?.let(::clean).orEmpty()
            val plans = plansIn(chunk)
            if (profile.isBlank() && plans.isEmpty()) return@mapNotNull null
            WebProgram(
                code = code,
                direction = direction,
                level = level,
                profile = profile,
                plans = plans,
            )
        }
    }

    private fun plansIn(chunk: String): List<PlanLink> {
        val plans = mutableListOf<PlanLink>()
        val years = yearSplit.findAll(chunk).toList()
        years.forEachIndexed { index, yearMatch ->
            val year = yearMatch.groupValues[1].toInt()
            val start = yearMatch.range.last
            val end = years.getOrNull(index + 1)?.range?.first ?: chunk.length
            val slice = chunk.substring(start, end)
            val planHtml = Regex("""itemprop="educationplan"([\s\S]*?)</div>""")
                .find(slice)?.groupValues?.get(1).orEmpty()
            for (link in Regex("""<a\s[^>]*href="([^"]*)"[^>]*>([\s\S]*?)</a>""").findAll(planHtml)) {
                val href = link.groupValues[1].trim()
                val form = clean(link.groupValues[2])
                if (href.isBlank() || form.isBlank()) continue
                if (!href.contains(".pdf", ignoreCase = true)) continue
                plans += PlanLink(year = year, form = form, url = absolute(href))
            }
        }
        return plans
    }

    private fun absolute(href: String): String =
        if (href.startsWith("http")) href else "https://www.pgups.ru$href"

    private fun clean(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace('\u00a0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}
