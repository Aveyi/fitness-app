package ru.pacani.fitnessdiary.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.data.local.relation.CompletedSetRow
import ru.pacani.fitnessdiary.data.local.relation.WorkoutStatRow

/**
 * Запросы статистики. Владелец — участник 1.
 * DAO только достаёт строки; суммы и группировку по неделям считает StatsCalculator,
 * чтобы их можно было проверить юнит-тестами.
 */
@Dao
interface StatsDao {

    /** Завершённые тренировки пользователя, начатые не раньше [from] (epoch millis), по возрастанию. */
    @Query(
        """
        SELECT w.startedAt, w.finishedAt,
            (SELECT COUNT(*) FROM workout_exercises we
                WHERE we.workoutId = w.id
                AND EXISTS (SELECT 1 FROM sets s WHERE s.workoutExerciseId = we.id AND s.isCompleted = 1)
            ) AS completedExerciseCount
        FROM workouts w
        WHERE w.userId = :userId AND w.finishedAt IS NOT NULL AND w.startedAt >= :from
        ORDER BY w.startedAt
        """,
    )
    fun observeFinishedWorkoutStats(userId: Long, from: Long): Flow<List<WorkoutStatRow>>

    /**
     * Выполненные подходы с весом больше нуля во всех завершённых тренировках пользователя.
     * Рекорд — максимум по упражнению; его и дату считает StatsCalculator.personalRecords.
     */
    @Query(
        """
        SELECT we.exerciseId, e.name AS exerciseName, s.weightKg, w.startedAt
        FROM sets s
        INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        WHERE w.userId = :userId AND w.finishedAt IS NOT NULL AND s.isCompleted = 1 AND s.weightKg > 0
        """,
    )
    fun observeCompletedWeightedSets(userId: Long): Flow<List<CompletedSetRow>>
}
