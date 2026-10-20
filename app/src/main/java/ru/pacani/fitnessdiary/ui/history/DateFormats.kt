package ru.pacani.fitnessdiary.ui.history

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ru: Locale = Locale.forLanguageTag("ru")
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", ru)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", ru)
private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE, d MMMM", ru)
private val time = DateTimeFormatter.ofPattern("HH:mm", ru)

/** «5 – 11 октября», «29 сентября – 5 октября», с годом, если неделя не из текущего года. */
fun formatWeekRange(start: LocalDate, end: LocalDate, today: LocalDate): String {
    val withYear = end.year != today.year
    val endText = (if (withYear) dayMonthYear else dayMonth).format(end)
    val startText = if (start.month == end.month && start.year == end.year) {
        start.dayOfMonth.toString()
    } else {
        (if (start.year != end.year) dayMonthYear else dayMonth).format(start)
    }
    return "$startText – $endText"
}

/** «пн», «вт» для блока с датой в карточке. */
fun formatWeekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, ru).lowercase(ru)

/** «Понедельник, 19 октября · 18:30 – 19:28». */
fun formatWorkoutDateLine(start: ZonedDateTime, end: ZonedDateTime?): String {
    val date = weekdayDayMonth.format(start).replaceFirstChar { it.titlecase(ru) }
    val times = if (end != null) "${time.format(start)} – ${time.format(end)}" else time.format(start)
    return "$date · $times"
}
