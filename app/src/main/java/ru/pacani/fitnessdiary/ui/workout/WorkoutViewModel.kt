package ru.pacani.fitnessdiary.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.model.WorkoutSet

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val details: WorkoutDetails? = null,
    /** Сколько идёт тренировка (или сколько длилась, если завершена). */
    val elapsedMinutes: Long = 0,
    /** Обратный отсчёт отдыха в секундах; null — таймер не идёт. */
    val restSecondsLeft: Int? = null,
) {
    /** true — тренировка идёт сейчас, false — редактируем завершённую. */
    val isActive: Boolean get() = details?.workout?.isActive == true
}

sealed interface WorkoutEvent {
    /** Тренировка завершена или редактирование закончено — экран закрывается. */
    data object Finished : WorkoutEvent
}

class WorkoutViewModel(
    private val workoutId: Long,
    private val repository: WorkoutRepository,
    /** Источник времени; в тестах — виртуальное время. */
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Когда закончится отдых (epoch millis). Живёт во ViewModel, поэтому переживает поворот экрана. */
    private val restEndsAt = MutableStateFlow<Long?>(null)

    /** Текущее время раз в секунду: для «Идёт 42 мин» и таймера отдыха. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(TICK_MS)
        }
    }

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<WorkoutUiState> =
        combine(repository.observeWorkout(workoutId), ticker, restEndsAt) { details, now, restEnd ->
            if (details == null) {
                WorkoutUiState(isLoading = false, notFound = true)
            } else {
                val workout = details.workout
                val end = workout.finishedAt?.toEpochMilli() ?: now
                val elapsedMs = (end - workout.startedAt.toEpochMilli()).coerceAtLeast(0)
                val restLeft = if (workout.isActive && restEnd != null) {
                    ((restEnd - now + 999) / 1000).toInt().takeIf { it > 0 }
                } else {
                    null
                }
                WorkoutUiState(
                    isLoading = false,
                    details = details,
                    elapsedMinutes = elapsedMs / 60_000,
                    restSecondsLeft = restLeft,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    // ---------- Упражнения ----------

    /** Упражнение выбрано в каталоге (режим выбора). */
    fun addExercise(exerciseId: Long) {
        viewModelScope.launch { repository.addExercise(workoutId, exerciseId) }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch { repository.removeExercise(workoutExerciseId) }
    }

    // ---------- Подходы ----------

    fun addSet(workoutExerciseId: Long) {
        viewModelScope.launch { repository.addSet(workoutExerciseId) }
    }

    fun updateSet(set: WorkoutSet, weightKg: Double = set.weightKg, reps: Int = set.reps) {
        if (weightKg == set.weightKg && reps == set.reps) return
        viewModelScope.launch { repository.updateSet(set.id, weightKg, reps) }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch { repository.deleteSet(set.id) }
    }

    /** Отметка подхода. Когда подход отмечен выполненным во время тренировки, запускается отдых. */
    fun toggleSet(set: WorkoutSet) {
        val startRest = !set.isCompleted && uiState.value.isActive
        viewModelScope.launch {
            repository.toggleSetCompleted(set.id)
            if (startRest) restEndsAt.value = clock() + REST_MS
        }
    }

    // ---------- Таймер отдыха ----------

    fun addRestTime() {
        restEndsAt.update { end -> maxOf(end ?: 0L, clock()) + REST_EXTRA_MS }
    }

    fun skipRest() {
        restEndsAt.value = null
    }

    // ---------- Тренировка ----------

    fun rename(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.updateWorkoutTitle(workoutId, trimmed) }
    }

    /** Завершить тренировку (или закончить редактирование завершённой) с необязательной заметкой. */
    fun finish(note: String?) {
        val wasActive = uiState.value.isActive
        viewModelScope.launch {
            repository.updateNote(workoutId, note)
            if (wasActive) repository.finishWorkout(workoutId)
            restEndsAt.value = null
            _events.send(WorkoutEvent.Finished)
        }
    }

    companion object {
        const val REST_MS = 90_000L
        const val REST_EXTRA_MS = 30_000L
        private const val TICK_MS = 1_000L

        /** id приходит из маршрута WorkoutRoute, поэтому фабрика на конкретную тренировку. */
        fun factory(workoutId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                WorkoutViewModel(workoutId, app.container.workoutRepository)
            }
        }
    }
}
