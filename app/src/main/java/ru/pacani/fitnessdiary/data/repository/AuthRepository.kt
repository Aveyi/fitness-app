package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User

/** Данные формы регистрации. Поля уже проверены Validators. */
data class Registration(
    val name: String,
    val email: String,
    val password: String,
    val heightCm: Int?,
    val weightKg: Double?,
    val goal: Goal,
)

sealed interface RegisterResult {
    data class Success(val userId: Long) : RegisterResult
    data object EmailTaken : RegisterResult
}

sealed interface LoginResult {
    data class Success(val userId: Long) : LoginResult

    /** Одинаково для неизвестного email и неверного пароля: не раскрываем, какие email есть в базе. */
    data object InvalidCredentials : LoginResult
}

/** Локальная авторизация (раздел 8 context.md). Успешные вход и регистрация сразу сохраняют сессию. */
interface AuthRepository {

    suspend fun register(registration: Registration): RegisterResult

    suspend fun login(email: String, password: String): LoginResult

    suspend fun logout()

    /** Вошедший пользователь или null. Меняется при входе, выходе и правке профиля. */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Проверка сессии при запуске: true, если пользователь из сессии есть в базе.
     * Если его нет (например, база пересоздана при смене схемы), сессия очищается.
     */
    suspend fun restoreSession(): Boolean

    /** Правка профиля текущего пользователя. */
    suspend fun updateProfile(heightCm: Int?, weightKg: Double?, goal: Goal)
}
