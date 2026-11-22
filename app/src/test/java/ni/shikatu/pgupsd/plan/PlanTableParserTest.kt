package ni.shikatu.pgupsd.plan

import ni.shikatu.pgupsd.data.plan.PlanGlyph
import ni.shikatu.pgupsd.data.plan.PlanTableParser
import ni.shikatu.pgupsd.domain.PlanBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTableParserTest {
    @Test
    fun readsAttestationHoursWrappedNamesAndLaterSemesters() {
        val plan = PlanTableParser.parse(listOf(title(), leftPage(), rightPage(), tenthPage()))
        assertEquals("09.03.01", plan.meta.code)
        assertEquals("Информатика и вычислительная техника", plan.meta.direction)
        assertEquals("Программное обеспечение", plan.meta.profile)
        assertEquals("Информационные и вычислительные системы", plan.meta.chair)
        assertEquals(2026, plan.meta.year)
        assertEquals("очная", plan.meta.form)
        assertEquals("№ 929 от 19.09.2017", plan.meta.standard)
        assertTrue(plan.meta.duration.startsWith("4"))

        val history = plan.courses.first { it.id == "Б1.О.1" }
        assertEquals("История России", history.name)
        assertEquals(emptyList<Int>(), history.exams)
        assertEquals(listOf(1, 2), history.zachets)
        assertEquals(PlanBlock.CORE, history.block)
        val first = history.offering(1)!!
        assertEquals(2, first.credits)
        assertEquals(36, first.hours.lecture)
        assertEquals(16, first.hours.practice)
        assertEquals(16, first.hours.selfStudy)
        assertEquals(4, first.hours.control)
        val second = history.offering(2)!!
        assertEquals(48, second.hours.lecture)
        assertEquals(4, second.hours.selfStudy)

        val philosophy = plan.courses.first { it.id == "Б1.О.2" }
        assertEquals("Философия", philosophy.name)
        assertEquals(listOf(4), philosophy.exams)
        assertEquals(emptyList<Int>(), philosophy.zachets)
        assertEquals(32, philosophy.offering(4)!!.hours.lecture)
        assertEquals(60, philosophy.offering(4)!!.hours.selfStudy)
        assertEquals(36, philosophy.offering(4)!!.hours.control)

        val law = plan.courses.first { it.id == "Б1.О.8" }
        assertEquals("Правовое обеспечение профессиональной деятельности", law.name)
        assertFalse(philosophy.name.contains("Правовое"))

        val safety = plan.courses.first { it.id == "Б1.О.4" }
        assertEquals(listOf(5), safety.zachets)
        assertEquals(emptyList<Int>(), safety.exams)
        assertEquals(3, safety.offering(5)!!.credits)
        assertEquals(4, safety.offering(5)!!.hours.control)
        assertEquals("Техносферная и экологическая безопасность", safety.department)

        assertNull(plan.courses.find { it.id == "Б1.В.ДВ.1" })
        val chosen = plan.courses.first { it.id == "Б1.В.ДВ.1.1" }
        val alternative = plan.courses.first { it.id == "Б1.В.ДВ.1.2" }
        assertTrue(chosen.inPlan)
        assertFalse(alternative.inPlan)
        assertEquals(PlanBlock.ELECTIVE, chosen.block)

        val extra = plan.courses.first { it.id == "ФТД.1" }
        assertEquals(listOf(10), extra.zachets)
        assertEquals(PlanBlock.EXTRA, extra.block)
        assertEquals(2, extra.offering(10)!!.credits)
    }

    private fun title() = listOf(
        glyph(40f, 10f, "Направление : 09.03.01 «Информатика и вычислительная техника»"),
        glyph(40f, 20f, "Профиль : Программное обеспечение"),
        glyph(40f, 30f, "Кафедра: Информационные и вычислительные системы"),
        glyph(40f, 40f, "Факультет: Автоматизация и интеллектуальные технологии"),
        glyph(40f, 50f, "Квалификация: бакалавр Год начала подготовки (по учебному плану) 202 6"),
        glyph(40f, 60f, "Форма обучения: очная Образовательный стандарт (ФГОС) № 929 от 19.09.2017"),
        glyph(40f, 70f, "Срок получения образования: 4 г."),
    )

    private fun leftPage(): List<PlanGlyph> {
        val header = mutableListOf(
            glyph(372f, 36f, "Семестр 1"),
            glyph(494f, 36f, "Семестр 2"),
            glyph(616f, 36f, "Семестр 3"),
            glyph(739f, 36f, "Семестр 4"),
            glyph(176f, 47f, "Экза"),
            glyph(55f, 50f, "Индекс"),
            glyph(111f, 50f, "Наименование"),
            glyph(190f, 50f, "Зачет"),
            glyph(208f, 50f, "КП"),
            glyph(224f, 50f, "КР"),
            glyph(355f, 50f, "Лек"),
            glyph(477f, 50f, "Лек"),
            glyph(600f, 50f, "Лек"),
            glyph(722f, 50f, "Лек"),
            glyph(324f, 50f, "з.е."),
            glyph(338f, 50f, "Итого"),
            glyph(386f, 50f, "Пр"),
            glyph(417f, 50f, "СР"),
            glyph(447f, 50f, "з.е."),
            glyph(460f, 50f, "Итого"),
            glyph(509f, 50f, "Пр"),
            glyph(540f, 50f, "СР"),
            glyph(569f, 50f, "з.е."),
            glyph(582f, 50f, "Итого"),
            glyph(692f, 50f, "з.е."),
            glyph(705f, 50f, "Итого"),
            glyph(754f, 50f, "Пр"),
            glyph(784f, 50f, "СР"),
            glyph(177f, 53f, "мен"),
            glyph(430f, 53f, "роль"),
            glyph(553f, 53f, "роль"),
            glyph(798f, 53f, "роль"),
        )
        header += glyph(35f, 80f, "+")
        header += glyph(47f, 80f, "Б1.О.1")
        header += glyph(78f, 80f, "История")
        header += glyph(120f, 80f, "России")
        header += glyph(193f, 80f, "12")
        header += listOf(327f to "2", 356f to "36", 387f to "16", 417f to "16", 434f to "4").map { glyph(it.first, 80f, it.second) }
        header += listOf(449f to "2", 478f to "48", 509f to "16", 541f to "4", 556f to "4").map { glyph(it.first, 80f, it.second) }

        header += glyph(35f, 90f, "+")
        header += glyph(47f, 90f, "Б1.О.2")
        header += glyph(78f, 90f, "Философия")
        header += glyph(179f, 90f, "4")
        header += listOf(694f to "4", 723f to "32", 754f to "16", 784f to "60", 800f to "36").map { glyph(it.first, 90f, it.second) }

        header += glyph(35f, 110f, "+")
        header += glyph(47f, 110f, "Б1.О.4")
        header += glyph(78f, 110f, "Безопасность жизнедеятельности")
        header += glyph(195f, 110f, "5")

        header += glyph(78f, 118f, "Правовое")
        header += glyph(35f, 120.5f, "+")
        header += glyph(47f, 120.5f, "Б1.О.8")
        header += glyph(78f, 123f, "обеспечение профессиональной деятельности")

        header += glyph(35f, 140f, "+")
        header += glyph(47f, 140f, "Б1.В.ДВ.1")
        header += glyph(78f, 140f, "Дисциплины по выбору")
        header += glyph(327f, 140f, "5")
        header += glyph(35f, 150f, "+")
        header += glyph(47f, 150f, "Б1.В.ДВ.1.1")
        header += glyph(78f, 150f, "Программирование на языке JAVA")
        header += glyph(35f, 160f, "-")
        header += glyph(47f, 160f, "Б1.В.ДВ.1.2")
        header += glyph(78f, 160f, "Программирование на языке C#")
        return header
    }

    private fun rightPage() = listOf(
        glyph(108f, 28f, "Семестр 5"),
        glyph(34f, 40f, "Индекс"),
        glyph(60f, 40f, "з.е."),
        glyph(74f, 40f, "Итого"),
        glyph(91f, 40f, "Лек"),
        glyph(122f, 40f, "Пр"),
        glyph(153f, 40f, "СР"),
        glyph(183f, 40f, "з.е."),
        glyph(196f, 40f, "Итого"),
        glyph(213f, 40f, "Лек"),
        glyph(550f, 40f, "Код"),
        glyph(585f, 40f, "Наименование"),
        glyph(166f, 43f, "роль"),
        glyph(35f, 110f, "Б1.О.4"),
        glyph(63f, 110f, "3"),
        glyph(170f, 110f, "4"),
        glyph(562f, 107.5f, "Техносферная"),
        glyph(562f, 112.5f, "и экологическая безопасность"),
    )

    private fun tenthPage() = listOf(
        glyph(80f, 28f, "Семестр 9"),
        glyph(200f, 28f, "Семестр 10"),
        glyph(176f, 37f, "Экза"),
        glyph(190f, 40f, "Зачет"),
        glyph(355f, 40f, "Лек"),
        glyph(470f, 40f, "Лек"),
        glyph(300f, 40f, "з.е."),
        glyph(314f, 40f, "Итого"),
        glyph(420f, 40f, "з.е."),
        glyph(434f, 40f, "Итого"),
        glyph(35f, 80f, "+"),
        glyph(47f, 80f, "ФТД.1"),
        glyph(78f, 80f, "Технический иностранный язык"),
        glyph(193f, 80f, "10"),
        glyph(424f, 80f, "2"),
    )

    private fun glyph(x: Float, y: Float, text: String) = PlanGlyph(x, y, text)
}
