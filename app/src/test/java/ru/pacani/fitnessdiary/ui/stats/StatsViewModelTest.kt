package ru.pacani.fitnessdiary.ui.stats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.pacani.fitnessdiary.data.repository.StatsRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.stats.PersonalRecord
import ru.pacani.fitnessdiary.domain.stats.StatsPeriod
import ru.pacani.fitnessdiary.domain.stats.WorkoutStat
import ru.pacani.fitnessdiary.ui.auth.FakeAuthRepository
import ru.pacani.fitnessdiary.util.MainDispatcherRule
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val zone = ZoneId.of("Europe/Moscow")

    // Вторник, 20 октября 2026: текущая неделя с 19 октября
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 20, 12, 0).atZone(zone).toInstant(), zone)

    private val auth = FakeAuthRepository().apply {
        currentUser.value = User(1L, "Иван", "ivan@mail.ru", null, null, Goal.STRENGTH, Instant.EPOCH)
    }
    private val stats = FakeStatsRepository()

    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()
    private fun workout(dateTime: String, exercises: Int) = WorkoutStat(at(dateTime), Duration.ofMinutes(60), exercises)

    private fun TestScope.viewModel() = StatsViewModel(auth, stats, clock).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun periods_filterWorkoutsAndWeeks() = runTest {
        stats.workouts.value = listOf(
            workout("2026-08-05T18:00", exercises = 3), // 11 недель назад
            workout("2026-10-06T18:00", exercises = 4), // 2 недели назад
            workout("2026-10-12T18:00", exercises = 6), // прошлая неделя
            workout("2026-10-20T09:00", exercises = 5), // эта неделя
        )
        val vm = viewModel()

        // По умолчанию 3 месяца = 13 недель: все тренировки
        assertEquals(StatsPeriod.THREE_MONTHS, vm.uiState.value.period)
        assertEquals(13, vm.uiState.value.weeks.size)
        assertEquals(4, vm.uiState.value.summary.workoutCount)
        assertEquals(18, vm.uiState.value.summary.exerciseCount)

        vm.onPeriodChange(StatsPeriod.FOUR_WEEKS)
        val fourWeeks = vm.uiState.value
        assertEquals(listOf(0, 4, 6, 5), fourWeeks.weeks.map { it.exerciseCount })
        assertEquals(3, fourWeeks.summary.workoutCount)
        assertEquals(15.0 / 4, fourWeeks.averageExercisesPerWeek, 1e-9)
        assertEquals(LocalDate.of(2026, 10, 12), fourWeeks.bestWeek?.weekStart)

        // Всё время: с недели первой тренировки (3 августа) — 12 недель
        vm.onPeriodChange(StatsPeriod.ALL_TIME)
        assertEquals(12, vm.uiState.value.weeks.size)
        assertEquals(LocalDate.of(2026, 8, 3), vm.uiState.value.weeks.first().weekStart)
    }

    @Test
    fun records_markNewOnesFromThisWeek() = runTest {
        stats.records.value = listOf(
            PersonalRecord(1, "Жим лёжа", 85.0, at("2026-10-19T18:00")),
            PersonalRecord(2, "Присед", 110.0, at("2026-10-15T18:00")),
        )
        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(2, state.records.size)
        assertTrue(state.isNew(state.records[0]))
        assertFalse(state.isNew(state.records[1]))
    }

    @Test
    fun noData_isEmpty() = runTest {
        val vm = viewModel()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isEmpty)
        assertEquals(null, vm.uiState.value.bestWeek)
    }
}

private class FakeStatsRepository : StatsRepository {
    val workouts = MutableStateFlow<List<WorkoutStat>>(emptyList())
    val records = MutableStateFlow<List<PersonalRecord>>(emptyList())

    override fun observeFinishedWorkouts(userId: Long, from: Instant): Flow<List<WorkoutStat>> =
        workouts.map { list -> list.filter { it.startedAt >= from } }

    override fun observePersonalRecords(userId: Long): Flow<List<PersonalRecord>> = records
}
