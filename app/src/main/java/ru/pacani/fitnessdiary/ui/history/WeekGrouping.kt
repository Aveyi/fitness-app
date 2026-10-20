package ru.pacani.fitnessdiary.ui.history

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import ru.pacani.fitnessdiary.domain.model.Workout

/** Заголовок группы в Истории. */
sealed interface WeekLabel {
    data object ThisWeek : WeekLabel
    data object LastWeek : WeekLabel

    /** Неделя с понедельника [start] по воскресенье [end]: «5 – 11 октября». */
    data class Range(val start: LocalDate, val end: LocalDate) : WeekLabel
}

data class WeekGroup(
    val label: WeekLabel,
    val workouts: List<Workout>,
)

/** Понедельник недели, в которую попадает дата. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Группирует тренировки по неделям (неделя с понедельника), сохраняя порядок:
 * тренировки приходят новыми сверху, группы идут так же.
 */
fun groupByWeek(workouts: List<Workout>, today: LocalDate, zone: ZoneId): List<WeekGroup> {
    val thisWeek = today.weekStart()
    val lastWeek = thisWeek.minusWeeks(1)
    return workouts
        .groupBy { it.startedAt.atZone(zone).toLocalDate().weekStart() }
        .map { (monday, items) ->
            val label = when (monday) {
                thisWeek -> WeekLabel.ThisWeek
                lastWeek -> WeekLabel.LastWeek
                else -> WeekLabel.Range(monday, monday.plusDays(6))
            }
            WeekGroup(label, items)
        }
}
