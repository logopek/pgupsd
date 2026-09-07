package ni.shikatu.pgupsd.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class WeekParity {
    ODD,
    EVEN,
    BOTH,
    ;

    fun matches(actual: WeekParity): Boolean =
        this == BOTH || actual == BOTH || this == actual
}

enum class LessonKind {
    LECTURE,
    PRACTICE,
    LAB,
    OTHER,
}

data class Teacher(
    val title: String,
    val name: String,
) {
    val displayName: String
        get() = if (title.isBlank()) name else "$title $name"

    val key: String
        get() = normalizeKey(name)
}

data class DateSpan(
    val from: LocalDate,
    val to: LocalDate,
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(from) && !date.isAfter(to)
}

data class Lesson(
    val id: String,
    val groupName: String,
    val dayOfWeek: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val parity: WeekParity,
    val subject: String,
    val kind: LessonKind,
    val kindLabel: String,
    val teachers: List<Teacher>,
    val rooms: List<String>,
    val subgroup: Int?,
    val valid: DateSpan?,
    val raw: String,
) {
    val subjectKey: String get() = normalizeKey(subject)

    fun visibleOn(date: LocalDate): Boolean {
        if (date.dayOfWeek != dayOfWeek) return false
        if (!parity.matches(isoWeekParity(date))) return false
        if (valid != null && !valid.contains(date)) return false
        return true
    }
}

data class Faculty(
    val code: String,
    val shortName: String,
    val fullName: String,
    val courses: List<Int>,
    val yearFolder: String,
) {
    fun fileUrl(course: Int): String =
        "https://rasp.pgups.ru/files/b/$yearFolder/${code}_${course}.xlsx"
}

data class ScheduleCatalog(
    val faculties: List<Faculty>,
    val currentParityHint: WeekParity?,
)

data class UserProfile(
    val facultyCode: String,
    val facultyShortName: String,
    val facultyFullName: String,
    val course: Int,
    val groupName: String,
    val yearFolder: String,
    val xlsxUrl: String,
)

data class RefreshMeta(
    val lastSuccessMillis: Long?,
    val lastAttemptMillis: Long?,
    val etag: String?,
    val lastModifiedHeader: String?,
    val sourceFileName: String?,
    val title: String?,
    val error: String?,
)

data class LocalNote(
    val id: Long,
    val groupName: String,
    val date: LocalDate,
    val startMinutes: Int,
    val subjectKey: String,
    val subgroup: Int?,
    val text: String,
    val deadline: Boolean = false,
)

data class GlobalNote(
    val id: Long,
    val subjectKey: String,
    val teacherKey: String,
    val subjectLabel: String,
    val teacherLabel: String,
    val text: String,
)

data class TeacherCard(
    val teacher: Teacher,
    val subjects: List<String>,
    val rooms: List<String>,
    val staff: StaffProfile? = null,
) {
    val displayName: String
        get() = staff?.fullName ?: teacher.displayName
}

data class StaffProfile(
    val slug: String,
    val fullName: String,
    val photoUrl: String?,
    val degree: String?,
    val rank: String?,
    val posts: List<String>,
    val phones: List<String>,
    val emails: List<String>,
    val office: String?,
    val disciplines: List<String> = emptyList(),
    val profileUrl: String,
) {
    val department: String?
        get() = posts.firstOrNull()
            ?.substringAfter(" - ", missingDelimiterValue = "")
            ?.ifBlank { posts.firstOrNull() }
}

data class Occurrence(
    val date: LocalDate,
    val lesson: Lesson,
    val localNote: LocalNote?,
    val globalNotes: List<GlobalNote> = emptyList(),
) {
    val key: String
        get() = listOf(
            date.toEpochDay().toString(),
            lesson.id,
            occurrenceKey(
                groupName = lesson.groupName,
                date = date,
                startMinutes = lesson.start.hour * 60 + lesson.start.minute,
                subjectKey = lesson.subjectKey,
                subgroup = lesson.subgroup,
            ),
        ).joinToString("|")
}

fun isoWeekNumber(date: LocalDate): Int =
    date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())

fun isoWeekParity(date: LocalDate): WeekParity =
    if (isoWeekNumber(date) % 2 == 0) WeekParity.EVEN else WeekParity.ODD

fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

private val pairStarts = listOf(
    LocalTime.of(9, 0),
    LocalTime.of(10, 45),
    LocalTime.of(13, 15),
    LocalTime.of(15, 0),
    LocalTime.of(16, 45),
    LocalTime.of(18, 30),
)

fun pairNumber(start: LocalTime): Int {
    val exact = pairStarts.indexOf(start)
    if (exact >= 0) return exact + 1
    val nearest = pairStarts.minBy { startMinutes ->
        kotlin.math.abs((start.toSecondOfDay() - startMinutes.toSecondOfDay()) / 60)
    }
    return pairStarts.indexOf(nearest) + 1
}

fun normalizeKey(value: String): String =
    value.lowercase()
        .replace('ё', 'е')
        .replace(Regex("\\s+"), " ")
        .trim()

fun personMatchKey(fullName: String): String {
    val parts = normalizeKey(fullName)
        .replace(".", " ")
        .split(' ')
        .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    val last = parts.first()
    val initials = parts.drop(1).joinToString("") { it.take(1) }
    return "$last|$initials"
}

fun occurrenceKey(
    groupName: String,
    date: LocalDate,
    startMinutes: Int,
    subjectKey: String,
    subgroup: Int?,
): String = listOf(
    groupName.trim(),
    date.toEpochDay().toString(),
    startMinutes.toString(),
    subjectKey,
    (subgroup ?: 0).toString(),
).joinToString("|")
