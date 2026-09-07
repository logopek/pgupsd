package ni.shikatu.pgupsd.xlsx

import ni.shikatu.pgupsd.data.remote.ScheduleCatalogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogParserTest {
    @Test
    fun parsesHomepageLinks() {
        val html = """
            Четная неделя
            <a href="https://rasp.pgups.ru/files/b/2025/ait_1.xlsx">1 курс</a>
            <a href="https://rasp.pgups.ru/files/b/2025/ait_2.xlsx">2 курс</a>
            <a href="https://rasp.pgups.ru/files/b/2025/upl_3.xlsx">3 курс</a>
        """.trimIndent()
        val catalog = ScheduleCatalogParser.parse(html)
        val ait = catalog.faculties.first { it.code == "ait" }
        assertEquals(listOf(1, 2), ait.courses)
        assertEquals("2025", ait.yearFolder)
        assertTrue(catalog.faculties.any { it.code == "upl" })
        assertEquals(ni.shikatu.pgupsd.domain.WeekParity.EVEN, catalog.currentParityHint)
    }
}
