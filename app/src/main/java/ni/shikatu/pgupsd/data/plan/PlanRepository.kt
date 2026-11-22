package ni.shikatu.pgupsd.data.plan

import android.content.Context
import ni.shikatu.pgupsd.data.remote.PgupsClient
import ni.shikatu.pgupsd.domain.Curriculum
import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.WebFaculty
import ni.shikatu.pgupsd.domain.WebProgram
import java.io.File

class PlanRepository(
    private val client: PgupsClient,
    context: Context,
) {
    private val cache = File(context.cacheDir, "study-plans").apply { mkdirs() }
    private val checks = File(context.filesDir, "plan-done.txt")
    private val appContext = context.applicationContext

    fun faculties(): List<WebFaculty> =
        PlanCatalogParser.faculties(client.fetchHtml("https://www.pgups.ru/struct/faculties/"))

    fun programs(faculty: WebFaculty): List<WebProgram> {
        val url = "https://www.pgups.ru${faculty.path}napravleniya-podgotovki"
        return PlanCatalogParser.programs(client.fetchHtml(url))
    }

    fun curriculum(link: PlanLink, force: Boolean = false): Curriculum {
        val file = File(cache, link.url.hashCode().toUInt().toString(16) + ".pdf")
        if (force || !file.exists() || file.length() < 1000) {
            file.writeBytes(client.downloadBytes(link.url))
        }
        val parsed = try {
            PdfPlanLoader.parse(appContext, file.readBytes())
        } catch (error: Exception) {
            file.delete()
            throw IllegalStateException("Не удалось прочитать PDF учебного плана", error)
        }
        if (parsed.courses.isEmpty()) {
            file.delete()
            error("В этом PDF нет таблицы дисциплин. На сайте для части наборов лежит другой формат.")
        }
        return parsed
    }

    fun loadChecks(): Set<String> =
        if (!checks.exists()) emptySet() else checks.readLines().filter { it.isNotBlank() }.toSet()

    fun saveChecks(keys: Set<String>) {
        checks.writeText(keys.joinToString("\n"))
    }
}
