package ru.pacani.fitnessdiary.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {

    private val zone = ZoneId.of("Europe/Moscow")

    private fun workout(dateTime: String, minutes: Long = 60, exercises: Int = 5) = WorkoutStat(
        startedAt = LocalDateTime.parse(dateTime).atZone(zone).toInstant(),
        duration = Duration.ofMinutes(minutes),
        completedExerciseCount = exercises,
    )

    @Test
    fun weekStart_isMonday() {
        assertEquals(LocalDate.of(2026, 10, 19), StatsCalculator.weekStart(LocalDate.of(2026, 10, 19))) // пн
        assertEquals(LocalDate.of(2026, 10, 19), StatsCalculator.weekStart(LocalDate.of(2026, 10, 20))) // вт
        assertEquals(LocalDate.of(2026, 10, 19), StatsCalculator.weekStart(LocalDate.of(2026, 10, 25))) // вс
        assertEquals(LocalDate.of(2026, 10, 26), StatsCalculator.weekStart(LocalDate.of(2026, 10, 26)))
        // Неделя через границу месяца и года
        assertEquals(LocalDate.of(2025, 12, 29), StatsCalculator.weekStart(LocalDate.of(2026, 1, 2)))
    }

    @Test
    fun summarize() {
        val summary = StatsCalculator.summarize(
            listOf(workout("2026-10-19T18:00", minutes = 58, exercises = 5), workout("2026-10-21T18:00", minutes = 70, exercises = 4)),
        )
        assertEquals(2, summary.workoutCount)
        assertEquals(9, summary.exerciseCount)
        assertEquals(Duration.ofMinutes(128), summary.totalDuration)
        assertEquals(4.5, summary.averageExercisesPerWorkout, 0.0)
    }

    @Test
    fun summarize_empty() {
        assertEquals(StatsSummary.EMPTY, StatsCalculator.summarize(emptyList()))
        assertEquals(0.0, StatsSummary.EMPTY.averageExercisesPerWorkout, 0.0)
    }

    @Test
    fun exercisesPerWeek_groupsByMonday_andKeepsEmptyWeeks() {
        val workouts = listOf(
            workout("2026-09-28T10:00", exercises = 3), // пн, неделя 28 сен
            workout("2026-10-04T21:00", exercises = 4), // вс, та же неделя
            workout("2026-10-12T09:00", exercises = 6), // неделя 12 окт
            workout("2026-10-20T18:00", exercises = 5), // текущая неделя 19 окт
        )

        val weeks = StatsCalculator.exercisesPerWeek(
            workouts,
            currentWeekStart = LocalDate.of(2026, 10, 19),
            weeks = 4,
            zone = zone,
        )

        assertEquals(
            listOf(
                WeekCount(LocalDate.of(2026, 9, 28), 7),
                WeekCount(LocalDate.of(2026, 10, 5), 0), // пустая неделя остаётся
                WeekCount(LocalDate.of(2026, 10, 12), 6),
                WeekCount(LocalDate.of(2026, 10, 19), 5),
            ),
            weeks,
        )
    }

    @Test
    fun exercisesPerWeek_usesLocalDate_notUtc() {
        // Понедельник 00:30 по Москве — это ещё воскресенье по UTC; неделя должна быть новой
        val weeks = StatsCalculator.exercisesPerWeek(
            listOf(workout("2026-10-19T00:30", exercises = 2)),
            currentWeekStart = LocalDate.of(2026, 10, 19),
            weeks = 2,
            zone = zone,
        )
        assertEquals(listOf(0, 2), weeks.map { it.exerciseCount })
    }

    @Test
    fun exercisesPerWeek_ignoresWorkoutsOutsideRange() {
        val weeks = StatsCalculator.exercisesPerWeek(
            listOf(workout("2026-01-05T10:00", exercises = 9)),
            currentWeekStart = LocalDate.of(2026, 10, 19),
            weeks = 6,
            zone = zone,
        )
        assertEquals(6, weeks.size)
        assertEquals(0, weeks.sumOf { it.exerciseCount })
        assertEquals(LocalDate.of(2026, 9, 14), weeks.first().weekStart)
    }

    @Test
    fun startOf_isMidnightInZone() {
        val instant = StatsCalculator.startOf(LocalDate.of(2026, 10, 19), zone)
        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0), LocalDateTime.ofInstant(instant, zone))
    }

    @Test
    fun periodWeeks_fixedPeriods() {
        val current = LocalDate.of(2026, 10, 19)
        assertEquals(4, StatsCalculator.periodWeeks(StatsPeriod.FOUR_WEEKS, current, LocalDate.of(2020, 1, 1)))
        assertEquals(13, StatsCalculator.periodWeeks(StatsPeriod.THREE_MONTHS, current, null))
    }

    @Test
    fun periodWeeks_allTime_fromFirstWorkoutWeek() {
        val current = LocalDate.of(2026, 10, 19)
        // Первая тренировка в среду 5 августа: её неделя — с 3 августа, до 19 октября 12 недель
        assertEquals(12, StatsCalculator.periodWeeks(StatsPeriod.ALL_TIME, current, LocalDate.of(2026, 8, 5)))
        // Первая тренировка на этой неделе: всё равно 4 недели, а не один столбец на весь график
        assertEquals(4, StatsCalculator.periodWeeks(StatsPeriod.ALL_TIME, current, LocalDate.of(2026, 10, 20)))
        // Тренировок нет
        assertEquals(4, StatsCalculator.periodWeeks(StatsPeriod.ALL_TIME, current, null))
    }

    @Test
    fun bestWeek_ignoresCurrentWeek_andPrefersLaterOnTie() {
        val current = LocalDate.of(2026, 10, 19)
        val weeks = listOf(
            WeekCount(LocalDate.of(2026, 9, 28), 14),
            WeekCount(LocalDate.of(2026, 10, 5), 9),
            WeekCount(LocalDate.of(2026, 10, 12), 14), // та же сумма, но позже — она и лучшая
            WeekCount(current, 20), // текущая неделя ещё идёт, не считается
        )
        assertEquals(LocalDate.of(2026, 10, 12), StatsCalculator.bestWeek(weeks, current)?.weekStart)
    }

    @Test
    fun bestWeek_noneWhenEmpty() {
        val current = LocalDate.of(2026, 10, 19)
        assertEquals(null, StatsCalculator.bestWeek(listOf(WeekCount(LocalDate.of(2026, 10, 12), 0)), current))
        assertEquals(null, StatsCalculator.bestWeek(emptyList(), current))
    }

    @Test
    fun personalRecords_maxWeightFirstAchieved_newestFirst() {
        fun set(id: Long, name: String, weight: Double, dateTime: String) =
            CompletedSet(id, name, weight, LocalDateTime.parse(dateTime).atZone(zone).toInstant())

        val records = StatsCalculator.personalRecords(
            listOf(
                set(1, "Жим лёжа", 80.0, "2026-10-05T18:00"),
                set(1, "Жим лёжа", 85.0, "2026-10-12T18:00"), // рекорд впервые
                set(1, "Жим лёжа", 85.0, "2026-10-19T18:00"), // повтор — не новый рекорд
                set(2, "Присед", 110.0, "2026-10-15T18:00"),
                set(2, "Присед", 100.0, "2026-10-19T18:00"),
            ),
        )

        assertEquals(listOf(2L, 1L), records.map { it.exerciseId }) // присед поднят позже
        assertEquals(110.0, records[0].weightKg, 0.0)
        assertEquals(85.0, records[1].weightKg, 0.0)
        assertEquals(
            LocalDateTime.of(2026, 10, 12, 18, 0),
            LocalDateTime.ofInstant(records[1].achievedAt, zone),
        )
    }

    @Test
    fun personalRecords_empty() {
        assertEquals(emptyList<PersonalRecord>(), StatsCalculator.personalRecords(emptyList()))
    }
}
