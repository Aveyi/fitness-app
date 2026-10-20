package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.pacani.fitnessdiary.data.local.TransactionRunner
import ru.pacani.fitnessdiary.data.local.dao.WorkoutDao
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutExerciseEntity
import ru.pacani.fitnessdiary.data.local.relation.ExerciseWithSets
import ru.pacani.fitnessdiary.data.local.relation.WorkoutSummaryRow
import ru.pacani.fitnessdiary.data.local.relation.WorkoutWithExercises

/**
 * WorkoutDao в памяти: данные в обычных списках, без Room и Android.
 * Каскадное удаление повторяет внешние ключи из схемы.
 */
class FakeWorkoutDao : WorkoutDao {

    val workouts = mutableListOf<WorkoutEntity>()
    val workoutExercises = mutableListOf<WorkoutExerciseEntity>()
    val sets = mutableListOf<SetEntity>()

    /** Справочник упражнений: в реальной базе он в другой таблице, здесь нужен для @Relation. */
    val exercises = mutableListOf<ExerciseEntity>()

    private var nextId = 1L
    private val changes = MutableStateFlow(0)

    private fun changed() {
        changes.value++
    }

    // ---------- Тренировки ----------

    override suspend fun insertWorkout(workout: WorkoutEntity): Long {
        val id = nextId++
        workouts += workout.copy(id = id)
        changed()
        return id
    }

    private fun activeWorkout(userId: Long): WorkoutEntity? =
        workouts.filter { it.userId == userId && it.finishedAt == null }.maxByOrNull { it.startedAt }

    override suspend fun getActiveWorkout(userId: Long): WorkoutEntity? = activeWorkout(userId)

    override fun observeActiveWorkout(userId: Long): Flow<WorkoutEntity?> =
        changes.map { activeWorkout(userId) }

    override suspend fun setFinishedAt(workoutId: Long, finishedAt: Long) {
        workouts.update(workoutId) { it.copy(finishedAt = finishedAt) }
    }

    override suspend fun updateTitle(workoutId: Long, title: String) {
        workouts.update(workoutId) { it.copy(title = title) }
    }

    override suspend fun updateNote(workoutId: Long, note: String?) {
        workouts.update(workoutId) { it.copy(note = note) }
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        workoutExercises.filter { it.workoutId == workoutId }.forEach { removeWorkoutExercise(it.id) }
        workouts.removeAll { it.id == workoutId }
        changed()
    }

    override fun observeWorkoutWithExercises(workoutId: Long): Flow<WorkoutWithExercises?> =
        changes.map {
            val workout = workouts.find { it.id == workoutId } ?: return@map null
            WorkoutWithExercises(
                workout = workout,
                exercises = workoutExercises.filter { it.workoutId == workoutId }.map { we ->
                    ExerciseWithSets(
                        workoutExercise = we,
                        exercise = exercises.first { it.id == we.exerciseId },
                        sets = sets.filter { it.workoutExerciseId == we.id },
                    )
                },
            )
        }

    private fun summaries(userId: Long): List<WorkoutSummaryRow> =
        workouts
            .filter { it.userId == userId && it.finishedAt != null }
            .sortedByDescending { it.startedAt }
            .map { w ->
                val exerciseIds = workoutExercises.filter { it.workoutId == w.id }.map { it.id }
                val completedSets = sets.filter { it.workoutExerciseId in exerciseIds && it.isCompleted }
                WorkoutSummaryRow(
                    id = w.id,
                    userId = w.userId,
                    title = w.title,
                    startedAt = w.startedAt,
                    finishedAt = w.finishedAt,
                    note = w.note,
                    exerciseCount = completedSets.map { it.workoutExerciseId }.distinct().size,
                    completedSetCount = completedSets.size,
                )
            }

    override fun observeFinishedWorkoutSummaries(userId: Long): Flow<List<WorkoutSummaryRow>> =
        changes.map { summaries(userId) }

    override fun observeLastFinishedWorkoutSummary(userId: Long): Flow<WorkoutSummaryRow?> =
        changes.map { summaries(userId).firstOrNull() }

    // ---------- Упражнения в тренировке ----------

    override suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long {
        val id = nextId++
        workoutExercises += workoutExercise.copy(id = id)
        changed()
        return id
    }

    override suspend fun getMaxExerciseOrder(workoutId: Long): Int =
        workoutExercises.filter { it.workoutId == workoutId }.maxOfOrNull { it.orderIndex } ?: -1

    override suspend fun deleteWorkoutExercise(workoutExerciseId: Long) {
        removeWorkoutExercise(workoutExerciseId)
        changed()
    }

    private fun removeWorkoutExercise(workoutExerciseId: Long) {
        sets.removeAll { it.workoutExerciseId == workoutExerciseId }
        workoutExercises.removeAll { it.id == workoutExerciseId }
    }

    // ---------- Подходы ----------

    override suspend fun insertSet(set: SetEntity): Long {
        val id = nextId++
        sets += set.copy(id = id)
        changed()
        return id
    }

    override suspend fun getLastSet(workoutExerciseId: Long): SetEntity? =
        sets.filter { it.workoutExerciseId == workoutExerciseId }.maxByOrNull { it.orderIndex }

    override suspend fun updateSet(setId: Long, weightKg: Double, reps: Int) {
        sets.update(setId) { it.copy(weightKg = weightKg, reps = reps) }
    }

    override suspend fun toggleSetCompleted(setId: Long) {
        sets.update(setId) { it.copy(isCompleted = !it.isCompleted) }
    }

    override suspend fun deleteSet(setId: Long) {
        sets.removeAll { it.id == setId }
        changed()
    }

    // ---------- Рекорды ----------

    override suspend fun getBestWeightBefore(userId: Long, exerciseId: Long, before: Long): Double? {
        val workoutIds = workouts
            .filter { it.userId == userId && it.finishedAt != null && it.startedAt < before }
            .map { it.id }
        val weIds = workoutExercises
            .filter { it.workoutId in workoutIds && it.exerciseId == exerciseId }
            .map { it.id }
        return sets.filter { it.workoutExerciseId in weIds && it.isCompleted }.maxOfOrNull { it.weightKg }
    }

    // ---------- Вспомогательное ----------

    private fun <T> MutableList<T>.update(id: Long, transform: (T) -> T) {
        val index = indexOfFirst { item ->
            when (item) {
                is WorkoutEntity -> item.id == id
                is SetEntity -> item.id == id
                is WorkoutExerciseEntity -> item.id == id
                else -> false
            }
        }
        if (index >= 0) this[index] = transform(this[index])
        changed()
    }
}

/** В тестах транзакция не нужна: просто выполняем блок. */
class ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <R> inTransaction(block: suspend () -> R): R = block()
}
