package ru.pacani.fitnessdiary.ui.exercises

import androidx.annotation.StringRes
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

/** Подпись категории на русском. */
@StringRes
fun ExerciseCategory.labelRes(): Int = when (this) {
    ExerciseCategory.CHEST -> R.string.category_chest
    ExerciseCategory.BACK -> R.string.category_back
    ExerciseCategory.LEGS -> R.string.category_legs
    ExerciseCategory.SHOULDERS -> R.string.category_shoulders
    ExerciseCategory.ARMS -> R.string.category_arms
    ExerciseCategory.ABS -> R.string.category_abs
    ExerciseCategory.CARDIO -> R.string.category_cardio
    ExerciseCategory.OTHER -> R.string.category_other
}
