package ru.pacani.fitnessdiary.ui.profile

import androidx.annotation.StringRes
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.FitnessApp
import ru.pacani.fitnessdiary.data.repository.AuthRepository
import ru.pacani.fitnessdiary.data.repository.WorkoutRepository
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.validation.Validators
import ru.pacani.fitnessdiary.ui.auth.messageRes
import ru.pacani.fitnessdiary.ui.components.formatWeightKg

/** Поля формы «Параметры» в том виде, как их ввёл пользователь. */
data class ProfileForm(
    val height: String = "",
    val weight: String = "",
    val goal: Goal = Goal.STRENGTH,
    @param:StringRes val heightError: Int? = null,
    @param:StringRes val weightError: Int? = null,
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val finishedWorkoutCount: Int = 0,
    val form: ProfileForm = ProfileForm(),
    /** Форма отличается от сохранённого профиля: кнопка «Сохранить» активна. */
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    /** Показать «Изменения сохранены»; экран сбрасывает через onSavedMessageShown. */
    val showSavedMessage: Boolean = false,
    /** Сессия очищена: экран переходит ко входу. */
    val isLoggedOut: Boolean = false,
)

private data class Flags(
    val isSaving: Boolean = false,
    val showSavedMessage: Boolean = false,
    val isLoggedOut: Boolean = false,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    /** null, пока не пришёл пользователь: форму заполняем его данными один раз. */
    private val form = MutableStateFlow<ProfileForm?>(null)
    private val flags = MutableStateFlow(Flags())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val profile = authRepository.observeCurrentUser()
        .onEach { user -> if (user != null && form.value == null) form.value = user.toForm() }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(user to 0)
            } else {
                workoutRepository.observeFinishedWorkouts(user.id).map { user to it.size }
            }
        }

    val uiState: StateFlow<ProfileUiState> = combine(profile, form, flags) { (user, count), form, flags ->
        val currentForm = form ?: ProfileForm()
        ProfileUiState(
            isLoading = user == null && !flags.isLoggedOut,
            user = user,
            finishedWorkoutCount = count,
            form = currentForm,
            hasChanges = user != null && currentForm.differsFrom(user),
            isSaving = flags.isSaving,
            showSavedMessage = flags.showSavedMessage,
            isLoggedOut = flags.isLoggedOut,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onHeightChange(value: String) = editForm { it.copy(height = value, heightError = null) }
    fun onWeightChange(value: String) = editForm { it.copy(weight = value, weightError = null) }
    fun onGoalChange(goal: Goal) = editForm { it.copy(goal = goal) }

    fun onSave() {
        val current = form.value ?: return
        if (flags.value.isSaving) return

        val heightError = Validators.validateHeight(current.height)?.messageRes()
        val weightError = Validators.validateWeight(current.weight)?.messageRes()
        if (heightError != null || weightError != null) {
            form.value = current.copy(heightError = heightError, weightError = weightError)
            return
        }

        flags.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            authRepository.updateProfile(
                heightCm = current.height.takeIf { it.isNotBlank() }?.let(Validators::parseHeight),
                weightKg = current.weight.takeIf { it.isNotBlank() }?.let(Validators::parseWeight),
                goal = current.goal,
            )
            flags.update { it.copy(isSaving = false, showSavedMessage = true) }
        }
    }

    fun onSavedMessageShown() {
        flags.update { it.copy(showSavedMessage = false) }
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            flags.update { it.copy(isLoggedOut = true) }
        }
    }

    private fun editForm(transform: (ProfileForm) -> ProfileForm) {
        form.update { it?.let(transform) }
    }

    private fun User.toForm() = ProfileForm(
        height = heightCm?.toString().orEmpty(),
        weight = weightKg?.let(::formatWeightKg).orEmpty(),
        goal = goal,
    )

    /** Сравниваем значения, а не строки: «78» и «78,0» — одно и то же. */
    private fun ProfileForm.differsFrom(user: User): Boolean {
        val height = height.takeIf { it.isNotBlank() }?.let(Validators::parseHeight)
        val weight = weight.takeIf { it.isNotBlank() }?.let(Validators::parseWeight)
        val heightChanged = if (this.height.isBlank()) user.heightCm != null else height != user.heightCm
        val weightChanged = if (this.weight.isBlank()) user.weightKg != null else weight != user.weightKg
        return heightChanged || weightChanged || goal != user.goal
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApp).container
                ProfileViewModel(container.authRepository, container.workoutRepository)
            }
        }
    }
}
