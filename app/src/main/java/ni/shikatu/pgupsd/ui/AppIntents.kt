package ni.shikatu.pgupsd.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import ni.shikatu.pgupsd.domain.Occurrence
import java.time.ZoneId

fun addLessonToCalendar(context: Context, occurrence: Occurrence) {
    val lesson = occurrence.lesson
    val zone = ZoneId.systemDefault()
    val begin = occurrence.date.atTime(lesson.start).atZone(zone).toInstant().toEpochMilli()
    val end = occurrence.date.atTime(lesson.end).atZone(zone).toInstant().toEpochMilli()
    val description = buildString {
        val teachers = lesson.teachers.joinToString { it.displayName }
        if (teachers.isNotBlank()) appendLine(teachers)
        val note = occurrence.localNote
        if (note != null) {
            if (note.text.isNotBlank()) appendLine(note.text.trim())
            if (note.deadline) appendLine("Отслеживаю")
        }
        occurrence.globalNotes.forEach { global ->
            if (global.text.isNotBlank()) appendLine(global.text.trim())
        }
    }.trim()
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.TITLE, lesson.subject)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, lesson.rooms.joinToString())
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
    startExternal(context, intent, "Нет приложения календаря")
}

fun dialPhone(context: Context, raw: String) {
    val number = raw.filter { it.isDigit() || it == '+' }
    if (number.isEmpty()) return
    startExternal(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")), "Не удалось открыть телефон")
}

fun sendEmail(context: Context, address: String) {
    val email = address.trim()
    if (email.isEmpty()) return
    startExternal(
        context,
        Intent(Intent.ACTION_SENDTO).setData(Uri.fromParts("mailto", email, null)),
        "Нет почтового приложения",
    )
}

private fun startExternal(context: Context, intent: Intent, error: String) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }
}
