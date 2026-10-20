package ru.pacani.fitnessdiary.ui.exercises

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.data.repository.ExerciseRepository
import ru.pacani.fitnessdiary.data.session.SessionRepository
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

data class ExercisesUiState(
    val query: String = "",
    /** null — «Все». */
    val selectedCategory: ExerciseCategory? = null,
    val exercises: List<Exercise> = emptyList(),
    /** Первая загрузка каталога, пока список пуст. */
    val isLoading: Boolean = true,
    /** Pull-to-refresh. */
    val isRefreshing: Boolean = false,
)

class ExercisesViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<ExerciseCategory?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private val isInitialLoading = MutableStateFlow(true)

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** Сообщения для Snackbar (id строки). */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val exercises: Flow<List<Exercise>> =
        combine(sessionRepository.currentUserId, query, category) { userId, q, c -> Triple(userId, q, c) }
            .flatMapLatest { (userId, q, c) -> exerciseRepository.observeExercises(userId, q, c) }

    val uiState: StateFlow<ExercisesUiState> =
        combine(exercises, query, category, isRefreshing, isInitialLoading) { list, q, c, refreshing, initial ->
            ExercisesUiState(
                query = q,
                selectedCategory = c,
                exercises = list,
                isLoading = initial && list.isEmpty(),
                isRefreshing = refreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    init {
        // Первый запуск: кэш пуст — загружаем каталог из wger
        viewModelScope.launch {
            exerciseRepository.refreshIfEmpty().onFailure { _messages.send(it.toMessage()) }
            isInitialLoading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(value: ExerciseCategory?) {
        category.value = value
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            exerciseRepository.refresh().onFailure { _messages.send(it.toMessage()) }
            isRefreshing.value = false
        }
    }

    fun addCustomExercise(name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            val userId = sessionRepository.currentUserId.first()
            if (userId == null) {
                _messages.send(R.string.exercises_error_not_logged_in)
                return@launch
            }
            exerciseRepository.addCustomExercise(userId, name, category)
            _messages.send(R.string.exercises_custom_added)
        }
    }

    @StringRes
    private fun Throwable.toMessage(): Int =
        if (this is IOException) R.string.exercises_error_no_connection else R.string.exercises_error_load

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FitnessApp
                ExercisesViewModel(app.container.exerciseRepository, app.container.sessionRepository)
            }
        }
    }
}
