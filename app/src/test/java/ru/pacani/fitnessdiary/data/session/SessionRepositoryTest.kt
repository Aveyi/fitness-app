package ru.pacani.fitnessdiary.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SessionRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository() = SessionRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "session.preferences_pb")
        },
    )

    @Test
    fun noSession_byDefault() = runTest {
        repository().currentUserId.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun setCurrentUser_thenClear() = runTest {
        val repository = repository()
        repository.currentUserId.test {
            assertNull(awaitItem())

            repository.setCurrentUser(42L)
            assertEquals(42L, awaitItem())

            repository.setCurrentUser(7L)
            assertEquals(7L, awaitItem())

            repository.clear()
            assertNull(awaitItem())
        }
    }
}
