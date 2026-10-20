package ru.pacani.fitnessdiary.ui.home

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.pacani.fitnessdiary.data.repository.StatsRepository
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.stats.PersonalRecord
import ru.pacani.fitnessdiary.domain.stats.WorkoutStat
import ru.pacani.fitnessdiary.ui.auth.FakeAuthRepository
import ru.pacani.fitnessdiary.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val zone = ZoneId.of("Europe/Moscow")

    // Вторник, 20 октября 2026: текущая неделя начинается 19 октября
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 20, 12, 0).atZone(zone).toInstant(), zone)

    private val auth = FakeAuthRepository().apply {
        currentUser.value = User(1L, "Иван", "ivan@mail.ru", 180, 78.0, Goal.STRENGTH, Instant.EPOCH)
    }
    private val workouts = FakeWorkoutRepository()
    private val stats = FakeStatsRepository()

    private fun viewModel() = HomeViewModel(auth, workouts, stats, clock)

    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    @Test
    fun thisWeekAndChart() = runTest {
        stats.workouts.value = listOf(
            WorkoutStat(at("2026-10-12T18:00"), Duration.ofMinutes(60), 6), // прошлая неделя
            WorkoutStat(at("2026-10-19T18:00"), Duration.ofMinutes(58), 5), // эта неделя
        )
        val vm = viewModel()
        backgroundCollect(vm)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2026, 10, 20), state.today)
        assertEquals(1, state.thisWeek.workoutCount)
        assertEquals(5, state.thisWeek.exerciseCount)
        assertEquals(Duration.ofMinutes(58), state.thisWeek.totalDuration)

        assertEquals(HomeViewModel.CHART_WEEKS, state.weeks.size)
        assertEquals(LocalDate.of(2026, 9, 14), state.weeks.first().weekStart)
        assertEquals(listOf(0, 0, 0, 0, 6, 5), state.weeks.map { it.exerciseCount })
        // Запрос за 6 недель: с понедельника 14 сентября
        assertEquals(at("2026-09-14T00:00"), stats.requestedFrom)
    }

    @Test
    fun start_createsWorkout_andOpensIt() = runTest {
        val vm = viewModel()
        backgroundCollect(vm)

        vm.onStartWorkout("Тренировка")

        assertEquals(listOf(1L to "Тренировка"), workouts.started)
        assertEquals(FakeWorkoutRepository.NEW_WORKOUT_ID, vm.uiState.value.openWorkoutId)

        vm.onWorkoutOpened()
        assertNull(vm.uiState.value.openWorkoutId)
    }

    @Test
    fun continue_opensActiveWorkout_withoutCreatingNew() = runTest {
        workouts.active.value = workout(id = 7L, finishedAt = null)
        val vm = viewModel()
        backgroundCollect(vm)

        vm.onStartWorkout("Тренировка")

        assertTrue(workouts.started.isEmpty())
        assertEquals(7L, vm.uiState.value.openWorkoutId)
    }

    @Test
    fun whileStarting_secondTapIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        workouts.gate = gate
        val vm = viewModel()
        backgroundCollect(vm)

        vm.onStartWorkout("Тренировка")
        assertTrue(vm.uiState.value.isStartingWorkout)
        vm.onStartWorkout("Тренировка")

        gate.complete(Unit)
        assertEquals(1, workouts.started.size)
        assertFalse(vm.uiState.value.isStartingWorkout)
    }

    @Test
    fun lastWorkout_isShown() = runTest {
        workouts.last.value = workout(id = 3L, finishedAt = at("2026-10-19T19:00"))
        val vm = viewModel()
        backgroundCollect(vm)

        assertEquals(3L, vm.uiState.value.lastWorkout?.id)
        assertEquals("Иван", vm.uiState.value.user?.name)
    }

    private fun TestScope.backgroundCollect(vm: HomeViewModel) {
        // uiState запускается по подписке (WhileSubscribed), как на экране
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun workout(id: Long, finishedAt: Instant?) = Workout(
        id = id,
        userId = 1L,
        title = "Грудь + трицепс",
        startedAt = at("2026-10-19T18:00"),
        finishedAt = finishedAt,
        note = null,
        exerciseCount = 5,
        completedSetCount = 15,
    )
}

private class FakeStatsRepository : StatsRepository {
    val workouts = MutableStateFlow<List<WorkoutStat>>(emptyList())
    var requestedFrom: Instant? = null

    override fun observeFinishedWorkouts(userId: Long, from: Instant): Flow<List<WorkoutStat>> {
        requestedFrom = from
        return workouts.map { list -> list.filter { it.startedAt >= from } }
    }

    override fun observePersonalRecords(userId: Long): Flow<List<PersonalRecord>> = error("not used")
}

/** Только то, что нужно Главной; остальные методы в этих тестах не вызываются. */
private class FakeWorkoutRepository : WorkoutRepository {
    val active = MutableStateFlow<Workout?>(null)
    val last = MutableStateFlow<Workout?>(null)
    val started = mutableListOf<Pair<Long, String>>()
    var gate: CompletableDeferred<Unit>? = null

    override fun observeActiveWorkout(userId: Long): Flow<Workout?> = active
    override fun observeLastFinishedWorkout(userId: Long): Flow<Workout?> = last

    override suspend fun startWorkout(userId: Long, title: String): Long {
        started += userId to title
        gate?.await()
        return NEW_WORKOUT_ID
    }

    override fun observeFinishedWorkouts(userId: Long): Flow<List<Workout>> = error("not used")
    override fun observeWorkout(workoutId: Long): Flow<WorkoutDetails?> = error("not used")
    override suspend fun getActiveWorkout(userId: Long): Workout? = active.value
    override suspend fun finishWorkout(workoutId: Long) = error("not used")
    override suspend fun deleteWorkout(workoutId: Long) = error("not used")
    override suspend fun updateWorkoutTitle(workoutId: Long, title: String) = error("not used")
    override suspend fun updateNote(workoutId: Long, note: String?) = error("not used")
    override suspend fun addExercise(workoutId: Long, exerciseId: Long): Long = error("not used")
    override suspend fun removeExercise(workoutExerciseId: Long) = error("not used")
    override suspend fun addSet(workoutExerciseId: Long): Long = error("not used")
    override suspend fun updateSet(setId: Long, weightKg: Double, reps: Int) = error("not used")
    override suspend fun toggleSetCompleted(setId: Long) = error("not used")
    override suspend fun deleteSet(setId: Long) = error("not used")
    override suspend fun getBestWeightBefore(userId: Long, exerciseId: Long, beforeWorkoutStartedAt: Long): Double? =
        error("not used")

    companion object {
        const val NEW_WORKOUT_ID = 42L
    }
}
