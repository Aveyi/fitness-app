package ru.pacani.fitnessdiary.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.pacani.fitnessdiary.data.auth.PasswordHasher
import ru.pacani.fitnessdiary.data.session.SessionRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import java.io.File

class RoomAuthRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dao = FakeUserDao()
    private lateinit var session: SessionRepository

    private fun TestScope.repository(): RoomAuthRepository {
        session = SessionRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "session.preferences_pb") },
        )
        return RoomAuthRepository(
            userDao = dao,
            session = session,
            hasher = PasswordHasher(),
            hashingDispatcher = StandardTestDispatcher(testScheduler),
            now = { 1_000L },
        )
    }

    private fun registration(email: String = "Ivan@Mail.ru", password: String = "secret123") = Registration(
        name = "  Иван ",
        email = email,
        password = password,
        heightCm = 180,
        weightKg = 78.5,
        goal = Goal.MASS,
    )

    @Test
    fun register_savesNormalizedUserAndStartsSession() = runTest {
        val repository = repository()

        val result = repository.register(registration(email = "  Ivan@Mail.RU "))

        assertTrue(result is RegisterResult.Success)
        val saved = dao.users.value.single()
        assertEquals("ivan@mail.ru", saved.email)
        assertEquals("Иван", saved.name)
        assertEquals("MASS", saved.goal)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(saved.id, session.currentUserId.first())
    }

    @Test
    fun register_storesHashNotPassword() = runTest {
        val repository = repository()
        repository.register(registration(password = "secret123"))

        val saved = dao.users.value.single()
        assertNotEquals("secret123", saved.passwordHash)
        assertFalse(saved.passwordHash.contains("secret123"))
        assertTrue(PasswordHasher().verify("secret123", saved.salt, saved.passwordHash))
    }

    @Test
    fun register_takenEmail_ignoringCaseAndSpaces() = runTest {
        val repository = repository()
        repository.register(registration(email = "ivan@mail.ru"))

        val result = repository.register(registration(email = " IVAN@mail.ru "))

        assertEquals(RegisterResult.EmailTaken, result)
        assertEquals(1, dao.users.value.size)
    }

    @Test
    fun login_success_withAnyEmailCase() = runTest {
        val repository = repository()
        val userId = (repository.register(registration()) as RegisterResult.Success).userId
        repository.logout()

        val result = repository.login(" IVAN@mail.ru ", "secret123")

        assertEquals(LoginResult.Success(userId), result)
        assertEquals(userId, session.currentUserId.first())
    }

    @Test
    fun login_wrongPasswordAndUnknownEmail_giveSameResult() = runTest {
        val repository = repository()
        repository.register(registration())
        repository.logout()

        assertEquals(LoginResult.InvalidCredentials, repository.login("ivan@mail.ru", "wrong123"))
        assertEquals(LoginResult.InvalidCredentials, repository.login("nobody@mail.ru", "secret123"))
        assertNull(session.currentUserId.first())
    }

    @Test
    fun observeCurrentUser_followsLoginLogoutAndProfileUpdate() = runTest {
        val repository = repository()
        repository.observeCurrentUser().test {
            assertNull(awaitItem())

            repository.register(registration())
            val user = awaitItem()!!
            assertEquals("Иван", user.name)
            assertEquals(Goal.MASS, user.goal)

            repository.updateProfile(heightCm = 181, weightKg = null, goal = Goal.STRENGTH)
            val updated = awaitItem()!!
            assertEquals(181, updated.heightCm)
            assertNull(updated.weightKg)
            assertEquals(Goal.STRENGTH, updated.goal)

            repository.logout()
            assertNull(awaitItem())
        }
    }

    @Test
    fun restoreSession() = runTest {
        val repository = repository()
        assertFalse(repository.restoreSession())

        repository.register(registration())
        assertTrue(repository.restoreSession())

        // Пользователя нет в базе (база пересоздана): сессия очищается
        dao.users.value = emptyList()
        assertFalse(repository.restoreSession())
        assertNull(session.currentUserId.first())
    }
}
