package ru.pacani.fitnessdiary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutExerciseEntity
import ru.pacani.fitnessdiary.data.local.relation.WorkoutSummaryRow
import ru.pacani.fitnessdiary.data.local.relation.WorkoutWithExercises

/**
 * Только простые операции. Логика из нескольких шагов (не больше одной активной
 * тренировки, копирование подхода и т.п.) — в WorkoutRepository внутри транзакции.
 * Так репозиторий можно тестировать с фейковым DAO.
 */
@Dao
interface WorkoutDao {

    // ---------- Тренировки ----------

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkout(userId: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(userId: Long): Flow<WorkoutEntity?>

    @Query("UPDATE workouts SET finishedAt = :finishedAt WHERE id = :workoutId")
    suspend fun setFinishedAt(workoutId: Long, finishedAt: Long)

    @Query("UPDATE workouts SET title = :title WHERE id = :workoutId")
    suspend fun updateTitle(workoutId: Long, title: String)

    @Query("UPDATE workouts SET note = :note WHERE id = :workoutId")
    suspend fun updateNote(workoutId: Long, note: String?)

    /** Упражнения и подходы удалит каскад. */
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun observeWorkoutWithExercises(workoutId: Long): Flow<WorkoutWithExercises?>

    @Query(
        """
        SELECT w.id, w.userId, w.title, w.startedAt, w.finishedAt, w.note,
            (SELECT COUNT(*) FROM workout_exercises we
                WHERE we.workoutId = w.id
                AND EXISTS (SELECT 1 FROM sets s WHERE s.workoutExerciseId = we.id AND s.isCompleted = 1)
            ) AS exerciseCount,
            (SELECT COUNT(*) FROM sets s
                INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
                WHERE we.workoutId = w.id AND s.isCompleted = 1
            ) AS completedSetCount
        FROM workouts w
        WHERE w.userId = :userId AND w.finishedAt IS NOT NULL
        ORDER BY w.startedAt DESC
        """,
    )
    fun observeFinishedWorkoutSummaries(userId: Long): Flow<List<WorkoutSummaryRow>>

    @Query(
        """
        SELECT w.id, w.userId, w.title, w.startedAt, w.finishedAt, w.note,
            (SELECT COUNT(*) FROM workout_exercises we
                WHERE we.workoutId = w.id
                AND EXISTS (SELECT 1 FROM sets s WHERE s.workoutExerciseId = we.id AND s.isCompleted = 1)
            ) AS exerciseCount,
            (SELECT COUNT(*) FROM sets s
                INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
                WHERE we.workoutId = w.id AND s.isCompleted = 1
            ) AS completedSetCount
        FROM workouts w
        WHERE w.userId = :userId AND w.finishedAt IS NOT NULL
        ORDER BY w.startedAt DESC
        LIMIT 1
        """,
    )
    fun observeLastFinishedWorkoutSummary(userId: Long): Flow<WorkoutSummaryRow?>

    // ---------- Упражнения в тренировке ----------

    @Insert
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    /** -1, если упражнений ещё нет. */
    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getMaxExerciseOrder(workoutId: Long): Int

    /** Подходы удалит каскад. */
    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteWorkoutExercise(workoutExerciseId: Long)

    // ---------- Подходы ----------

    @Insert
    suspend fun insertSet(set: SetEntity): Long

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY orderIndex DESC LIMIT 1")
    suspend fun getLastSet(workoutExerciseId: Long): SetEntity?

    @Query("UPDATE sets SET weightKg = :weightKg, reps = :reps WHERE id = :setId")
    suspend fun updateSet(setId: Long, weightKg: Double, reps: Int)

    @Query("UPDATE sets SET isCompleted = NOT isCompleted WHERE id = :setId")
    suspend fun toggleSetCompleted(setId: Long)

    @Query("DELETE FROM sets WHERE id = :setId")
    suspend fun deleteSet(setId: Long)

    // ---------- Рекорды ----------

    /**
     * Лучший вес по упражнению среди выполненных подходов в завершённых тренировках,
     * начатых раньше [before]. Нужен для пометки «Рекорд» в деталях тренировки:
     * подход — рекорд, если его вес больше этого значения (или значения нет).
     */
    @Query(
        """
        SELECT MAX(s.weightKg) FROM sets s
        INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE w.userId = :userId AND we.exerciseId = :exerciseId
            AND s.isCompleted = 1 AND w.finishedAt IS NOT NULL AND w.startedAt < :before
        """,
    )
    suspend fun getBestWeightBefore(userId: Long, exerciseId: Long, before: Long): Double?
}
