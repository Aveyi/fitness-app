package ru.pacani.fitnessdiary.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutExerciseEntity

/**
 * Упражнение в тренировке вместе с карточкой из справочника и подходами.
 * @Relation не гарантирует порядок: сортируем по orderIndex после загрузки (см. Mappers.kt).
 */
data class ExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntity>,
)

/** Тренировка целиком: для экранов «Тренировка» и «Детали тренировки». */
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val exercises: List<ExerciseWithSets>,
)
