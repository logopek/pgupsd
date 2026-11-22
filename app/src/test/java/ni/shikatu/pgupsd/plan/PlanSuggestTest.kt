package ni.shikatu.pgupsd.plan

import ni.shikatu.pgupsd.data.plan.intakeYear
import ni.shikatu.pgupsd.data.plan.suggestPlan
import ni.shikatu.pgupsd.domain.PlanLink
import ni.shikatu.pgupsd.domain.WebProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PlanSuggestTest {
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun intakeYearFollowsTheAcademicYear() {
        assertEquals(2026, intakeYear(1, today))
        assertEquals(2024, intakeYear(3, today))
        assertEquals(2025, intakeYear(1, LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun suggestsTheFullTimeFileNamedAfterTheGroup() {
        val programs = listOf(
            program(
                "09.03.01",
                PlanLink(2026, "очная форма", "https://www.pgups.ru/upload/09.03.01_IVB_2026_.pdf"),
                PlanLink(2024, "очная форма", "https://www.pgups.ru/upload/09.03.01_%D0%98%D0%92%D0%91_2024.pdf"),
                PlanLink(2026, "заочная форма", "https://www.pgups.ru/upload/09.03.01_IVB_2026_z.pdf"),
            ),
            program(
                "13.03.01",
                PlanLink(2026, "очная форма", "https://www.pgups.ru/upload/13.03.01_TEB_2026_.pdf"),
                PlanLink(2026, "очно-заочная форма", "https://www.pgups.ru/upload/13.03.01_TEB_OZ_2026.pdf"),
            ),
        )
        val first = suggestPlan(programs, "ИВБ-611", 1, today)
        assertEquals("09.03.01_IVB_2026_.pdf", first?.url?.substringAfterLast('/'))
        val third = suggestPlan(programs, "ИВБ-411", 3, today)
        assertEquals("https://www.pgups.ru/upload/09.03.01_%D0%98%D0%92%D0%91_2024.pdf", third?.url)
    }

    @Test
    fun doesNotGuessWhenSeveralPlansMatch() {
        val programs = listOf(
            program(
                "09.03.01",
                PlanLink(2026, "очная форма", "https://www.pgups.ru/upload/09.03.01_IVB_2026_a.pdf"),
                PlanLink(2026, "очная форма", "https://www.pgups.ru/upload/09.03.01_IVB_2026_b.pdf"),
            ),
        )
        assertNull(suggestPlan(programs, "ИВБ-101", 1, today))
    }

    private fun program(code: String, vararg plans: PlanLink) = WebProgram(
        code = code,
        direction = code,
        level = "Бакалавриат",
        profile = code,
        plans = plans.toList(),
    )
}
