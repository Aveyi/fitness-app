package ru.pacani.fitnessdiary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Упражнение внутри тренировки. Упражнение из справочника, которое есть
 * в истории, удалить нельзя (RESTRICT).
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    /** Порядок в тренировке, с 0. */
    val orderIndex: Int,
)
