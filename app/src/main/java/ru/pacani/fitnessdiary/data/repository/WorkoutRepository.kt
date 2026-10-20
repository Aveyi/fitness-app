package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails

interface WorkoutRepository {

    /** Завершённые тренировки, новые сверху. Для Истории. */
    fun observeFinishedWorkouts(userId: Long): Flow<List<Workout>>

    /** Последняя завершённая тренировка. Для Главной. */
    fun observeLastFinishedWorkout(userId: Long): Flow<Workout?>

    /** Незавершённая тренировка, если есть. Для кнопки «Продолжить» на Главной. */
    fun observeActiveWorkout(userId: Long): Flow<Workout?>

    /** Тренировка целиком. Для экранов «Тренировка» и «Детали тренировки». */
    fun observeWorkout(workoutId: Long): Flow<WorkoutDetails?>

    suspend fun getActiveWorkout(userId: Long): Workout?

    /** Создаёт тренировку. Если незавершённая уже есть, возвращает её id. */
    suspend fun startWorkout(userId: Long, title: String): Long

    suspend fun finishWorkout(workoutId: Long)

    suspend fun deleteWorkout(workoutId: Long)

    suspend fun updateWorkoutTitle(workoutId: Long, title: String)

    suspend fun updateNote(workoutId: Long, note: String?)

    /** Добавляет упражнение в конец тренировки и сразу один пустой подход. Возвращает id строки. */
    suspend fun addExercise(workoutId: Long, exerciseId: Long): Long

    suspend fun removeExercise(workoutExerciseId: Long)

    /** Добавляет подход; вес и повторения копируются из предыдущего. */
    suspend fun addSet(workoutExerciseId: Long): Long

    suspend fun updateSet(setId: Long, weightKg: Double, reps: Int)

    suspend fun toggleSetCompleted(setId: Long)

    suspend fun deleteSet(setId: Long)

    /** Лучший вес по упражнению до указанной тренировки. Для пометки «Рекорд». */
    suspend fun getBestWeightBefore(userId: Long, exerciseId: Long, beforeWorkoutStartedAt: Long): Double?
}
