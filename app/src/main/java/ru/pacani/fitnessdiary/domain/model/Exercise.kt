package ru.pacani.fitnessdiary.domain.model

data class Exercise(
    val id: Long,
    /** id в wger; null у собственных упражнений. */
    val remoteId: Int?,
    val name: String,
    val description: String?,
    val category: String,
    val equipment: String?,
    val isCustom: Boolean,
)
