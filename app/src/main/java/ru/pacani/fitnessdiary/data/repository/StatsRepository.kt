package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pacani.fitnessdiary.data.local.dao.StatsDao
import ru.pacani.fitnessdiary.domain.stats.CompletedSet
import ru.pacani.fitnessdiary.domain.stats.PersonalRecord
import ru.pacani.fitnessdiary.domain.stats.StatsCalculator
import ru.pacani.fitnessdiary.domain.stats.WorkoutStat
import java.time.Duration
import java.time.Instant

/** Данные для Главной и Статистики. Суммы, недели и рекорды считает StatsCalculator. */
interface StatsRepository {

    /** Завершённые тренировки пользователя, начатые не раньше [from], по возрастанию даты. */
    fun observeFinishedWorkouts(userId: Long, from: Instant): Flow<List<WorkoutStat>>

    /** Личные рекорды пользователя за всё время, новые сверху. */
    fun observePersonalRecords(userId: Long): Flow<List<PersonalRecord>>
}

class RoomStatsRepository(private val dao: StatsDao) : StatsRepository {

    override fun observeFinishedWorkouts(userId: Long, from: Instant): Flow<List<WorkoutStat>> =
        dao.observeFinishedWorkoutStats(userId, from.toEpochMilli()).map { rows ->
            rows.map { row ->
                WorkoutStat(
                    startedAt = Instant.ofEpochMilli(row.startedAt),
                    // Часы на телефоне могли перевести назад: отрицательное время не считаем
                    duration = Duration.ofMillis((row.finishedAt - row.startedAt).coerceAtLeast(0)),
                    completedExerciseCount = row.completedExerciseCount,
                )
            }
        }

    override fun observePersonalRecords(userId: Long): Flow<List<PersonalRecord>> =
        dao.observeCompletedWeightedSets(userId).map { rows ->
            StatsCalculator.personalRecords(
                rows.map { CompletedSet(it.exerciseId, it.exerciseName, it.weightKg, Instant.ofEpochMilli(it.startedAt)) },
            )
        }
}
