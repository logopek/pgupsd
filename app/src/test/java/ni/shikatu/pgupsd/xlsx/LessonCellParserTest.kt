package ni.shikatu.pgupsd.xlsx

import ni.shikatu.pgupsd.data.xlsx.LessonCellParser
import ni.shikatu.pgupsd.domain.LessonKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCellParserTest {
    @Test
    fun parsesLectureWithTeacherAndRoom() {
        val raw = """
            Математика
            практическое занятие
            доц. Коновалова Д.С.
            ауд. 7-312
        """.trimIndent()
        val parsed = LessonCellParser.parse(raw).single()
        assertEquals("Математика", parsed.subject)
        assertEquals(LessonKind.PRACTICE, parsed.kind)
        assertEquals("Коновалова Д.С.", parsed.teachers.single().name)
        assertEquals("доц.", parsed.teachers.single().title)
        assertEquals("7-312", parsed.rooms.single())
    }

    @Test
    fun parsesTwoTeachersAndRooms() {
        val raw = "Физика лабораторное занятие доц. Никитченко В.И. доц. Рубан А.С. ауд. 6-301"
        val parsed = LessonCellParser.parse(raw).single()
        assertEquals(2, parsed.teachers.size)
        assertEquals("6-301", parsed.rooms.single())
    }

    @Test
    fun parsesSubgroupsAsSeparateLessons() {
        val raw = "подгруппа 1: Сопротивление материалов лабораторное занятие доц. Видюшенков С.А. ауд. 3-310 подгруппа 2: Основы механосборочных работ лабораторное занятие ст. преп. Бабакаев В.Э. ауд. 8-003"
        val parsed = LessonCellParser.parse(raw)
        assertEquals(2, parsed.size)
        assertEquals(1, parsed[0].subgroup)
        assertEquals(2, parsed[1].subgroup)
        assertTrue(parsed[0].subject.contains("Сопротивление"))
        assertTrue(parsed[1].subject.contains("механосборочных"))
    }

    @Test
    fun skipsSelfStudy() {
        assertTrue(LessonCellParser.parse("Самостоятельная работа").isEmpty())
    }

    @Test
    fun parsesFootnote() {
        val note = LessonCellParser.parseFootnote("* - занятия проводятся с 15.09.2026 по 27.10.2026")
        assertEquals("*", note!!.first)
        assertEquals("2026-09-15", note.second.from.toString())
        assertEquals("2026-10-27", note.second.to.toString())
    }

    @Test
    fun parsesFootnoteWithSoAndExtraDot() {
        val so = LessonCellParser.parseFootnote("* - занятия проводятся со 02.09.2026 по 09.09.2026")
        assertEquals("*", so!!.first)
        assertEquals("2026-09-02", so.second.from.toString())
        val extraDot = LessonCellParser.parseFootnote("** - занятия проводятся с 04.09.2026. по 11.09.2026")
        assertEquals("**", extraDot!!.first)
        assertEquals("2026-09-04", extraDot.second.from.toString())
        assertEquals("2026-09-11", extraDot.second.to.toString())
    }

    @Test
    fun splitsStackedFootnoteLessonsWithoutBreakingStarRuns() {
        val raw = """
            **История России 
            лекция
            проф. Фортунатов В.В.
             ауд. 1-316 
             *** Физика
            лекция
            проф. Дьяченко А.Т.
            ауд. 1-316
        """.trimIndent()
        val parsed = LessonCellParser.parse(raw)
        assertEquals(2, parsed.size)
        assertEquals("**", parsed[0].marker)
        assertTrue(parsed[0].subject.contains("История"))
        assertEquals("***", parsed[1].marker)
        assertTrue(parsed[1].subject.contains("Физика"))
    }
}
