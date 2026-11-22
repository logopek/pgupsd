package ni.shikatu.pgupsd.plan

import ni.shikatu.pgupsd.data.plan.PlanCatalogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanCatalogParserTest {
    @Test
    fun readsFacultiesFromTheContentColumnOnly() {
        val html = """
            <a href="/struct/institut_nepreryvnogo_obrazovaniya/">Институт непрерывного образования</a>
            <div id="content-col">
              <a href="/struct/faculties/">Факультеты</a>
              <a href="/struct/avtomatizatsiya-i-intellektualnye-tekhnologii/">
                Автоматизация и интеллектуальные технологии
              </a>
              <a href="/struct/kafedra_informatika/">Кафедра «Информатика»</a>
              <a href="/struct/ekonomika-i-menedzhment/">Экономика и менеджмент</a>
              <a href="/struct/fakultet-bezotryvnykh-form-obucheniya/">Факультет безотрывных форм обучения</a>
            </div>
            <div id="aside-col">
              <a href="/struct/muzey_pgups/">Музей ПГУПС</a>
            </div>
        """.trimIndent()
        val faculties = PlanCatalogParser.faculties(html)
        assertEquals(
            listOf(
                "Автоматизация и интеллектуальные технологии",
                "Экономика и менеджмент",
                "Факультет безотрывных форм обучения",
            ),
            faculties.map { it.name },
        )
        assertEquals("/struct/avtomatizatsiya-i-intellektualnye-tekhnologii/", faculties.first().path)
    }

    @Test
    fun splitsProfilesAndIgnoresDescriptionPdfs() {
        val html = """
            <div id="content-col">
              <div class="education_card education_color-1">
                <h4><span itemprop="EduCode">23.05.05</span> Системы обеспечения движения поездов</h4>
                <div class="edu-lvl-cardstyle">Специалитет</div>
                <div class="education_name"><h7>Электроснабжение железных дорог</h7></div>
                <a href="#">Для набора 2026 года</a>
                <div itemprop="OOPmain"><a href="/upload/oop.pdf">описание</a></div>
                <div itemprop="educationplan">
                  <a href="/upload/23.05.05_ES_2026.pdf">очная форма</a>,
                  <a href="">заочная форма</a>
                </div>
                <div class="education_name"><h7>Автоматика и телемеханика на железнодорожном транспорте</h7></div>
                <a href="#">Для набора 2026 года</a>
                <div itemprop="educationplan">
                  <a href="/upload/23.05.05_AT_2026.pdf">очная форма</a>
                </div>
                <div itemprop="educationshedule">
                  <a href="/upload/kug.pdf">график</a>
                </div>
              </div>
              <div class="education_card">
                <h4><span itemprop="EduCode">09.03.01</span> Информатика и вычислительная техника</h4>
                <div class="edu-lvl-cardstyle">Бакалавриат</div>
                <div class="education_name"><h7>Программное обеспечение</h7></div>
                <a href="#">Для набора 2022 года</a>
                <div itemprop="OOPmain"><a href="/upload/oop_ivb_2022.pdf">описание</a></div>
                <a href="#">Для набора 2026 года</a>
                <div itemprop="educationplan">
                  <a href="/upload/09.03.01_IVB_2026_.pdf">очная форма</a>,
                  <a href="">заочная форма</a>
                </div>
                <a href="#">Для набора 2024 года</a>
                <div itemprop="educationplan">
                  <a href="/upload/zagruski/09.03.01_ИВБ_2024.pdf">очная форма</a>
                </div>
              </div>
            </div>
        """.trimIndent()
        val programs = PlanCatalogParser.programs(html)
        assertEquals(3, programs.size)
        val supply = programs.first { it.profile.startsWith("Электроснабжение") }
        assertEquals("23.05.05", supply.code)
        assertEquals("Специалитет", supply.level)
        assertEquals(listOf("https://www.pgups.ru/upload/23.05.05_ES_2026.pdf"), supply.plans.map { it.url })
        val automation = programs.first { it.profile.startsWith("Автоматика") }
        assertEquals("23.05.05_AT_2026.pdf", automation.plans.single().url.substringAfterLast('/'))
        val ivb = programs.first { it.code == "09.03.01" }
        assertEquals(listOf(2026, 2024), ivb.plans.map { it.year })
        assertTrue(ivb.plans.any { it.url.endsWith("09.03.01_ИВБ_2024.pdf") })
        assertTrue(programs.none { it.plans.any { plan -> plan.url.contains("oop") || plan.url.contains("kug") } })
    }
}
