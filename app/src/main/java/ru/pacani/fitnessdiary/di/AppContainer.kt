package ru.pacani.fitnessdiary.di

import android.content.Context
import ru.pacani.fitnessdiary.data.local.AppDatabase
import ru.pacani.fitnessdiary.data.local.RoomTransactionRunner
import ru.pacani.fitnessdiary.data.repository.RoomWorkoutRepository
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.data.session.SessionRepository
import ru.pacani.fitnessdiary.data.session.sessionDataStore

/**
 * Ручной DI: все зависимости приложения создаются здесь (раздел 4 context.md).
 *
 * Репозитории добавляются по мере появления:
 * - `authRepository`, `statsRepository` — участник 1;
 * - `exerciseRepository` — участник 2 (сеть).
 *
 * ViewModel получает их через `(this[APPLICATION_KEY] as FitnessApp).container`.
 */
interface AppContainer {
    val sessionRepository: SessionRepository
    val workoutRepository: WorkoutRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    // applicationContext, чтобы не держать ссылку на Activity
    private val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    override val sessionRepository by lazy { SessionRepository(appContext.sessionDataStore) }

    override val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(database.workoutDao(), RoomTransactionRunner(database))
    }
}
