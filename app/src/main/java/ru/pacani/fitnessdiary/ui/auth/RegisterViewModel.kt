package ru.pacani.fitnessdiary.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.RegisterResult
import ru.pacani.fitnessdiary.data.repository.Registration
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.validation.PasswordStrength
import ru.pacani.fitnessdiary.domain.validation.Validators
import ru.pacani.fitnessdiary.domain.validation.evaluatePasswordStrength

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val height: String = "",
    val weight: String = "",
    val goal: Goal = Goal.STRENGTH,
    val isPasswordVisible: Boolean = false,
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val passwordRepeatError: Int? = null,
    @param:StringRes val heightError: Int? = null,
    @param:StringRes val weightError: Int? = null,
    val isLoading: Boolean = false,
    /** Аккаунт создан и сессия сохранена: экран переходит на Главную. */
    val isRegistered: Boolean = false,
) {
    val passwordStrength: PasswordStrength get() = evaluatePasswordStrength(password)

    val hasErrors: Boolean
        get() = listOf(nameError, emailError, passwordError, passwordRepeatError, heightError, weightError)
            .any { it != null }
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * До первой попытки отправки ошибки не показываем, чтобы не ругаться на недописанное поле.
     * После неё проверяем форму при каждом изменении: ошибка исчезает, как только поле исправлено.
     */
    private var showErrors = false

    /** Email, который оказался занят: ошибка держится, пока его не исправят. */
    private var takenEmail: String? = null

    fun onNameChange(value: String) = edit { it.copy(name = value) }
    fun onEmailChange(value: String) = edit { it.copy(email = value) }
    fun onPasswordChange(value: String) = edit { it.copy(password = value) }
    fun onPasswordRepeatChange(value: String) = edit { it.copy(passwordRepeat = value) }
    fun onHeightChange(value: String) = edit { it.copy(height = value) }
    fun onWeightChange(value: String) = edit { it.copy(weight = value) }
    fun onGoalChange(goal: Goal) = edit { it.copy(goal = goal) }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmit() {
        if (_uiState.value.isLoading) return
        showErrors = true
        val state = _uiState.updateAndGet { it.validated() }
        if (state.hasErrors) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val registration = Registration(
                name = state.name,
                email = state.email,
                password = state.password,
                heightCm = state.height.takeIf { it.isNotBlank() }?.let(Validators::parseHeight),
                weightKg = state.weight.takeIf { it.isNotBlank() }?.let(Validators::parseWeight),
                goal = state.goal,
            )
            when (authRepository.register(registration)) {
                is RegisterResult.Success -> _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                RegisterResult.EmailTaken -> {
                    takenEmail = state.email.trim().lowercase()
                    _uiState.update { it.copy(isLoading = false).validated() }
                }
            }
        }
    }

    private fun edit(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { state ->
            val edited = transform(state)
            if (showErrors) edited.validated() else edited
        }
    }

    private fun RegisterUiState.validated() = copy(
        nameError = Validators.validateName(name)?.messageRes(),
        emailError = Validators.validateEmail(email)?.messageRes()
            ?: R.string.error_email_taken.takeIf { email.trim().lowercase() == takenEmail },
        passwordError = Validators.validatePassword(password)?.messageRes(),
        passwordRepeatError = Validators.validatePasswordRepeat(password, passwordRepeat)?.messageRes(),
        heightError = Validators.validateHeight(height)?.messageRes(),
        weightError = Validators.validateWeight(weight)?.messageRes(),
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                RegisterViewModel(app.container.authRepository)
            }
        }
    }
}
