package ru.pacani.fitnessdiary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import ru.pacani.fitnessdiary.ui.auth.LoginScreen
import ru.pacani.fitnessdiary.ui.auth.RegisterScreen
import ru.pacani.fitnessdiary.ui.exercises.ExerciseDetailsScreen
import ru.pacani.fitnessdiary.ui.exercises.ExercisesScreen
import ru.pacani.fitnessdiary.ui.history.HistoryScreen
import ru.pacani.fitnessdiary.ui.history.WorkoutDetailsScreen
import ru.pacani.fitnessdiary.ui.home.HomeScreen
import ru.pacani.fitnessdiary.ui.profile.ProfileScreen
import ru.pacani.fitnessdiary.ui.stats.StatsScreen
import ru.pacani.fitnessdiary.ui.workout.WorkoutScreen

/**
 * Все переходы приложения. Экраны не знают про NavController:
 * получают лямбды, а связываем их здесь.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        navigation<AuthGraph>(startDestination = LoginRoute) {
            composable<LoginRoute> {
                LoginScreen(
                    onLoggedIn = navController::navigateToMain,
                    onOpenRegister = { navController.navigate(RegisterRoute) },
                )
            }
            composable<RegisterRoute> {
                RegisterScreen(
                    onRegistered = navController::navigateToMain,
                    onBack = navController::popBackStack,
                )
            }
        }

        navigation<MainGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenWorkout = { workoutId -> navController.navigate(WorkoutRoute(workoutId)) },
                    onOpenWorkoutDetails = { workoutId -> navController.navigate(WorkoutDetailsRoute(workoutId)) },
                    onOpenStats = { navController.navigateToTab(TopLevelTab.Stats) },
                    onOpenProfile = { navController.navigate(ProfileRoute) },
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenWorkout = { workoutId -> navController.navigate(WorkoutDetailsRoute(workoutId)) },
                )
            }
            composable<StatsRoute> {
                StatsScreen()
            }
            composable<ExercisesRoute> { entry ->
                val route = entry.toRoute<ExercisesRoute>()
                ExercisesScreen(
                    pickMode = route.pickMode,
                    onOpenExercise = { exerciseId -> navController.navigate(ExerciseDetailsRoute(exerciseId)) },
                    onExercisePicked = { exerciseId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(PICKED_EXERCISE_ID_KEY, exerciseId)
                        navController.popBackStack()
                    },
                    onBack = navController::popBackStack,
                )
            }
            composable<WorkoutRoute> { entry ->
                val route = entry.toRoute<WorkoutRoute>()
                // Сюда каталог в режиме выбора кладёт exerciseId (см. ExercisesRoute выше).
                // ViewModel тренировки может читать тот же ключ из своего SavedStateHandle.
                val pickedExerciseId by entry.savedStateHandle
                    .getStateFlow<Long?>(PICKED_EXERCISE_ID_KEY, null)
                    .collectAsStateWithLifecycle()
                WorkoutScreen(
                    workoutId = route.workoutId,
                    pickedExerciseId = pickedExerciseId,
                    onPickedExerciseHandled = { entry.savedStateHandle.remove<Long>(PICKED_EXERCISE_ID_KEY) },
                    onAddExercise = { navController.navigate(ExercisesRoute(pickMode = true)) },
                    onFinished = navController::popBackStack,
                    onBack = navController::popBackStack,
                )
            }
            composable<WorkoutDetailsRoute> { entry ->
                val route = entry.toRoute<WorkoutDetailsRoute>()
                WorkoutDetailsScreen(
                    workoutId = route.workoutId,
                    onEdit = { navController.navigate(WorkoutRoute(route.workoutId)) },
                    onBack = navController::popBackStack,
                )
            }
            composable<ExerciseDetailsRoute> { entry ->
                val route = entry.toRoute<ExerciseDetailsRoute>()
                ExerciseDetailsScreen(
                    exerciseId = route.exerciseId,
                    onBack = navController::popBackStack,
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onLoggedOut = navController::navigateToAuth,
                    onBack = navController::popBackStack,
                )
            }
        }
    }
}

/** После входа или регистрации: в main-граф, auth-граф убираем из back stack. */
fun NavHostController.navigateToMain() {
    navigate(MainGraph) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** После выхода: в auth-граф с полной очисткой back stack. */
fun NavHostController.navigateToAuth() {
    navigate(AuthGraph) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** Переход по вкладке: у каждой вкладки свой сохранённый стек, повторное нажатие не плодит копии. */
fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
