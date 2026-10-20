package ru.pacani.fitnessdiary.data.local.relation

/**
 * Завершённая тренировка для статистики: время и число выполненных упражнений
 * (есть хотя бы один отмеченный подход — как в WorkoutSummaryRow).
 */
data class WorkoutStatRow(
    val startedAt: Long,
    val finishedAt: Long,
    val completedExerciseCount: Int,
)
