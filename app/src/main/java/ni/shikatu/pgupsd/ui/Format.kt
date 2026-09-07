package ni.shikatu.pgupsd.ui

import ni.shikatu.pgupsd.domain.DateSpan
import ni.shikatu.pgupsd.domain.LessonKind
import ni.shikatu.pgupsd.domain.WeekParity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

private val monthGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val monthShort = listOf(
    "янв", "фев", "мар", "апр", "мая", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

fun DayOfWeek.ruFull(): String = when (this) {
    DayOfWeek.MONDAY -> "Понедельник"
    DayOfWeek.TUESDAY -> "Вторник"
    DayOfWeek.WEDNESDAY -> "Среда"
    DayOfWeek.THURSDAY -> "Четверг"
    DayOfWeek.FRIDAY -> "Пятница"
    DayOfWeek.SATURDAY -> "Суббота"
    DayOfWeek.SUNDAY -> "Воскресенье"
}

fun DayOfWeek.ruShort(): String = when (this) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

fun LocalDate.ruLong(): String = "${dayOfMonth} ${monthGenitive[monthValue - 1]}"

fun LocalTime.ru(): String = format(timeFmt)

fun WeekParity.ru(): String = when (this) {
    WeekParity.ODD -> "нечётная"
    WeekParity.EVEN -> "чётная"
    WeekParity.BOTH -> "каждая"
}

fun WeekParity.ruCapital(): String = when (this) {
    WeekParity.ODD -> "Нечётная неделя"
    WeekParity.EVEN -> "Чётная неделя"
    WeekParity.BOTH -> "Каждая неделя"
}

fun LessonKind.ru(): String = when (this) {
    LessonKind.LECTURE -> "Лекция"
    LessonKind.PRACTICE -> "Практика"
    LessonKind.LAB -> "Лабораторная"
    LessonKind.OTHER -> "Занятие"
}

fun formatRange(start: LocalTime, end: LocalTime): String = "${start.ru()}–${end.ru()}"

fun DateSpan.ruShort(): String =
    if (from == to) from.ruLong()
    else if (from.year == to.year && from.month == to.month) "${from.dayOfMonth}–${to.ruLong()}"
    else "${from.ruLong()} – ${to.ruLong()}"

fun LocalDate.ruShort(): String = "${dayOfMonth} ${monthShort[monthValue - 1]}"

fun formatDurationMinutes(minutes: Long): String {
    if (minutes < 60) return "$minutes мин"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0L) "$hours ч" else "$hours ч $rest мин"
}
