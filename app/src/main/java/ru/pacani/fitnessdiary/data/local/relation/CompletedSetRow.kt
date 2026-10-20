package ru.pacani.fitnessdiary.data.local.relation

/** Выполненный подход с весом в завершённой тренировке: сырьё для личных рекордов. */
data class CompletedSetRow(
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val startedAt: Long,
)
