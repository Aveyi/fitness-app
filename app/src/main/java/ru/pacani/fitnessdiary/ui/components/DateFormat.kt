package ru.pacani.fitnessdiary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import ru.pacani.fitnessdiary.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Russian = Locale.forLanguageTag("ru")
private val HeaderFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Russian)
private val LongDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Russian)
private val WeekdayFormat = DateTimeFormatter.ofPattern("EE", Russian)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm", Russian)

/** «14 сен» — подписи недель на графике. Сокращения без точки, как в макете (у Java было бы «сент.»). */
@Composable
fun shortDate(date: LocalDate): String {
    val months = stringArrayResource(R.array.months_short)
    return "${date.dayOfMonth} ${months[date.monthValue - 1]}"
}

/** «Вторник, 20 октября» — шапка Главной. */
fun headerDate(date: LocalDate): String = HeaderFormat.format(date).replaceFirstChar { it.titlecase(Russian) }

/** «3 августа 2026» — «С нами с …» в Профиле. */
fun longDate(date: LocalDate): String = LongDateFormat.format(date)

/** «пн» — день недели на карточке тренировки. */
fun weekdayShort(date: LocalDate): String = WeekdayFormat.format(date)

/** «18:30» */
fun timeOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String = TimeFormat.format(instant.atZone(zone))
