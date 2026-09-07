package ni.shikatu.pgupsd.xlsx

import ni.shikatu.pgupsd.data.xlsx.ScheduleXlsxParser
import ni.shikatu.pgupsd.domain.WeekParity
import ni.shikatu.pgupsd.domain.isoWeekParity
import ni.shikatu.pgupsd.domain.pairNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleXlsxParserTest {
    @Test
    fun parsesAitFirstCourse() {
        val parsed = open("ait_1.xlsx")
        assertTrue(parsed.title.contains("АВТОМАТИЗАЦИЯ"))
        assertTrue(parsed.groups.contains("ИВБ-611"))
        val ivb = parsed.lessons.filter { it.groupName == "ИВБ-611" }
        assertTrue(ivb.isNotEmpty())
        val math = ivb.first { it.subject.contains("Математика") && it.start == LocalTime.of(10, 45) }
        assertEquals(DayOfWeek.MONDAY, math.dayOfWeek)
        assertTrue(math.teachers.any { it.name.contains("Коновалова") })
        assertTrue(math.rooms.contains("7-312"))
    }

    @Test
    fun oddEvenSplitOnUnmergedRows() {
        val parsed = open("ait_1.xlsx")
        val ivb = parsed.lessons.filter { it.groupName == "ИВБ-611" }
        val mondayFirst = ivb.filter { it.dayOfWeek == DayOfWeek.MONDAY && it.start == LocalTime.of(9, 0) }
        assertTrue(mondayFirst.any { it.parity == WeekParity.ODD || it.parity == WeekParity.BOTH })
        assertTrue(mondayFirst.any { it.kindLabel.contains("лекц") || it.subject.contains("Физическая") })
    }

    @Test
    fun parsesTesGroupsAndFootnotes() {
        val parsed = open("tes_2.xlsx")
        assertTrue(parsed.groups.contains("ПС-501"))
        val ps = parsed.lessons.filter { it.groupName == "ПС-501" }
        assertTrue(ps.isNotEmpty())
        assertTrue(ps.any { it.teachers.isNotEmpty() })
        assertTrue(ps.any { it.rooms.isNotEmpty() })
    }

    @Test
    fun lessonIdsAreUnique() {
        for (name in listOf("ait_1.xlsx", "tes_2.xlsx")) {
            val parsed = open(name)
            val ids = parsed.lessons.map { it.id }
            assertEquals(name, ids.size, ids.toSet().size)
        }
    }

    @Test
    fun doesNotDuplicateMergedTimeSlots() {
        val parsed = open("ait_1.xlsx")
        val date = LocalDate.of(2026, 9, 1)
        val ivb = parsed.lessons.filter { it.groupName == "ИВБ-611" && it.visibleOn(date) }
        val fingerprint = ivb.map {
            listOf(
                it.start.toString(),
                it.subjectKey,
                it.kind.name,
                it.subgroup ?: 0,
                it.teachers.joinToString { t -> t.key },
            ).joinToString("|")
        }
        assertEquals(fingerprint.distinct(), fingerprint)

        val discrete = ivb.filter {
            it.subject.contains("Дискретная", ignoreCase = true) &&
                it.start == LocalTime.of(10, 45)
        }
        assertEquals(1, discrete.size)
    }

    @Test
    fun parseTimeFormats() {
        assertEquals(LocalTime.of(9, 0) to LocalTime.of(10, 30), ScheduleXlsxParser.parseTime("900-1030"))
        assertEquals(LocalTime.of(10, 45) to LocalTime.of(12, 15), ScheduleXlsxParser.parseTime("1045-1215"))
        assertEquals(LocalTime.of(14, 40) to LocalTime.of(16, 10), ScheduleXlsxParser.parseTime("1440-1610"))
    }

    @Test
    fun fridayFootnotesStaySeparateLessons() {
        val parsed = open("ait_1.xlsx")
        val ivb = parsed.lessons.filter {
            it.groupName == "ИВБ-611" && it.dayOfWeek == DayOfWeek.FRIDAY && it.start == LocalTime.of(9, 0)
        }
        val history = ivb.single { it.subject.contains("История") }
        val physics = ivb.single { it.subject.contains("Физика") }
        assertEquals("2026-09-04", history.valid!!.from.toString())
        assertEquals("2026-09-11", history.valid!!.to.toString())
        assertEquals("2026-09-18", physics.valid!!.from.toString())
        assertEquals("2026-10-09", physics.valid!!.to.toString())
        val onFourth = LocalDate.of(2026, 9, 4)
        assertTrue(history.visibleOn(onFourth))
        assertTrue(!physics.visibleOn(onFourth))
        val inOctober = LocalDate.of(2026, 9, 25)
        assertTrue(physics.visibleOn(inOctober))
        assertTrue(!history.visibleOn(inOctober))
    }

    @Test
    fun pairNumberFollowsBellsNotDayIndex() {
        assertEquals(1, pairNumber(LocalTime.of(9, 0)))
        assertEquals(2, pairNumber(LocalTime.of(10, 45)))
        assertEquals(2, pairNumber(LocalTime.of(10, 30)))
        assertEquals(3, pairNumber(LocalTime.of(13, 15)))
        assertEquals(4, pairNumber(LocalTime.of(15, 0)))
        assertEquals(5, pairNumber(LocalTime.of(16, 45)))
    }

    @Test
    fun isoWeekMatchesSiteOnFirstSeptember2026() {
        val date = LocalDate.of(2026, 9, 1)
        assertEquals(WeekParity.EVEN, isoWeekParity(date))
    }

    private fun open(name: String) =
        javaClass.classLoader!!.getResourceAsStream(name)!!.use { ScheduleXlsxParser.parse(it) }
}
