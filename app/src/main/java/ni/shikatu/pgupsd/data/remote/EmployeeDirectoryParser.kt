package ni.shikatu.pgupsd.data.remote

import ni.shikatu.pgupsd.domain.StaffProfile
import java.util.Base64

object EmployeeDirectoryParser {
    private val cardSplit = Regex("""<div class="table faculty"""", RegexOption.IGNORE_CASE)
    private val fioLink = Regex(
        """<a[^>]*href=["']([^"']*sveden/employees/([a-z0-9-]+))["'][^>]*itemprop=["']fio["'][^>]*>([^<]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val fioLinkAlt = Regex(
        """<a[^>]*itemprop=["']fio["'][^>]*href=["']([^"']*sveden/employees/([a-z0-9-]+))["'][^>]*>([^<]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val imgSrc = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val pageRegex = Regex("""PAGEN_2=(\d+)""")

    fun parseList(html: String): List<StaffProfile> =
        cardSplit.split(html).drop(1).mapNotNull { parseCard(it) }.distinctBy { it.slug }

    fun parseProfile(html: String, base: StaffProfile? = null): StaffProfile? {
        val fromList = parseList(html).firstOrNull() ?: base ?: return null
        return fromList.copy(
            degree = fromList.degree ?: itemProp(html, "degree"),
            rank = fromList.rank ?: itemProp(html, "academstat"),
            disciplines = parseDisciplines(html).ifEmpty { fromList.disciplines },
            office = fromList.office ?: parseOffice(html),
            phones = fromList.phones.ifEmpty { parsePhones(html) },
            emails = fromList.emails.ifEmpty { parseEmails(html) },
        )
    }

    fun maxPage(html: String): Int =
        pageRegex.findAll(html).map { it.groupValues[1].toInt() }.maxOrNull() ?: 1

    private fun parseCard(raw: String): StaffProfile? {
        val fio = fioLink.find(raw) ?: fioLinkAlt.find(raw) ?: return null
        val href = fio.groupValues[1]
        val slug = fio.groupValues[2]
        val fullName = stripHtml(fio.groupValues[3])
        if (fullName.isBlank()) return null
        val photo = imgSrc.find(raw)?.groupValues?.get(1)?.let(::absoluteUrl)
        val posts = parsePosts(raw)
        return StaffProfile(
            slug = slug,
            fullName = fullName,
            photoUrl = photo,
            degree = itemProp(raw, "degree"),
            rank = itemProp(raw, "academstat"),
            posts = posts,
            phones = parsePhones(raw),
            emails = parseEmails(raw),
            office = parseOffice(raw),
            profileUrl = absoluteUrl(href),
        )
    }

    private fun parsePosts(html: String): List<String> {
        val raw = itemPropHtml(html, "post") ?: return emptyList()
        val text = stripHtml(raw.replace(Regex("(?i)<br\\s*/?>"), "\n"))
        return text.split('\n').map { it.trim(' ', ',') }.filter { it.isNotEmpty() }
    }

    private fun parsePhones(html: String): List<String> {
        val inner = Regex(
            """itemprop=["']telephone["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1) ?: return emptyList()
        return inner.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), "\n")
            .split('\n')
            .map { it.replace("&nbsp;", " ").trim() }
            .filter { line -> line.any { it.isDigit() } }
    }

    private fun parseEmails(html: String): List<String> {
        val encoded = Regex(
            """(?:itemprop=["']e-mail["']|class=["']mailcode["'])[^>]*>([^<]+)""",
            RegexOption.IGNORE_CASE,
        ).findAll(html).map { it.groupValues[1].trim() }.toList()
        val mailto = Regex("""mailto:([^"'>\s]+)""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1].trim() }.toList()
        return (encoded + mailto).mapNotNull(::decodeMail).distinct()
    }

    private fun parseOffice(html: String): String? {
        val match = Regex(
            """user-icon map[\s\S]*?<div class="cell">([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE,
        ).find(html) ?: return null
        return stripHtml(match.groupValues[1]).ifBlank { null }
    }

    private fun parseDisciplines(html: String): List<String> {
        val inner = Regex(
            """itemprop=["']teachingDiscipline["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1) ?: return emptyList()
        val withoutLabel = inner.replace(Regex("(?i)<b>[\\s\\S]*?</b>"), " ")
        return stripHtml(withoutLabel)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun itemProp(html: String, name: String): String? =
        itemPropHtml(html, name)?.let(::stripHtml)?.ifBlank { null }

    private fun itemPropHtml(html: String, name: String): String? {
        val re = Regex(
            """itemprop=["']$name["'][^>]*>([\s\S]*?)</(?:dd|div|a|span)>""",
            RegexOption.IGNORE_CASE,
        )
        return re.find(html)?.groupValues?.get(1)
    }

    private fun decodeMail(raw: String): String? {
        val value = raw.trim()
        if (value.contains('@')) return value
        val decoded = runCatching {
            String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        }.getOrNull() ?: return null
        return decoded.takeIf { it.contains('@') }
    }

    internal fun stripHtml(raw: String): String =
        raw.replace(Regex("(?i)<br\\s*/?>"), ", ")
            .replace(Regex("<[^>]+>"), " ")
            .replace('\u00A0', ' ')
            .replace("&nbsp;", " ")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace("&amp;", "&")
            .replace(Regex("\\s+,"), ",")
            .replace(Regex(",\\s*,+"), ",")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', ';')

    internal fun absoluteUrl(path: String): String {
        val trimmed = path.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return "https://www.pgups.ru" + if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}
