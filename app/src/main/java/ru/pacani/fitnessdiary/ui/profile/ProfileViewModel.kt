package ru.pacani.fitnessdiary.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.AuthRepository

/** Пока только выход. Данные профиля и их правка добавятся вместе с экраном. */
class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _isLoggedOut = MutableStateFlow(false)

    /** Сессия очищена: экран переходит ко входу. */
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedOut.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                ProfileViewModel(app.container.authRepository)
            }
        }
    }
}
