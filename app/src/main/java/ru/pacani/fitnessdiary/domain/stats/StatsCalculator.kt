package ru.pacani.fitnessdiary.domain.stats

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

/** Выполненный подход с весом: из таких считаются личные рекорды. */
data class CompletedSet(
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val startedAt: Instant,
)

/** Лучший вес по упражнению и когда он был поднят впервые. */
data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val achievedAt: Instant,
)

/** Период на экране Статистика. [weeks] = null — всё время, с недели первой тренировки. */
enum class StatsPeriod(val weeks: Int?) {
    FOUR_WEEKS(4),
    THREE_MONTHS(13),
    ALL_TIME(null),
}

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

    /**
     * Сколько недель показывать за [period], включая текущую. Для «всего времени» — от недели
     * первой тренировки [firstWorkoutDate], но не меньше [MIN_ALL_TIME_WEEKS]: иначе у нового
     * пользователя график из одного столбца на всю ширину.
     */
    fun periodWeeks(period: StatsPeriod, currentWeekStart: LocalDate, firstWorkoutDate: LocalDate?): Int {
        period.weeks?.let { return it }
        val firstWeek = firstWorkoutDate?.let(::weekStart) ?: return MIN_ALL_TIME_WEEKS
        val weeks = ChronoUnit.WEEKS.between(firstWeek, currentWeekStart).toInt() + 1
        return weeks.coerceAtLeast(MIN_ALL_TIME_WEEKS)
    }

    const val MIN_ALL_TIME_WEEKS = 4

    /**
     * Лучшая завершённая неделя (текущая не считается, она ещё идёт) — та же, что выделена
     * на графике. При равенстве — более поздняя. null, если упражнений не было.
     */
    fun bestWeek(weeks: List<WeekCount>, currentWeekStart: LocalDate): WeekCount? =
        weeks.filter { it.weekStart != currentWeekStart && it.exerciseCount > 0 }
            .maxWithOrNull(compareBy<WeekCount> { it.exerciseCount }.thenBy { it.weekStart })

    /**
     * Личные рекорды: максимальный вес по каждому упражнению и дата, когда он был поднят
     * впервые (повтор того же веса рекордом не считается). Новые рекорды — сверху.
     */
    fun personalRecords(sets: List<CompletedSet>): List<PersonalRecord> =
        sets.groupBy { it.exerciseId }
            .map { (exerciseId, exerciseSets) ->
                val best = exerciseSets.maxOf { it.weightKg }
                val first = exerciseSets.filter { it.weightKg == best }.minBy { it.startedAt }
                PersonalRecord(exerciseId, first.exerciseName, best, first.startedAt)
            }
            .sortedByDescending { it.achievedAt }
}
