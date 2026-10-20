package ru.pacani.fitnessdiary.domain.stats

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Завершённая тренировка в том виде, который нужен статистике. */
data class WorkoutStat(
    val startedAt: Instant,
    val duration: Duration,
    /** Упражнения, где отмечен хотя бы один подход. */
    val completedExerciseCount: Int,
)

data class StatsSummary(
    val workoutCount: Int,
    val exerciseCount: Int,
    /** Время в зале: сумма длительностей тренировок. */
    val totalDuration: Duration,
) {
    /** Среднее число выполненных упражнений за тренировку; 0, если тренировок нет. */
    val averageExercisesPerWorkout: Double
        get() = if (workoutCount == 0) 0.0 else exerciseCount.toDouble() / workoutCount

    companion object {
        val EMPTY = StatsSummary(workoutCount = 0, exerciseCount = 0, totalDuration = Duration.ZERO)
    }
}

/** Выполненные упражнения за неделю, которая начинается в понедельник [weekStart]. */
data class WeekCount(
    val weekStart: LocalDate,
    val exerciseCount: Int,
)

/** Расчёты статистики (раздел 7 context.md). Чистые функции: время и часовой пояс приходят снаружи. */
object StatsCalculator {

    /** Понедельник недели, в которую попадает [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun summarize(workouts: List<WorkoutStat>): StatsSummary = StatsSummary(
        workoutCount = workouts.size,
        exerciseCount = workouts.sumOf { it.completedExerciseCount },
        totalDuration = workouts.fold(Duration.ZERO) { total, w -> total + w.duration },
    )

    /**
     * Выполненные упражнения по неделям: [weeks] недель подряд, последняя — та, что начинается
     * в [currentWeekStart]. Недели без тренировок тоже попадают в список (с нулём),
     * чтобы на графике были пустые столбцы, а не пропуски. Тренировка относится к неделе
     * по дате начала в часовом поясе [zone]. Порядок — от старых к новым.
     */
    fun exercisesPerWeek(
        workouts: List<WorkoutStat>,
        currentWeekStart: LocalDate,
        weeks: Int,
        zone: ZoneId,
    ): List<WeekCount> {
        require(weeks > 0) { "weeks must be positive" }
        val countsByWeek = workouts
            .groupBy { weekStart(it.startedAt.atZone(zone).toLocalDate()) }
            .mapValues { (_, list) -> list.sumOf { it.completedExerciseCount } }
        return (weeks - 1 downTo 0).map { weeksAgo ->
            val start = currentWeekStart.minusWeeks(weeksAgo.toLong())
            WeekCount(start, countsByWeek[start] ?: 0)
        }
    }

    /** Начало недели [weekStart] как момент времени: для запроса «тренировки с ...». */
    fun startOf(weekStart: LocalDate, zone: ZoneId): Instant = weekStart.atStartOfDay(zone).toInstant()
}
