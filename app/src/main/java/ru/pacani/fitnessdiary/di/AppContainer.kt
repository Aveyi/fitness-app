package ru.pacani.fitnessdiary.di

import android.content.Context
import ru.pacani.fitnessdiary.data.local.AppDatabase
import ru.pacani.fitnessdiary.data.local.RoomTransactionRunner
import ru.pacani.fitnessdiary.data.repository.RoomWorkoutRepository
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository

/**
 * Ручной DI: все зависимости приложения создаются здесь (раздел 4 context.md).
 *
 * Репозитории добавляются по мере появления:
 * - `sessionRepository`, `authRepository`, `statsRepository` — участник 1;
 * - `workoutRepository`, `exerciseRepository` — участник 2 (БД и сеть).
 *
 * ViewModel получает их через `(this[APPLICATION_KEY] as FitnessApp).container`.
 */
interface AppContainer {
    val workoutRepository: WorkoutRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    // applicationContext, чтобы не держать ссылку на Activity
    private val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    override val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(database.workoutDao(), RoomTransactionRunner(database))
    }
}
