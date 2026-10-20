package ru.pacani.fitnessdiary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.ui.navigation.AuthGraph
import ru.pacani.fitnessdiary.ui.navigation.MainGraph

/** Выбирает стартовый граф по сессии. Пока значение null, держим splash. */
class MainViewModel(authRepository: AuthRepository) : ViewModel() {

    val startDestination: StateFlow<Any?> = flow {
        emit(if (authRepository.restoreSession()) MainGraph else AuthGraph)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                MainViewModel(app.container.authRepository)
            }
        }
    }
}
