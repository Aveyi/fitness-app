package ru.pacani.fitnessdiary.data.repository

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.pacani.fitnessdiary.data.auth.PasswordHasher
import ru.pacani.fitnessdiary.data.local.dao.UserDao
import ru.pacani.fitnessdiary.data.local.entity.UserEntity
import ru.pacani.fitnessdiary.data.session.SessionRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import java.time.Instant

class RoomAuthRepository(
    private val userDao: UserDao,
    private val session: SessionRepository,
    private val hasher: PasswordHasher,
    /** PBKDF2 намеренно медленный: считаем не в главном потоке. В тестах подменяется. */
    private val hashingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Источник времени; в тестах подменяется. */
    private val now: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    override suspend fun register(registration: Registration): RegisterResult {
        val email = normalizeEmail(registration.email)
        if (userDao.findByEmail(email) != null) return RegisterResult.EmailTaken

        val (salt, hash) = withContext(hashingDispatcher) {
            val salt = hasher.newSalt()
            salt to hasher.hash(registration.password, salt)
        }
        val user = UserEntity(
            name = registration.name.trim(),
            email = email,
            passwordHash = hash,
            salt = salt,
            heightCm = registration.heightCm,
            weightKg = registration.weightKg,
            goal = registration.goal.name,
            createdAt = now(),
        )
        val userId = try {
            userDao.insert(user)
        } catch (e: SQLiteConstraintException) {
            // Тот же email успели зарегистрировать между проверкой и вставкой
            return RegisterResult.EmailTaken
        }
        session.setCurrentUser(userId)
        return RegisterResult.Success(userId)
    }

    override suspend fun login(email: String, password: String): LoginResult {
        val user = userDao.findByEmail(normalizeEmail(email))
        val passwordMatches = withContext(hashingDispatcher) {
            if (user == null) {
                // Считаем хеш и для несуществующего email, чтобы по времени ответа
                // нельзя было понять, зарегистрирован ли он
                hasher.hash(password, DUMMY_SALT)
                false
            } else {
                hasher.verify(password, user.salt, user.passwordHash)
            }
        }
        if (user == null || !passwordMatches) return LoginResult.InvalidCredentials

        session.setCurrentUser(user.id)
        return LoginResult.Success(user.id)
    }

    override suspend fun logout() {
        session.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> =
        session.currentUserId.flatMapLatest { userId ->
            if (userId == null) flowOf(null) else userDao.observeById(userId).map { it?.toDomain() }
        }

    override suspend fun restoreSession(): Boolean {
        val userId = session.currentUserId.first() ?: return false
        if (userDao.getById(userId) != null) return true
        session.clear()
        return false
    }

    override suspend fun updateProfile(heightCm: Int?, weightKg: Double?, goal: Goal) {
        val userId = session.currentUserId.first() ?: return
        userDao.updateProfile(userId, heightCm, weightKg, goal.name)
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    private fun UserEntity.toDomain() = User(
        id = id,
        name = name,
        email = email,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = Goal.entries.firstOrNull { it.name == goal } ?: Goal.MAINTENANCE,
        createdAt = Instant.ofEpochMilli(createdAt),
    )

    private companion object {
        /** 16 нулевых байт в Base64: соль для «холостого» хеша при неизвестном email. */
        const val DUMMY_SALT = "AAAAAAAAAAAAAAAAAAAAAA=="
    }
}
