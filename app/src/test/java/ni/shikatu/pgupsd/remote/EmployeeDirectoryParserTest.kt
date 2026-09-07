package ni.shikatu.pgupsd.remote

import ni.shikatu.pgupsd.data.remote.EmployeeDirectoryParser
import ni.shikatu.pgupsd.domain.personMatchKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmployeeDirectoryParserTest {
    private val html = """
        <div class="table faculty" id="bx_1">
          <div class="cell">
            <img src="/upload/gluharev.jpg" alt="Глухарев Михаил Леонидович">
          </div>
          <div class="cell">
            <a href="/sveden/employees/glukharev-mikhail-leonidovich" class="title" itemprop="fio">Глухарев Михаил Леонидович</a>
            <dl>
              <dt>Ученая степень:</dt>
              <dd itemprop="Degree">к.т.н.</dd>
              <dt>Занимаемые должности:</dt>
              <dd itemprop="post">Доцент - <a href="/struct/kafedra/">Кафедра «Информатика и информационная безопасность»</a></dd>
            </dl>
            <i class="user-icon map"></i>
            <div class="cell">Санкт-Петербург, Московский пр., д. 9, ауд. 2-105</div>
            <a href="mailto:cmVjdG9yQHBndXBzLnJ1" itemprop="e-mail" class="mailcode">cmVjdG9yQHBndXBzLnJ1</a>
          </div>
        </div>
        <div class="table faculty" id="bx_2">
          <a href="/sveden/employees/konovalova-darya-sergeevna" class="title" itemprop="fio">Коновалова Дарья Сергеевна</a>
          <dd itemprop="Degree">к.т.н.</dd>
          <dd itemprop="AcademStat">доцент</dd>
          <dd itemprop="post">Доцент - Кафедра «Высшая математика»</dd>
          <div class="cell" itemprop="telephone">(812) 457-00-00<br>55-001</div>
        </div>
    """.trimIndent()

    @Test
    fun parsesCards() {
        val people = EmployeeDirectoryParser.parseList(html)
        assertEquals(2, people.size)
        val glu = people.first { it.slug == "glukharev-mikhail-leonidovich" }
        assertEquals("Глухарев Михаил Леонидович", glu.fullName)
        assertEquals("к.т.н.", glu.degree)
        assertTrue(glu.posts.first().contains("Информатика"))
        assertTrue(glu.office!!.contains("2-105"))
        assertEquals("https://www.pgups.ru/upload/gluharev.jpg", glu.photoUrl)
        assertEquals("rector@pgups.ru", glu.emails.single())
        val kon = people.first { it.slug.startsWith("konovalova") }
        assertEquals("доцент", kon.rank)
        assertEquals(listOf("(812) 457-00-00", "55-001"), kon.phones)
    }

    @Test
    fun matchKeyAlignsScheduleNameWithFio() {
        assertEquals(
            personMatchKey("Коновалова Дарья Сергеевна"),
            personMatchKey("Коновалова Д.С."),
        )
        assertEquals("коновалова|дс", personMatchKey("Коновалова Д.С."))
    }

    @Test
    fun parsesDisciplinesFromProfile() {
        val profile = """
            <div class="table faculty">
              <a class="title" itemprop="fio" href="/sveden/employees/glukharev-mikhail-leonidovich">Глухарев Михаил Леонидович</a>
            </div>
            <div class="pb-7" itemprop="teachingDiscipline"><b>Преподаваемые дисциплины:</b> Информатика, Защита информации</div>
        """.trimIndent()
        val parsed = EmployeeDirectoryParser.parseProfile(profile)!!
        assertEquals(listOf("Информатика", "Защита информации"), parsed.disciplines)
    }
}
