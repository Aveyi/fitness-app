package ru.pacani.fitnessdiary.domain.model

data class Exercise(
    val id: Long,
    /** id в wger; null у собственных упражнений. */
    val remoteId: Int?,
    val name: String,
    /** Техника выполнения: строки через \n, пункты списка начинаются с «• ». */
    val description: String?,
    val category: ExerciseCategory,
    /** Оборудование на русском через запятую, например «Штанга, скамья». */
    val equipment: String?,
    val isCustom: Boolean,
)
