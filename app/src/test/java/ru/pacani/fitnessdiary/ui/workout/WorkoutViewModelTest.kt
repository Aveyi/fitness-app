package ru.pacani.fitnessdiary.ui.workout

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = 1L
    private val benchPressId = 101L

    private class Setup(
        val repository: RoomWorkoutRepository,
        val viewModel: WorkoutViewModel,
        val workoutId: Long,
        val events: MutableList<WorkoutEvent>,
    )

    /** Тренировка в фейковой базе + ViewModel, на состояние и события которой мы подписаны. */
    private suspend fun TestScope.setUp(): Setup {
        val dao = FakeWorkoutDao().apply {
            exercises += ExerciseEntity(id = benchPressId, name = "Жим штанги лёжа", category = "Chest")
        }
        val clock = { testScheduler.currentTime }
        val repository = RoomWorkoutRepository(dao, ImmediateTransactionRunner(), now = clock)
        val workoutId = repository.startWorkout(userId, "Грудь + трицепс")
        val viewModel = WorkoutViewModel(workoutId, repository, clock)
        val events = mutableListOf<WorkoutEvent>()

        // Подписка, как у экрана: без неё stateIn(WhileSubscribed) не запустится
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        runCurrent()
        return Setup(repository, viewModel, workoutId, events)
    }

    @Test
    fun `завершение тренировки меняет состояние и закрывает экран`() = runTest {
        val s = setUp()
        assertTrue(s.viewModel.uiState.value.isActive)

        advanceTimeBy(58 * 60_000L)
        runCurrent()
        assertEquals(58L, s.viewModel.uiState.value.elapsedMinutes)

        s.viewModel.finish(note = "Последний подход тяжело")
        runCurrent()

        val state = s.viewModel.uiState.value
        assertFalse(state.isActive)
        assertEquals(58L, state.elapsedMinutes)
        assertEquals("Последний подход тяжело", state.details?.workout?.note)
        assertEquals(listOf<WorkoutEvent>(WorkoutEvent.Finished), s.events)
        assertNull(s.repository.getActiveWorkout(userId))
    }

    @Test
    fun `выбранное в каталоге упражнение добавляется в тренировку`() = runTest {
        val s = setUp()

        s.viewModel.addExercise(benchPressId)
        runCurrent()

        val exercises = s.viewModel.uiState.value.details!!.exercises
        assertEquals("Жим штанги лёжа", exercises.single().exercise.name)
        assertEquals(1, exercises.single().sets.size)
    }

    @Test
    fun `отметка подхода запускает таймер отдыха`() = runTest {
        val s = setUp()
        s.viewModel.addExercise(benchPressId)
        runCurrent()
        val set = s.viewModel.uiState.value.details!!.exercises.single().sets.single()

        s.viewModel.toggleSet(set)
        runCurrent()
        assertEquals(90, s.viewModel.uiState.value.restSecondsLeft)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(60, s.viewModel.uiState.value.restSecondsLeft)

        s.viewModel.addRestTime()
        runCurrent()
        assertEquals(90, s.viewModel.uiState.value.restSecondsLeft)

        s.viewModel.skipRest()
        runCurrent()
        assertNull(s.viewModel.uiState.value.restSecondsLeft)
    }

    @Test
    fun `таймер отдыха исчезает сам, когда время вышло`() = runTest {
        val s = setUp()
        s.viewModel.addExercise(benchPressId)
        runCurrent()
        s.viewModel.toggleSet(s.viewModel.uiState.value.details!!.exercises.single().sets.single())
        runCurrent()

        advanceTimeBy(91_000)
        runCurrent()

        assertNull(s.viewModel.uiState.value.restSecondsLeft)
    }

    @Test
    fun `снятие отметки не запускает таймер`() = runTest {
        val s = setUp()
        s.viewModel.addExercise(benchPressId)
        runCurrent()
        val set = s.viewModel.uiState.value.details!!.exercises.single().sets.single()
        s.viewModel.toggleSet(set)
        runCurrent()
        s.viewModel.skipRest()
        runCurrent()

        val completed = s.viewModel.uiState.value.details!!.exercises.single().sets.single()
        s.viewModel.toggleSet(completed)
        runCurrent()

        assertFalse(s.viewModel.uiState.value.details!!.exercises.single().sets.single().isCompleted)
        assertNull(s.viewModel.uiState.value.restSecondsLeft)
    }

    @Test
    fun `пустое название не сохраняется`() = runTest {
        val s = setUp()

        s.viewModel.rename("   ")
        s.viewModel.rename("  Ноги ")
        runCurrent()

        assertEquals("Ноги", s.viewModel.uiState.value.details?.workout?.title)
    }
}
