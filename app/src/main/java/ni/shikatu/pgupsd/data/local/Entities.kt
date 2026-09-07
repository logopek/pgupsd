package ni.shikatu.pgupsd.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ni.shikatu.pgupsd.domain.DateSpan
import ni.shikatu.pgupsd.domain.GlobalNote
import ni.shikatu.pgupsd.domain.Lesson
import ni.shikatu.pgupsd.domain.LessonKind
import ni.shikatu.pgupsd.domain.LocalNote
import ni.shikatu.pgupsd.domain.StaffProfile
import ni.shikatu.pgupsd.domain.Teacher
import ni.shikatu.pgupsd.domain.WeekParity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val groupName: String,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val parity: String,
    val subject: String,
    val kind: String,
    val kindLabel: String,
    val teachers: String,
    val rooms: String,
    val subgroup: Int,
    val validFrom: Long?,
    val validTo: Long?,
    val raw: String,
) {
    fun toDomain(): Lesson = Lesson(
        id = id,
        groupName = groupName,
        dayOfWeek = DayOfWeek.of(dayOfWeek),
        start = LocalTime.of(startMinutes / 60, startMinutes % 60),
        end = LocalTime.of(endMinutes / 60, endMinutes % 60),
        parity = WeekParity.valueOf(parity),
        subject = subject,
        kind = LessonKind.valueOf(kind),
        kindLabel = kindLabel,
        teachers = decodeTeachers(teachers),
        rooms = decodeList(rooms),
        subgroup = subgroup.takeIf { it != 0 },
        valid = if (validFrom != null && validTo != null) {
            DateSpan(LocalDate.ofEpochDay(validFrom), LocalDate.ofEpochDay(validTo))
        } else {
            null
        },
        raw = raw,
    )

    companion object {
        fun from(lesson: Lesson): LessonEntity = LessonEntity(
            id = lesson.id,
            groupName = lesson.groupName,
            dayOfWeek = lesson.dayOfWeek.value,
            startMinutes = lesson.start.hour * 60 + lesson.start.minute,
            endMinutes = lesson.end.hour * 60 + lesson.end.minute,
            parity = lesson.parity.name,
            subject = lesson.subject,
            kind = lesson.kind.name,
            kindLabel = lesson.kindLabel,
            teachers = encodeTeachers(lesson.teachers),
            rooms = encodeList(lesson.rooms),
            subgroup = lesson.subgroup ?: 0,
            validFrom = lesson.valid?.from?.toEpochDay(),
            validTo = lesson.valid?.to?.toEpochDay(),
            raw = lesson.raw,
        )
    }
}

@Entity(
    tableName = "local_notes",
    indices = [Index(value = ["groupName", "dateEpoch", "startMinutes", "subjectKey", "subgroup"], unique = true)],
)
data class LocalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupName: String,
    val dateEpoch: Long,
    val startMinutes: Int,
    val subjectKey: String,
    val subgroup: Int,
    val text: String,
    val dueEpoch: Long? = null,
    val updatedAt: Long,
) {
    fun toDomain(): LocalNote = LocalNote(
        id = id,
        groupName = groupName,
        date = LocalDate.ofEpochDay(dateEpoch),
        startMinutes = startMinutes,
        subjectKey = subjectKey,
        subgroup = subgroup.takeIf { it != 0 },
        text = text,
        deadline = dueEpoch != null,
    )
}

@Entity(
    tableName = "global_notes",
    indices = [Index(value = ["subjectKey", "teacherKey"], unique = true)],
)
data class GlobalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectKey: String,
    val teacherKey: String,
    val subjectLabel: String,
    val teacherLabel: String,
    val text: String,
    val updatedAt: Long,
) {
    fun toDomain(): GlobalNote = GlobalNote(
        id = id,
        subjectKey = subjectKey,
        teacherKey = teacherKey,
        subjectLabel = subjectLabel,
        teacherLabel = teacherLabel,
        text = text,
    )
}

internal fun encodeList(items: List<String>): String = items.joinToString("\n")

internal fun decodeList(raw: String): List<String> =
    raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

internal fun encodeTeachers(items: List<Teacher>): String =
    items.joinToString("\n") { "${it.title}\t${it.name}" }

internal fun decodeTeachers(raw: String): List<Teacher> =
    raw.split('\n').mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        val parts = line.split('\t', limit = 2)
        if (parts.size == 2) Teacher(parts[0], parts[1]) else Teacher("", parts[0])
    }

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val slug: String,
    val fullName: String,
    val photoUrl: String?,
    val degree: String?,
    val rank: String?,
    val posts: String,
    val phones: String,
    val emails: String,
    val office: String?,
    val disciplines: String,
    val profileUrl: String,
) {
    fun toDomain(): StaffProfile =
        StaffProfile(
            slug = slug,
            fullName = fullName,
            photoUrl = photoUrl,
            degree = degree,
            rank = rank,
            posts = decodeList(posts),
            phones = decodeList(phones),
            emails = decodeList(emails),
            office = office,
            disciplines = decodeList(disciplines),
            profileUrl = profileUrl,
        )

    companion object {
        fun from(profile: StaffProfile): StaffEntity = StaffEntity(
            slug = profile.slug,
            fullName = profile.fullName,
            photoUrl = profile.photoUrl,
            degree = profile.degree,
            rank = profile.rank,
            posts = encodeList(profile.posts),
            phones = encodeList(profile.phones),
            emails = encodeList(profile.emails),
            office = profile.office,
            disciplines = encodeList(profile.disciplines),
            profileUrl = profile.profileUrl,
        )
    }
}
