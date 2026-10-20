package ru.pacani.fitnessdiary.di

import android.content.Context
import ru.pacani.fitnessdiary.data.session.SessionRepository
import ru.pacani.fitnessdiary.data.session.sessionDataStore

/**
 * Ручной DI: все зависимости приложения создаются здесь (раздел 4 context.md).
 *
 * Репозитории добавляются по мере появления:
 * - `authRepository`, `statsRepository` — участник 1;
 * - `workoutRepository`, `exerciseRepository` — участник 2 (БД и сеть).
 *
 * ViewModel получает их через `(this[APPLICATION_KEY] as FitnessApp).container`.
 */
interface AppContainer {
    val sessionRepository: SessionRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    // applicationContext, чтобы не держать ссылку на Activity
    private val appContext: Context = context.applicationContext

    override val sessionRepository by lazy { SessionRepository(appContext.sessionDataStore) }
}
