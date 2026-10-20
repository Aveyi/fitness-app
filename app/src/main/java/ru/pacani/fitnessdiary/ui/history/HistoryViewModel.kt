package ru.pacani.fitnessdiary.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.data.session.SessionRepository

data class HistoryUiState(
    val isLoading: Boolean = true,
    val groups: List<WeekGroup> = emptyList(),
    /** «Сегодня» на момент построения: нужно, чтобы решить, писать ли год в заголовке недели. */
    val today: LocalDate = LocalDate.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    sessionRepository: SessionRepository,
    private val clock: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /**
     * Тренировки, удалённые свайпом, пока на экране висит Snackbar «Отменить».
     * Из базы удаляем только после того, как Snackbar закрылся без отмены.
     */
    private val pendingDeletion = MutableStateFlow<Set<Long>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workouts = sessionRepository.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else workoutRepository.observeFinishedWorkouts(userId)
    }

    val uiState: StateFlow<HistoryUiState> =
        combine(workouts, pendingDeletion) { list, pending ->
            val today = clock().atZone(zone).toLocalDate()
            HistoryUiState(
                isLoading = false,
                groups = groupByWeek(list.filterNot { it.id in pending }, today, zone),
                today = today,
                zone = zone,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** Свайп: прячем тренировку сразу, удаление подтвердит [confirmDelete] или отменит [undoDelete]. */
    fun requestDelete(workoutId: Long) {
        pendingDeletion.update { it + workoutId }
    }

    fun undoDelete(workoutId: Long) {
        pendingDeletion.update { it - workoutId }
    }

    fun confirmDelete(workoutId: Long) {
        if (workoutId !in pendingDeletion.value) return
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workoutId)
            pendingDeletion.update { it - workoutId }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                HistoryViewModel(app.container.workoutRepository, app.container.sessionRepository)
            }
        }
    }
}
