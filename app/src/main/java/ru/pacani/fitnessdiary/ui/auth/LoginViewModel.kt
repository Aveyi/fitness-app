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
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.LoginResult
import ru.pacani.fitnessdiary.domain.validation.Validators

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    /** Ошибка формы целиком: «Неверный email или пароль». */
    @param:StringRes val formError: Int? = null,
    val isLoading: Boolean = false,
    /** Вход выполнен: экран переходит на Главную. */
    val isLoggedIn: Boolean = false,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, formError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, formError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isLoading) return

        // При входе правила нового пароля не проверяем: только что поле не пустое
        val emailError = Validators.validateEmail(state.email)?.messageRes()
        val passwordError = if (state.password.isEmpty()) R.string.error_required else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            when (authRepository.login(state.email, state.password)) {
                is LoginResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                LoginResult.InvalidCredentials -> _uiState.update {
                    it.copy(isLoading = false, formError = R.string.error_invalid_credentials)
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                LoginViewModel(app.container.authRepository)
            }
        }
    }
}
