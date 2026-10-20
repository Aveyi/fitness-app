package ru.pacani.fitnessdiary.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.model.WorkoutExercise

/** Упражнение в деталях: [recordWeightKg] — новый личный рекорд в этой тренировке, иначе null. */
data class ExerciseSummary(
    val exercise: WorkoutExercise,
    val recordWeightKg: Double?,
)

sealed interface WorkoutDetailsUiState {
    data object Loading : WorkoutDetailsUiState
    data object NotFound : WorkoutDetailsUiState
    data class Loaded(
        val details: WorkoutDetails,
        val exercises: List<ExerciseSummary>,
    ) : WorkoutDetailsUiState
}

sealed interface WorkoutDetailsEvent {
    data object Deleted : WorkoutDetailsEvent
}

class WorkoutDetailsViewModel(
    private val workoutId: Long,
    private val repository: WorkoutRepository,
) : ViewModel() {

    /** Пока идёт удаление, не показываем «не найдено» на долю секунды перед закрытием экрана. */
    private val isDeleting = MutableStateFlow(false)

    private val _events = Channel<WorkoutDetailsEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutDetailsEvent> = _events.receiveAsFlow()

    private val loaded: Flow<WorkoutDetailsUiState> = repository.observeWorkout(workoutId).map { details ->
        if (details == null) {
            WorkoutDetailsUiState.NotFound
        } else {
            WorkoutDetailsUiState.Loaded(details, details.exercises.map { summarize(details, it) })
        }
    }

    val uiState: StateFlow<WorkoutDetailsUiState> =
        combine(loaded, isDeleting) { state, deleting ->
            if (deleting && state is WorkoutDetailsUiState.NotFound) WorkoutDetailsUiState.Loading else state
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailsUiState.Loading)

    /**
     * Рекорд — лучший вес в этой тренировке, если он больше, чем во всех прошлых тренировках
     * с этим упражнением. Первое выполнение упражнения рекордом не считаем: сравнивать не с чем.
     */
    private suspend fun summarize(details: WorkoutDetails, exercise: WorkoutExercise): ExerciseSummary {
        val best = exercise.sets.filter { it.isCompleted }.maxOfOrNull { it.weightKg }
        if (best == null || best <= 0.0) return ExerciseSummary(exercise, null)
        val previous = repository.getBestWeightBefore(
            userId = details.workout.userId,
            exerciseId = exercise.exercise.id,
            beforeWorkoutStartedAt = details.workout.startedAt.toEpochMilli(),
        )
        val record = if (previous != null && best > previous) best else null
        return ExerciseSummary(exercise, record)
    }

    fun delete() {
        isDeleting.value = true
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            _events.send(WorkoutDetailsEvent.Deleted)
        }
    }

    companion object {
        fun factory(workoutId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                WorkoutDetailsViewModel(workoutId, app.container.workoutRepository)
            }
        }
    }
}
