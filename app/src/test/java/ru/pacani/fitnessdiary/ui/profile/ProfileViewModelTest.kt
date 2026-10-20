package ru.pacani.fitnessdiary.ui.profile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.ui.auth.FakeAuthRepository
import ru.pacani.fitnessdiary.util.MainDispatcherRule
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val auth = FakeAuthRepository().apply {
        currentUser.value = User(1L, "Иван", "ivan@mail.ru", 180, 78.5, Goal.STRENGTH, Instant.EPOCH)
    }
    private val workouts = FinishedWorkoutsRepository()

    private fun TestScope.viewModel() = ProfileViewModel(auth, workouts).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun formFilledFromUser_noChanges() = runTest {
        val vm = viewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("180", state.form.height)
        assertEquals("78,5", state.form.weight)
        assertEquals(Goal.STRENGTH, state.form.goal)
        assertFalse(state.hasChanges)
    }

    @Test
    fun finishedWorkoutCount() = runTest {
        workouts.finished.value = List(3) { workout(it.toLong()) }
        val vm = viewModel()

        assertEquals(3, vm.uiState.value.finishedWorkoutCount)
    }

    @Test
    fun sameValueDifferentText_isNotAChange() = runTest {
        val vm = viewModel()
        vm.onWeightChange("78.5")

        assertFalse(vm.uiState.value.hasChanges)
    }

    @Test
    fun save_updatesProfile_andShowsMessage() = runTest {
        val vm = viewModel()
        vm.onHeightChange("181")
        vm.onWeightChange("")
        vm.onGoalChange(Goal.MASS)
        assertTrue(vm.uiState.value.hasChanges)

        vm.onSave()

        assertEquals(listOf(Triple(181, null, Goal.MASS)), auth.profileUpdates)
        assertTrue(vm.uiState.value.showSavedMessage)
        // Профиль обновился, форма совпадает с ним
        assertFalse(vm.uiState.value.hasChanges)

        vm.onSavedMessageShown()
        assertFalse(vm.uiState.value.showSavedMessage)
    }

    @Test
    fun save_invalidValues_showsErrors() = runTest {
        val vm = viewModel()
        vm.onHeightChange("99")
        vm.onWeightChange("abc")

        vm.onSave()

        assertEquals(R.string.error_height_range, vm.uiState.value.form.heightError)
        assertEquals(R.string.error_not_a_number, vm.uiState.value.form.weightError)
        assertTrue(auth.profileUpdates.isEmpty())

        vm.onHeightChange("180")
        assertNull(vm.uiState.value.form.heightError)
    }

    @Test
    fun logout() = runTest {
        val vm = viewModel()
        vm.onLogout()

        assertEquals(1, auth.logoutCalls)
        assertTrue(vm.uiState.value.isLoggedOut)
    }

    private fun workout(id: Long) = Workout(id, 1L, "Тренировка", Instant.EPOCH, Instant.EPOCH, null)
}

/** Профилю нужен только список завершённых тренировок. */
private class FinishedWorkoutsRepository : WorkoutRepository {
    val finished = MutableStateFlow<List<Workout>>(emptyList())

    override fun observeFinishedWorkouts(userId: Long): Flow<List<Workout>> = finished

    override fun observeLastFinishedWorkout(userId: Long): Flow<Workout?> = error("not used")
    override fun observeActiveWorkout(userId: Long): Flow<Workout?> = error("not used")
    override fun observeWorkout(workoutId: Long): Flow<WorkoutDetails?> = error("not used")
    override suspend fun getActiveWorkout(userId: Long): Workout? = error("not used")
    override suspend fun startWorkout(userId: Long, title: String): Long = error("not used")
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
}
