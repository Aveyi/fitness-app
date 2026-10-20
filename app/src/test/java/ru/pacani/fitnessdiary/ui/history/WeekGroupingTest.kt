package ru.pacani.fitnessdiary.ui.history

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pacani.fitnessdiary.domain.model.Workout

class WeekGroupingTest {

    private val zone = ZoneId.of("Europe/Moscow")

    /** Вторник, 20 октября 2026 */
    private val today = LocalDate.of(2026, 10, 20)

    private fun workout(id: Long, date: LocalDate, hour: Int = 18): Workout {
        val start = date.atTime(hour, 30).atZone(zone).toInstant()
        return Workout(id, 1, "Тренировка $id", start, start.plus(Duration.ofMinutes(60)), null)
    }

    @Test
    fun `тренировки делятся на эту неделю, прошлую и диапазоны`() {
        val workouts = listOf(
            workout(1, LocalDate.of(2026, 10, 19)), // пн этой недели
            workout(2, LocalDate.of(2026, 10, 17)), // сб прошлой
            workout(3, LocalDate.of(2026, 10, 13)), // вт прошлой
            workout(4, LocalDate.of(2026, 10, 10)), // сб 5–11 октября
            workout(5, LocalDate.of(2026, 10, 5)), // пн 5–11 октября
        )

        val groups = groupByWeek(workouts, today, zone)

        assertEquals(
            listOf(
                WeekLabel.ThisWeek,
                WeekLabel.LastWeek,
                WeekLabel.Range(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)),
            ),
            groups.map { it.label },
        )
        assertEquals(listOf(listOf(1L), listOf(2L, 3L), listOf(4L, 5L)), groups.map { g -> g.workouts.map { it.id } })
    }

    @Test
    fun `воскресенье относится к своей неделе, а не к следующей`() {
        val sunday = LocalDate.of(2026, 10, 18)
        val groups = groupByWeek(listOf(workout(1, sunday, hour = 23)), today, zone)
        assertEquals(WeekLabel.LastWeek, groups.single().label)
    }

    @Test
    fun `пустой список даёт пустые группы`() {
        assertEquals(emptyList<WeekGroup>(), groupByWeek(emptyList(), today, zone))
    }

    @Test
    fun `заголовок недели внутри одного месяца`() {
        assertEquals(
            "5 – 11 октября",
            formatWeekRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11), today),
        )
    }

    @Test
    fun `заголовок недели на стыке месяцев`() {
        assertEquals(
            "28 сентября – 4 октября",
            formatWeekRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4), today),
        )
    }

    @Test
    fun `у недели прошлого года пишется год`() {
        assertEquals(
            "29 декабря 2025 – 4 января 2026",
            formatWeekRange(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4), LocalDate.of(2027, 3, 1)),
        )
        assertEquals(
            "3 – 9 марта 2025",
            formatWeekRange(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 9), today),
        )
    }

    @Test
    fun `короткий день недели и строка даты`() {
        assertEquals("пн", formatWeekdayShort(LocalDate.of(2026, 10, 19)))
        val start = LocalDate.of(2026, 10, 19).atTime(18, 30).atZone(zone)
        assertEquals(
            "Понедельник, 19 октября · 18:30 – 19:28",
            formatWorkoutDateLine(start, start.plusMinutes(58)),
        )
    }
}
