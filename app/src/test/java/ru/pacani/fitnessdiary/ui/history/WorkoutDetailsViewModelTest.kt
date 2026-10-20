package ru.pacani.fitnessdiary.ui.history

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.repository.FakeWorkoutDao
import ru.pacani.fitnessdiary.data.repository.ImmediateTransactionRunner
import ru.pacani.fitnessdiary.data.repository.RoomWorkoutRepository
import ru.pacani.fitnessdiary.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = 1L
    private val bench = 101L
    private val squat = 102L

    private lateinit var dao: FakeWorkoutDao
    private lateinit var repository: RoomWorkoutRepository
    private var now = 0L

    private fun createRepository() {
        dao = FakeWorkoutDao().apply {
            exercises += ExerciseEntity(id = bench, name = "Жим штанги лёжа", category = "Chest")
            exercises += ExerciseEntity(id = squat, name = "Приседания", category = "Legs")
        }
        repository = RoomWorkoutRepository(dao, ImmediateTransactionRunner(), now = { now })
    }

    /** Завершённая тренировка: упражнение и выполненные подходы с указанным весом. */
    private suspend fun finishedWorkout(exerciseId: Long, vararg weights: Double): Long {
        now += 86_400_000L
        val workoutId = repository.startWorkout(userId, "Тренировка")
        val weId = repository.addExercise(workoutId, exerciseId)
        // addExercise создаёт первый пустой подход, остальные добавляем
        repeat(weights.size - 1) { repository.addSet(weId) }
        dao.sets.filter { it.workoutExerciseId == weId }.zip(weights.toList()).forEach { (set, weight) ->
            repository.updateSet(set.id, weight, 5)
            repository.toggleSetCompleted(set.id)
        }
        now += 3_600_000L
        repository.finishWorkout(workoutId)
        return workoutId
    }

    private fun TestScope.viewModelFor(workoutId: Long): WorkoutDetailsViewModel {
        val vm = WorkoutDetailsViewModel(workoutId, repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    private fun WorkoutDetailsViewModel.loaded() = uiState.value as WorkoutDetailsUiState.Loaded

    @Test
    fun `больший вес, чем в прошлых тренировках, отмечается рекордом`() = runTest {
        createRepository()
        finishedWorkout(bench, 80.0, 82.5)
        val second = finishedWorkout(bench, 80.0, 85.0)

        val vm = viewModelFor(second)

        assertEquals(85.0, vm.loaded().exercises.single().recordWeightKg)
    }

    @Test
    fun `тот же вес рекордом не считается`() = runTest {
        createRepository()
        finishedWorkout(bench, 85.0)
        val second = finishedWorkout(bench, 85.0)

        assertNull(viewModelFor(second).loaded().exercises.single().recordWeightKg)
    }

    @Test
    fun `первое выполнение упражнения не рекорд`() = runTest {
        createRepository()
        finishedWorkout(bench, 100.0)
        val squatWorkout = finishedWorkout(squat, 60.0)

        assertNull(viewModelFor(squatWorkout).loaded().exercises.single().recordWeightKg)
    }

    @Test
    fun `рекорд считается только по прошлым тренировкам, а не по будущим`() = runTest {
        createRepository()
        val first = finishedWorkout(bench, 80.0)
        finishedWorkout(bench, 90.0)

        assertNull(viewModelFor(first).loaded().exercises.single().recordWeightKg)
    }

    @Test
    fun `удаление закрывает экран`() = runTest {
        createRepository()
        val workoutId = finishedWorkout(bench, 80.0)
        val vm = viewModelFor(workoutId)
        val events = mutableListOf<WorkoutDetailsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.delete()
        runCurrent()

        assertEquals(listOf<WorkoutDetailsEvent>(WorkoutDetailsEvent.Deleted), events)
        assertTrue(repository.getActiveWorkout(userId) == null)
    }
}
