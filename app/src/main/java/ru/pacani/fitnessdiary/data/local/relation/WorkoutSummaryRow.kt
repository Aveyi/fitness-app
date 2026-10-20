package ru.pacani.fitnessdiary.data.local.relation

/**
 * Строка для карточек тренировок (История, Главная): тренировка + посчитанные числа.
 *
 * [exerciseCount] — выполненные упражнения (есть хотя бы один отмеченный подход),
 * так же, как в статистике. [completedSetCount] — отмеченные подходы.
 */
data class WorkoutSummaryRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val note: String?,
    val exerciseCount: Int,
    val completedSetCount: Int,
)

/** Пара «локальный id — id в wger» для upsert каталога. */
data class RemoteIdMapping(
    val id: Long,
    val remoteId: Int,
)
