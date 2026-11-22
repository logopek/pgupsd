package ni.shikatu.pgupsd.plan

import ni.shikatu.pgupsd.data.plan.PlanGlyph
import ni.shikatu.pgupsd.data.plan.PlanTableParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanPdfParserTest {
    @Test
    fun parsesThePublishedIvbPlan() {
        val bytes = javaClass.getResourceAsStream("/ivb2026.pdf")?.readBytes()
            ?: error("Нет app/src/test/resources/ivb2026.pdf")
        val plan = PDDocument.load(bytes).use { document ->
            val stripper = GlyphStripper()
            stripper.sortByPosition = true
            stripper.getText(document)
            PlanTableParser.parse(stripper.pages)
        }

        assertEquals("09.03.01", plan.meta.code)
        assertEquals("Информатика и вычислительная техника", plan.meta.direction)
        assertEquals(
            "Программное обеспечение средств вычислительной техники и автоматизированных систем",
            plan.meta.profile,
        )
        assertEquals(2026, plan.meta.year)
        assertEquals("очная", plan.meta.form)
        assertEquals(62, plan.courses.size)

        val history = plan.courses.first { it.id == "Б1.О.1" }
        assertEquals("История России", history.name)
        assertEquals(emptyList<Int>(), history.exams)
        assertEquals(listOf(1, 2), history.zachets)
        assertEquals(36, history.offering(1)!!.hours.lecture)
        assertEquals(16, history.offering(1)!!.hours.practice)
        assertEquals(48, history.offering(2)!!.hours.lecture)
        assertEquals("История и философия", history.department)

        val philosophy = plan.courses.first { it.id == "Б1.О.2" }
        assertEquals(listOf(4), philosophy.exams)
        assertEquals(32, philosophy.offering(4)!!.hours.lecture)
        assertEquals(60, philosophy.offering(4)!!.hours.selfStudy)
        assertEquals(36, philosophy.offering(4)!!.hours.control)

        val language = plan.courses.first { it.id == "Б1.О.3" }
        assertEquals(listOf(2), language.exams)
        assertEquals(listOf(1, 7), language.zachets)
        assertEquals(48, language.offering(1)!!.hours.practice)
        assertEquals(2, language.offering(7)!!.credits)

        val law = plan.courses.first { it.id == "Б1.О.8" }
        assertEquals("Правовое обеспечение профессиональной деятельности", law.name)
        val systems = plan.courses.first { it.id == "Б1.В.ДВ.5.1" }
        assertEquals("Архитектура информационных систем на платформе 1С предприятия", systems.name)
        assertTrue(systems.inPlan)
        val other = plan.courses.first { it.id == "Б1.В.ДВ.3.2" }
        assertFalse(other.inPlan)
        assertTrue(plan.courses.none { it.id == "Б1.В.ДВ.1" || it.id == "ФТД" })
        assertEquals("Технический иностранный язык", plan.courses.first { it.id == "ФТД.1" }.name)
        assertEquals(
            "Выполнение и защита выпускной квалификационной работы",
            plan.courses.first { it.id == "Б3.Д.1" }.name,
        )
    }

    private class GlyphStripper : PDFTextStripper() {
        val pages = mutableListOf<MutableList<PlanGlyph>>()
        private var current = mutableListOf<PlanGlyph>()

        override fun startPage(page: PDPage) {
            current = mutableListOf()
            pages.add(current)
            super.startPage(page)
        }

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            val clean = text.trim()
            if (clean.isEmpty() || textPositions.isEmpty()) return
            val first = textPositions.first()
            current.add(PlanGlyph(first.xDirAdj, first.yDirAdj, clean))
        }
    }
}
