package ru.pacani.fitnessdiary.ui.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.LoginResult
import ru.pacani.fitnessdiary.data.repository.RegisterResult
import ru.pacani.fitnessdiary.data.repository.Registration
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User

/** AuthRepository для тестов ViewModel: запоминает вызовы и отдаёт заданный ответ. */
class FakeAuthRepository : AuthRepository {

    var loginResult: LoginResult = LoginResult.Success(1L)
    var registerResult: RegisterResult = RegisterResult.Success(1L)

    /** Если задан, login/register ждут его — так видно состояние загрузки. */
    var gate: CompletableDeferred<Unit>? = null

    val loginCalls = mutableListOf<Pair<String, String>>()
    val registrations = mutableListOf<Registration>()
    var logoutCalls = 0

    override suspend fun login(email: String, password: String): LoginResult {
        loginCalls += email to password
        gate?.await()
        return loginResult
    }

    override suspend fun register(registration: Registration): RegisterResult {
        registrations += registration
        gate?.await()
        return registerResult
    }

    override suspend fun logout() {
        logoutCalls++
    }

    /** Текущий пользователь для экранов, которые его показывают. */
    val currentUser = MutableStateFlow<User?>(null)

    override fun observeCurrentUser(): Flow<User?> = currentUser

    override suspend fun restoreSession(): Boolean = false

    val profileUpdates = mutableListOf<Triple<Int?, Double?, Goal>>()

    override suspend fun updateProfile(heightCm: Int?, weightKg: Double?, goal: Goal) {
        profileUpdates += Triple(heightCm, weightKg, goal)
        currentUser.value = currentUser.value?.copy(heightCm = heightCm, weightKg = weightKg, goal = goal)
    }
}
