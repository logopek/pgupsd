package ni.shikatu.pgupsd.domain

import ni.shikatu.pgupsd.ui.formatDurationMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DayItemsTest {
    @Test
    fun skipsFifteenMinuteBellGap() {
        val rows = dayItems(
            listOf(
                occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "Математика"),
                occ(LocalTime.of(10, 45), LocalTime.of(12, 15), "Физика"),
            ),
        )
        assertEquals(2, rows.size)
        assertTrue(rows.all { it is DayItem.Pair })
    }

    @Test
    fun lunchBetweenSecondAndThirdIsNotAWindow() {
        val rows = dayItems(
            listOf(
                occ(LocalTime.of(10, 45), LocalTime.of(12, 15), "Физика"),
                occ(LocalTime.of(13, 15), LocalTime.of(14, 45), "История"),
            ),
        )
        assertEquals(2, rows.size)
        assertTrue(rows.all { it is DayItem.Pair })
        assertNull(currentWindow(rows.map { (it as DayItem.Pair).occurrence }, LocalTime.of(12, 40)))
    }

    @Test
    fun skippedPairIsAWindow() {
        val rows = dayItems(
            listOf(
                occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "Математика"),
                occ(LocalTime.of(13, 15), LocalTime.of(14, 45), "История"),
            ),
        )
        assertEquals(3, rows.size)
        val window = rows[1] as DayItem.Window
        assertEquals(LocalTime.of(10, 30), window.start)
        assertEquals(LocalTime.of(13, 15), window.end)
    }

    @Test
    fun sameSlotSubgroupsAreNotAWindow() {
        val rows = dayItems(
            listOf(
                occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "История", subgroup = 1),
                occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "Физика", subgroup = 2),
                occ(LocalTime.of(10, 45), LocalTime.of(12, 15), "Математика"),
            ),
        )
        assertEquals(3, rows.size)
        assertTrue(rows.all { it is DayItem.Pair })
    }

    @Test
    fun seedsWindowFromPreviousPairWhenSlotSkipped() {
        val featured = occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "Математика")
        val rows = dayItems(
            listOf(occ(LocalTime.of(13, 15), LocalTime.of(14, 45), "История")),
            previous = featured,
        )
        val window = rows.first() as DayItem.Window
        assertEquals(LocalTime.of(10, 30), window.start)
        assertEquals(LocalTime.of(13, 15), window.end)
    }

    @Test
    fun doesNotSeedLunchFromPreviousPair() {
        val featured = occ(LocalTime.of(10, 45), LocalTime.of(12, 15), "Физика")
        val rows = dayItems(
            listOf(occ(LocalTime.of(13, 15), LocalTime.of(14, 45), "История")),
            previous = featured,
        )
        assertTrue(rows.all { it is DayItem.Pair })
    }

    @Test
    fun currentWindowOnlyForSkippedSlot() {
        val items = listOf(
            occ(LocalTime.of(9, 0), LocalTime.of(10, 30), "Математика"),
            occ(LocalTime.of(13, 15), LocalTime.of(14, 45), "История"),
        )
        val live = currentWindow(items, LocalTime.of(12, 20))
        assertEquals(LocalTime.of(10, 30), live?.start)
        assertEquals(LocalTime.of(13, 15), live?.end)
        assertNull(currentWindow(items, LocalTime.of(9, 30)))
        assertNull(currentWindow(items, LocalTime.of(8, 0)))
        assertNull(currentWindow(items, LocalTime.of(16, 0)))
    }

    @Test
    fun durationFormat() {
        assertEquals("45 мин", formatDurationMinutes(45))
        assertEquals("1 ч", formatDurationMinutes(60))
        assertEquals("1 ч 15 мин", formatDurationMinutes(75))
    }
}

private fun occ(
    start: LocalTime,
    end: LocalTime,
    subject: String,
    subgroup: Int? = null,
): Occurrence = Occurrence(
    date = LocalDate.of(2026, 9, 4),
    lesson = Lesson(
        id = "$subject-$start-$subgroup",
        groupName = "ИВБ-611",
        dayOfWeek = DayOfWeek.FRIDAY,
        start = start,
        end = end,
        parity = WeekParity.BOTH,
        subject = subject,
        kind = LessonKind.LECTURE,
        kindLabel = "лекция",
        teachers = emptyList(),
        rooms = listOf("1-101"),
        subgroup = subgroup,
        valid = null,
        raw = "",
    ),
    localNote = null,
)
