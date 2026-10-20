package ru.pacani.fitnessdiary.data.local

import java.time.Instant
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.relation.ExerciseWithSets
import ru.pacani.fitnessdiary.data.local.relation.WorkoutSummaryRow
import ru.pacani.fitnessdiary.data.local.relation.WorkoutWithExercises
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.model.WorkoutExercise
import ru.pacani.fitnessdiary.domain.model.WorkoutSet

fun WorkoutEntity.toDomain(exerciseCount: Int = 0, completedSetCount: Int = 0) = Workout(
    id = id,
    userId = userId,
    title = title,
    startedAt = Instant.ofEpochMilli(startedAt),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    note = note,
    exerciseCount = exerciseCount,
    completedSetCount = completedSetCount,
)

fun WorkoutSummaryRow.toDomain() = Workout(
    id = id,
    userId = userId,
    title = title,
    startedAt = Instant.ofEpochMilli(startedAt),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    note = note,
    exerciseCount = exerciseCount,
    completedSetCount = completedSetCount,
)

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    remoteId = remoteId,
    name = name,
    description = description,
    category = ExerciseCategory.fromKey(category),
    equipment = equipment,
    isCustom = isCustom,
)

fun SetEntity.toDomain() = WorkoutSet(
    id = id,
    orderIndex = orderIndex,
    weightKg = weightKg,
    reps = reps,
    isCompleted = isCompleted,
)

fun ExerciseWithSets.toDomain() = WorkoutExercise(
    id = workoutExercise.id,
    exercise = exercise.toDomain(),
    orderIndex = workoutExercise.orderIndex,
    sets = sets.sortedBy { it.orderIndex }.map { it.toDomain() },
)

/** @Relation не гарантирует порядок, поэтому сортируем здесь. */
fun WorkoutWithExercises.toDomain(): WorkoutDetails {
    val sortedExercises = exercises.sortedBy { it.workoutExercise.orderIndex }.map { it.toDomain() }
    return WorkoutDetails(
        workout = workout.toDomain(
            exerciseCount = sortedExercises.count { it.isCompleted },
            completedSetCount = sortedExercises.sumOf { ex -> ex.sets.count { it.isCompleted } },
        ),
        exercises = sortedExercises,
    )
}
