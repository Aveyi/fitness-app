package ru.pacani.fitnessdiary.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe маршруты Navigation Compose (раздел 5 context.md).
// Суффикс Route, чтобы не путать с доменными моделями (Workout, Exercise, ...).

// Графы
@Serializable data object AuthGraph
@Serializable data object MainGraph

// auth-граф
@Serializable data object LoginRoute
@Serializable data object RegisterRoute

// main-граф: вкладки нижней навигации
@Serializable data object HomeRoute
@Serializable data object HistoryRoute
@Serializable data object StatsRoute

/** Каталог упражнений. В режиме выбора возвращает id упражнения через [PICKED_EXERCISE_ID_KEY]. */
@Serializable data class ExercisesRoute(val pickMode: Boolean = false)

// main-граф: вложенные экраны
/** Тренировка: активная или редактирование завершённой. */
@Serializable data class WorkoutRoute(val workoutId: Long)
@Serializable data class WorkoutDetailsRoute(val workoutId: Long)
@Serializable data class ExerciseDetailsRoute(val exerciseId: Long)
@Serializable data object ProfileRoute

/** Ключ в savedStateHandle экрана Тренировка, куда каталог кладёт выбранный exerciseId. */
const val PICKED_EXERCISE_ID_KEY = "pickedExerciseId"
