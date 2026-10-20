package ru.pacani.fitnessdiary.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.StatsRepository
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.stats.StatsCalculator
import ru.pacani.fitnessdiary.domain.stats.StatsSummary
import ru.pacani.fitnessdiary.domain.stats.WeekCount
import ru.pacani.fitnessdiary.domain.stats.WorkoutStat
import java.time.Clock
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val today: LocalDate,
    /** Незавершённая тренировка: кнопка «Продолжить» вместо «Начать тренировку». */
    val activeWorkout: Workout? = null,
    val lastWorkout: Workout? = null,
    /** Тренировки, упражнения и время в зале за текущую неделю. */
    val thisWeek: StatsSummary = StatsSummary.EMPTY,
    /** Выполненные упражнения за последние [HomeViewModel.CHART_WEEKS] недель, последняя — текущая. */
    val weeks: List<WeekCount> = emptyList(),
    val isStartingWorkout: Boolean = false,
    /** Тренировку надо открыть (создана или продолжается). Экран сбрасывает через onWorkoutOpened. */
    val openWorkoutId: Long? = null,
)

/** Данные из репозиториев; к ним добавляется локальное состояние кнопки «Начать». */
private data class HomeData(
    val user: User?,
    val activeWorkout: Workout?,
    val lastWorkout: Workout?,
    val stats: List<WorkoutStat>,
)

private data class StartState(val isStarting: Boolean = false, val openWorkoutId: Long? = null)

class HomeViewModel(
    authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    statsRepository: StatsRepository,
    /** Источник «сегодня» и часового пояса; в тестах подменяется. */
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val currentWeekStart = StatsCalculator.weekStart(today)
    private val chartFrom = StatsCalculator.startOf(currentWeekStart.minusWeeks(CHART_WEEKS - 1L), clock.zone)
    private val thisWeekFrom = StatsCalculator.startOf(currentWeekStart, clock.zone)

    private val startState = MutableStateFlow(StartState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user == null) {
            flowOf(HomeData(null, null, null, emptyList()))
        } else {
            combine(
                workoutRepository.observeActiveWorkout(user.id),
                workoutRepository.observeLastFinishedWorkout(user.id),
                statsRepository.observeFinishedWorkouts(user.id, chartFrom),
            ) { active, last, stats -> HomeData(user, active, last, stats) }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(data, startState) { data, start ->
        HomeUiState(
            isLoading = false,
            user = data.user,
            today = today,
            activeWorkout = data.activeWorkout,
            lastWorkout = data.lastWorkout,
            thisWeek = StatsCalculator.summarize(data.stats.filter { it.startedAt >= thisWeekFrom }),
            weeks = StatsCalculator.exercisesPerWeek(data.stats, currentWeekStart, CHART_WEEKS, clock.zone),
            isStartingWorkout = start.isStarting,
            openWorkoutId = start.openWorkoutId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(today = today),
    )

    /**
     * «Начать тренировку» или «Продолжить». [defaultTitle] — название новой тренировки
     * из ресурсов (его можно сменить на экране Тренировка).
     */
    fun onStartWorkout(defaultTitle: String) {
        val state = uiState.value
        if (state.isStartingWorkout) return
        state.activeWorkout?.let { active ->
            startState.update { it.copy(openWorkoutId = active.id) }
            return
        }
        val userId = state.user?.id ?: return

        startState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            // Если незавершённая уже есть (например, начата в другом месте), репозиторий вернёт её id
            val workoutId = workoutRepository.startWorkout(userId, defaultTitle)
            startState.update { StartState(isStarting = false, openWorkoutId = workoutId) }
        }
    }

    fun onWorkoutOpened() {
        startState.update { it.copy(openWorkoutId = null) }
    }

    companion object {
        const val CHART_WEEKS = 6

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApp).container
                HomeViewModel(container.authRepository, container.workoutRepository, container.statsRepository)
            }
        }
    }
}
