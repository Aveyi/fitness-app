package ru.pacani.fitnessdiary.ui.stats

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
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.StatsRepository
import ru.pacani.fitnessdiary.domain.stats.PersonalRecord
import ru.pacani.fitnessdiary.domain.stats.StatsCalculator
import ru.pacani.fitnessdiary.domain.stats.StatsPeriod
import ru.pacani.fitnessdiary.domain.stats.StatsSummary
import ru.pacani.fitnessdiary.domain.stats.WeekCount
import ru.pacani.fitnessdiary.domain.stats.WorkoutStat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class StatsUiState(
    val isLoading: Boolean = true,
    val period: StatsPeriod = DEFAULT_PERIOD,
    /** Тренировки, упражнения и среднее за выбранный период. */
    val summary: StatsSummary = StatsSummary.EMPTY,
    /** Недели периода от старых к новым, последняя — текущая. */
    val weeks: List<WeekCount> = emptyList(),
    val bestWeek: WeekCount? = null,
    /** Выполненных упражнений в среднем за неделю периода. */
    val averageExercisesPerWeek: Double = 0.0,
    /** За всё время, независимо от периода: рекорд он и есть рекорд. */
    val records: List<PersonalRecord> = emptyList(),
    /** Рекорды, поставленные после этого момента (на текущей неделе), помечаются как новые. */
    val newRecordsSince: Instant = Instant.MAX,
) {
    val isEmpty: Boolean get() = summary.workoutCount == 0 && records.isEmpty()

    fun isNew(record: PersonalRecord): Boolean = record.achievedAt >= newRecordsSince
}

/** Как выбрано в макете. */
val DEFAULT_PERIOD = StatsPeriod.THREE_MONTHS

private data class StatsData(val workouts: List<WorkoutStat>, val records: List<PersonalRecord>)

class StatsViewModel(
    authRepository: AuthRepository,
    statsRepository: StatsRepository,
    /** Источник «сегодня» и часового пояса; в тестах подменяется. */
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val currentWeekStart: LocalDate = StatsCalculator.weekStart(LocalDate.now(clock))
    private val period = MutableStateFlow(DEFAULT_PERIOD)

    // Все завершённые тренировки: период фильтруем в памяти, чтобы переключение было мгновенным
    @OptIn(ExperimentalCoroutinesApi::class)
    private val data = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user == null) {
            flowOf(StatsData(emptyList(), emptyList()))
        } else {
            combine(
                statsRepository.observeFinishedWorkouts(user.id, Instant.EPOCH),
                statsRepository.observePersonalRecords(user.id),
            ) { workouts, records -> StatsData(workouts, records) }
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(data, period) { data, period ->
        buildState(data, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun onPeriodChange(newPeriod: StatsPeriod) {
        period.value = newPeriod
    }

    private fun buildState(data: StatsData, period: StatsPeriod): StatsUiState {
        val zone = clock.zone
        val firstWorkoutDate = data.workouts.firstOrNull()?.startedAt?.atZone(zone)?.toLocalDate()
        val weekCount = StatsCalculator.periodWeeks(period, currentWeekStart, firstWorkoutDate)
        val from = StatsCalculator.startOf(currentWeekStart.minusWeeks(weekCount - 1L), zone)
        val inPeriod = data.workouts.filter { it.startedAt >= from }

        val summary = StatsCalculator.summarize(inPeriod)
        val weeks = StatsCalculator.exercisesPerWeek(inPeriod, currentWeekStart, weekCount, zone)
        return StatsUiState(
            isLoading = false,
            period = period,
            summary = summary,
            weeks = weeks,
            bestWeek = StatsCalculator.bestWeek(weeks, currentWeekStart),
            averageExercisesPerWeek = summary.exerciseCount.toDouble() / weekCount,
            records = data.records,
            newRecordsSince = StatsCalculator.startOf(currentWeekStart, zone),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApp).container
                StatsViewModel(container.authRepository, container.statsRepository)
            }
        }
    }
}
