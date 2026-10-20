package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity

class RoomWorkoutRepositoryTest {

    private lateinit var dao: FakeWorkoutDao
    private lateinit var repository: RoomWorkoutRepository
    private var now = 1_000_000L

    private val userId = 1L
    private val benchPressId = 101L
    private val squatId = 102L

    @Before
    fun setUp() {
        dao = FakeWorkoutDao()
        dao.exercises += ExerciseEntity(id = benchPressId, name = "Жим штанги лёжа", category = "Chest")
        dao.exercises += ExerciseEntity(id = squatId, name = "Приседания со штангой", category = "Legs")
        repository = RoomWorkoutRepository(dao, ImmediateTransactionRunner(), now = { now })
    }

    // ---------- Активная тренировка ----------

    @Test
    fun `вторая активная тренировка не создаётся`() = runTest {
        val first = repository.startWorkout(userId, "Грудь + трицепс")
        val second = repository.startWorkout(userId, "Ноги")

        assertEquals(first, second)
        assertEquals(1, dao.workouts.size)
        assertEquals("Грудь + трицепс", dao.workouts.single().title)
    }

    @Test
    fun `после завершения можно начать новую тренировку`() = runTest {
        val first = repository.startWorkout(userId, "Грудь")
        now += 60_000
        repository.finishWorkout(first)
        now += 60_000
        val second = repository.startWorkout(userId, "Спина")

        assertNotEquals(first, second)
        assertNull(repository.getActiveWorkout(userId)?.takeIf { it.id == first })
        assertEquals(second, repository.getActiveWorkout(userId)?.id)
    }

    @Test
    fun `у разных пользователей активные тренировки независимы`() = runTest {
        val mine = repository.startWorkout(userId, "Грудь")
        val other = repository.startWorkout(userId + 1, "Ноги")

        assertNotEquals(mine, other)
        assertEquals(2, dao.workouts.size)
    }

    @Test
    fun `завершение проставляет время окончания`() = runTest {
        val id = repository.startWorkout(userId, "Грудь")
        now += 58 * 60_000L
        repository.finishWorkout(id)

        val workout = repository.observeWorkout(id).first()!!.workout
        assertFalse(workout.isActive)
        assertEquals(58L, workout.duration?.toMinutes())
        assertNull(repository.getActiveWorkout(userId))
    }

    // ---------- Упражнения и подходы ----------

    @Test
    fun `новое упражнение встаёт в конец и получает один пустой подход`() = runTest {
        val workoutId = repository.startWorkout(userId, "Тренировка")
        val first = repository.addExercise(workoutId, benchPressId)
        val second = repository.addExercise(workoutId, squatId)

        val details = repository.observeWorkout(workoutId).first()!!
        assertEquals(listOf(first, second), details.exercises.map { it.id })
        assertEquals(listOf(0, 1), details.exercises.map { it.orderIndex })
        details.exercises.forEach { exercise ->
            val set = exercise.sets.single()
            assertEquals(0.0, set.weightKg, 0.0)
            assertEquals(0, set.reps)
            assertFalse(set.isCompleted)
        }
    }

    @Test
    fun `новый подход копирует вес и повторения предыдущего`() = runTest {
        val workoutId = repository.startWorkout(userId, "Грудь")
        val exerciseId = repository.addExercise(workoutId, benchPressId)
        val firstSetId = dao.sets.single().id
        repository.updateSet(firstSetId, weightKg = 82.5, reps = 6)

        repository.addSet(exerciseId)

        val sets = repository.observeWorkout(workoutId).first()!!.exercises.single().sets
        assertEquals(2, sets.size)
        assertEquals(1, sets[1].orderIndex)
        assertEquals(82.5, sets[1].weightKg, 0.0)
        assertEquals(6, sets[1].reps)
        assertFalse("Новый подход не должен быть отмечен", sets[1].isCompleted)
    }

    @Test
    fun `отрицательные вес и повторения не сохраняются`() = runTest {
        val workoutId = repository.startWorkout(userId, "Грудь")
        repository.addExercise(workoutId, benchPressId)
        val setId = dao.sets.single().id

        repository.updateSet(setId, weightKg = -2.5, reps = -1)

        val set = dao.sets.single()
        assertEquals(0.0, set.weightKg, 0.0)
        assertEquals(0, set.reps)
    }

    @Test
    fun `удаление упражнения удаляет и его подходы`() = runTest {
        val workoutId = repository.startWorkout(userId, "Грудь")
        val exerciseId = repository.addExercise(workoutId, benchPressId)
        repository.addSet(exerciseId)

        repository.removeExercise(exerciseId)

        assertTrue(dao.workoutExercises.isEmpty())
        assertTrue(dao.sets.isEmpty())
    }

    // ---------- Подсчёт выполненного ----------

    @Test
    fun `выполненными считаются только упражнения с отмеченным подходом`() = runTest {
        val workoutId = repository.startWorkout(userId, "Тренировка")
        val bench = repository.addExercise(workoutId, benchPressId)
        repository.addExercise(workoutId, squatId)
        repository.addSet(bench)

        // Отмечаем один из двух подходов жима, приседания не трогаем
        val benchSetId = dao.sets.first { it.workoutExerciseId == bench }.id
        repository.toggleSetCompleted(benchSetId)

        val details = repository.observeWorkout(workoutId).first()!!
        assertEquals(1, details.completedExerciseCount)
        assertEquals(1, details.completedSetCount)
        assertEquals(3, details.totalSetCount)
    }

    @Test
    fun `повторная отметка снимает выполнение`() = runTest {
        val workoutId = repository.startWorkout(userId, "Грудь")
        repository.addExercise(workoutId, benchPressId)
        val setId = dao.sets.single().id

        repository.toggleSetCompleted(setId)
        repository.toggleSetCompleted(setId)

        val details = repository.observeWorkout(workoutId).first()!!
        assertEquals(0, details.completedExerciseCount)
        assertEquals(0, details.completedSetCount)
    }

    // ---------- История ----------

    @Test
    fun `в истории только завершённые тренировки, новые сверху`() = runTest {
        val old = repository.startWorkout(userId, "Старая")
        repository.finishWorkout(old)
        now += 86_400_000L
        val recent = repository.startWorkout(userId, "Новая")
        repository.finishWorkout(recent)
        now += 86_400_000L
        repository.startWorkout(userId, "Идёт сейчас")

        val history = repository.observeFinishedWorkouts(userId).first()
        assertEquals(listOf("Новая", "Старая"), history.map { it.title })
        assertEquals("Новая", repository.observeLastFinishedWorkout(userId).first()?.title)
    }

    @Test
    fun `удалённая тренировка пропадает вместе с упражнениями`() = runTest {
        val workoutId = repository.startWorkout(userId, "Грудь")
        repository.addExercise(workoutId, benchPressId)

        repository.deleteWorkout(workoutId)

        assertNull(repository.observeWorkout(workoutId).first())
        assertTrue(dao.workoutExercises.isEmpty())
        assertTrue(dao.sets.isEmpty())
    }

    @Test
    fun `пустая заметка сохраняется как null, название обрезается`() = runTest {
        val workoutId = repository.startWorkout(userId, "  Грудь  ")
        repository.updateNote(workoutId, "   ")

        val workout = dao.workouts.single()
        assertEquals("Грудь", workout.title)
        assertNull(workout.note)
        assertNotNull(repository.getActiveWorkout(userId))
    }
}
