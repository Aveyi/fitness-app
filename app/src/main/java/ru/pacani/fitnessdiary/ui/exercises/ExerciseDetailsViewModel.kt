package ru.pacani.fitnessdiary.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.ExerciseRepository
import ru.pacani.fitnessdiary.domain.model.Exercise

sealed interface ExerciseDetailsUiState {
    data object Loading : ExerciseDetailsUiState
    data object NotFound : ExerciseDetailsUiState
    data class Loaded(val exercise: Exercise) : ExerciseDetailsUiState
}

class ExerciseDetailsViewModel(
    exerciseId: Long,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<ExerciseDetailsUiState> = exerciseRepository.observeExercise(exerciseId)
        .map { exercise ->
            if (exercise == null) ExerciseDetailsUiState.NotFound else ExerciseDetailsUiState.Loaded(exercise)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailsUiState.Loading)

    companion object {
        /** id приходит из маршрута ExerciseDetailsRoute, поэтому фабрика на конкретное упражнение. */
        fun factory(exerciseId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                ExerciseDetailsViewModel(exerciseId, app.container.exerciseRepository)
            }
        }
    }
}
