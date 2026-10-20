package ru.pacani.fitnessdiary.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Тренировка для списков и карточек (История, Главная).
 *
 * [exerciseCount] — выполненные упражнения (хотя бы один отмеченный подход),
 * [completedSetCount] — отмеченные подходы.
 */
data class Workout(
    val id: Long,
    val userId: Long,
    val title: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val note: String?,
    val exerciseCount: Int = 0,
    val completedSetCount: Int = 0,
) {
    val isActive: Boolean get() = finishedAt == null

    /** null, пока тренировка идёт. */
    val duration: Duration? get() = finishedAt?.let { Duration.between(startedAt, it) }
}

/** Тренировка целиком: для экранов «Тренировка» и «Детали тренировки». */
data class WorkoutDetails(
    val workout: Workout,
    /** Отсортированы по порядку в тренировке. */
    val exercises: List<WorkoutExercise>,
) {
    val totalSetCount: Int get() = exercises.sumOf { it.sets.size }
    val completedSetCount: Int get() = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val completedExerciseCount: Int get() = exercises.count { it.isCompleted }
}

data class WorkoutExercise(
    /** id строки workout_exercises (не id упражнения из справочника). */
    val id: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    /** Отсортированы по номеру подхода. */
    val sets: List<WorkoutSet>,
) {
    /** Упражнение выполнено, если отмечен хотя бы один подход. */
    val isCompleted: Boolean get() = sets.any { it.isCompleted }
}

data class WorkoutSet(
    val id: Long,
    val orderIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean,
)
