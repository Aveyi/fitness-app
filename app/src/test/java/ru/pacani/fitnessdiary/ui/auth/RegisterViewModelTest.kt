package ru.pacani.fitnessdiary.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.RegisterResult
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.validation.PasswordStrength

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = RegisterViewModel(repository)

    @Test
    fun noErrorsShown_beforeFirstSubmit() {
        viewModel.onEmailChange("iv")
        viewModel.onPasswordChange("1")

        assertFalse(viewModel.uiState.value.hasErrors)
    }

    @Test
    fun submitEmptyForm_showsRequiredErrors() {
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_required, state.nameError)
        assertEquals(R.string.error_required, state.emailError)
        assertEquals(R.string.error_required, state.passwordError)
        assertEquals(R.string.error_required, state.passwordRepeatError)
        // Рост и вес необязательны
        assertNull(state.heightError)
        assertNull(state.weightError)
        assertTrue(repository.registrations.isEmpty())
    }

    @Test
    fun afterSubmit_errorsUpdateWhileTyping() {
        viewModel.onSubmit()

        viewModel.onPasswordChange("abcdefg1")
        viewModel.onPasswordRepeatChange("abcdefg2")
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(R.string.error_passwords_do_not_match, viewModel.uiState.value.passwordRepeatError)

        viewModel.onPasswordRepeatChange("abcdefg1")
        assertNull(viewModel.uiState.value.passwordRepeatError)
    }

    @Test
    fun outOfRangeHeightAndWeight() {
        fillValidForm()
        viewModel.onHeightChange("99")
        viewModel.onWeightChange("abc")
        viewModel.onSubmit()

        assertEquals(R.string.error_height_range, viewModel.uiState.value.heightError)
        assertEquals(R.string.error_not_a_number, viewModel.uiState.value.weightError)
        assertTrue(repository.registrations.isEmpty())
    }

    @Test
    fun success_passesParsedValues() {
        fillValidForm()
        viewModel.onHeightChange("180")
        viewModel.onWeightChange("78,5")
        viewModel.onGoalChange(Goal.WEIGHT_LOSS)
        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isRegistered)
        val registration = repository.registrations.single()
        assertEquals("Иван", registration.name)
        assertEquals(180, registration.heightCm)
        assertEquals(78.5, registration.weightKg!!, 0.0)
        assertEquals(Goal.WEIGHT_LOSS, registration.goal)
    }

    @Test
    fun emptyHeightAndWeight_areNull() {
        fillValidForm()
        viewModel.onSubmit()

        val registration = repository.registrations.single()
        assertNull(registration.heightCm)
        assertNull(registration.weightKg)
    }

    @Test
    fun emailTaken_errorStaysUntilEmailChanged() {
        repository.registerResult = RegisterResult.EmailTaken
        fillValidForm()
        viewModel.onSubmit()

        assertEquals(R.string.error_email_taken, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isRegistered)

        // Правка другого поля не убирает ошибку: email всё ещё занят
        viewModel.onNameChange("Иван Петров")
        assertEquals(R.string.error_email_taken, viewModel.uiState.value.emailError)

        viewModel.onEmailChange("ivan2@mail.ru")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun passwordStrength_followsPassword() {
        assertEquals(PasswordStrength.NONE, viewModel.uiState.value.passwordStrength)
        viewModel.onPasswordChange("abcdefg1")
        assertEquals(PasswordStrength.MEDIUM, viewModel.uiState.value.passwordStrength)
    }

    private fun fillValidForm() {
        viewModel.onNameChange("Иван")
        viewModel.onEmailChange("ivan@mail.ru")
        viewModel.onPasswordChange("secret123")
        viewModel.onPasswordRepeatChange("secret123")
    }
}
