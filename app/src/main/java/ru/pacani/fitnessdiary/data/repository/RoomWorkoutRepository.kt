package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pacani.fitnessdiary.data.local.TransactionRunner
import ru.pacani.fitnessdiary.data.local.dao.WorkoutDao
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutExerciseEntity
import ru.pacani.fitnessdiary.data.local.toDomain
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val transaction: TransactionRunner,
    /** Источник времени; в тестах подменяется. */
    private val now: () -> Long = System::currentTimeMillis,
) : WorkoutRepository {

    override fun observeFinishedWorkouts(userId: Long): Flow<List<Workout>> =
        dao.observeFinishedWorkoutSummaries(userId).map { rows -> rows.map { it.toDomain() } }

    override fun observeLastFinishedWorkout(userId: Long): Flow<Workout?> =
        dao.observeLastFinishedWorkoutSummary(userId).map { it?.toDomain() }

    override fun observeActiveWorkout(userId: Long): Flow<Workout?> =
        dao.observeActiveWorkout(userId).map { it?.toDomain() }

    override fun observeWorkout(workoutId: Long): Flow<WorkoutDetails?> =
        dao.observeWorkoutWithExercises(workoutId).map { it?.toDomain() }

    override suspend fun getActiveWorkout(userId: Long): Workout? =
        dao.getActiveWorkout(userId)?.toDomain()

    override suspend fun startWorkout(userId: Long, title: String): Long = transaction.inTransaction {
        dao.getActiveWorkout(userId)?.id
            ?: dao.insertWorkout(WorkoutEntity(userId = userId, title = title.trim(), startedAt = now()))
    }

    override suspend fun finishWorkout(workoutId: Long) {
        dao.setFinishedAt(workoutId, now())
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        dao.deleteWorkout(workoutId)
    }

    override suspend fun updateWorkoutTitle(workoutId: Long, title: String) {
        dao.updateTitle(workoutId, title.trim())
    }

    override suspend fun updateNote(workoutId: Long, note: String?) {
        dao.updateNote(workoutId, note?.trim()?.takeIf { it.isNotEmpty() })
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: Long): Long = transaction.inTransaction {
        val orderIndex = dao.getMaxExerciseOrder(workoutId) + 1
        val workoutExerciseId = dao.insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = orderIndex),
        )
        dao.insertSet(SetEntity(workoutExerciseId = workoutExerciseId, orderIndex = 0, weightKg = 0.0, reps = 0))
        workoutExerciseId
    }

    override suspend fun removeExercise(workoutExerciseId: Long) {
        dao.deleteWorkoutExercise(workoutExerciseId)
    }

    override suspend fun addSet(workoutExerciseId: Long): Long = transaction.inTransaction {
        val last = dao.getLastSet(workoutExerciseId)
        dao.insertSet(
            SetEntity(
                workoutExerciseId = workoutExerciseId,
                orderIndex = (last?.orderIndex ?: -1) + 1,
                weightKg = last?.weightKg ?: 0.0,
                reps = last?.reps ?: 0,
            ),
        )
    }

    override suspend fun updateSet(setId: Long, weightKg: Double, reps: Int) {
        dao.updateSet(setId, weightKg.coerceAtLeast(0.0), reps.coerceAtLeast(0))
    }

    override suspend fun toggleSetCompleted(setId: Long) {
        dao.toggleSetCompleted(setId)
    }

    override suspend fun deleteSet(setId: Long) {
        dao.deleteSet(setId)
    }

    override suspend fun getBestWeightBefore(userId: Long, exerciseId: Long, beforeWorkoutStartedAt: Long): Double? =
        dao.getBestWeightBefore(userId, exerciseId, beforeWorkoutStartedAt)
}
