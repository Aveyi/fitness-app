package ru.pacani.fitnessdiary.ui.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.LoginResult
import ru.pacani.fitnessdiary.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(repository)

    @Test
    fun emptyForm_showsFieldErrors_andDoesNotCallRepository() {
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_required, state.emailError)
        assertEquals(R.string.error_required, state.passwordError)
        assertTrue(repository.loginCalls.isEmpty())
    }

    @Test
    fun invalidEmail_showsError() {
        viewModel.onEmailChange("ivan@")
        viewModel.onPasswordChange("x")
        viewModel.onSubmit()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailError)
        // Правила нового пароля при входе не проверяются
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun editingField_clearsItsError() {
        viewModel.onSubmit()
        viewModel.onEmailChange("i")

        assertNull(viewModel.uiState.value.emailError)
        assertEquals(R.string.error_required, viewModel.uiState.value.passwordError)
    }

    @Test
    fun success_setsLoggedIn() {
        fillValidForm()
        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals(listOf("ivan@mail.ru" to "secret123"), repository.loginCalls)
    }

    @Test
    fun invalidCredentials_showsFormError_untilEdited() {
        repository.loginResult = LoginResult.InvalidCredentials
        fillValidForm()
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_invalid_credentials, state.formError)
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)

        viewModel.onPasswordChange("secret1234")
        assertNull(viewModel.uiState.value.formError)
    }

    @Test
    fun whileLoading_secondSubmitIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        fillValidForm()

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.onSubmit()
        assertEquals(1, repository.loginCalls.size)

        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isLoggedIn)
    }

    @Test
    fun togglePasswordVisibility() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }

    private fun fillValidForm() {
        viewModel.onEmailChange("ivan@mail.ru")
        viewModel.onPasswordChange("secret123")
    }
}
