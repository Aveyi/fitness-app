package ru.pacani.fitnessdiary.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.pacani.fitnessdiary.ui.navigation.AppNavHost
import ru.pacani.fitnessdiary.ui.navigation.AuthGraph
import ru.pacani.fitnessdiary.ui.navigation.BottomBar
import ru.pacani.fitnessdiary.ui.navigation.ExercisesRoute
import ru.pacani.fitnessdiary.ui.navigation.TopLevelTab
import ru.pacani.fitnessdiary.ui.navigation.navigateToTab
import ru.pacani.fitnessdiary.ui.navigation.topLevelTab

@Composable
fun FitnessDiaryApp(
    // Пока сессии нет, всегда стартуем со входа. Потом сюда придёт граф по currentUserId.
    startDestination: Any = AuthGraph,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.bottomBarTab()

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                BottomBar(
                    selected = currentTab,
                    onTabClick = navController::navigateToTab,
                )
            }
        },
        // Системные отступы обрабатывает Scaffold каждого экрана
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                // Чтобы экран не добавил отступ навигационной панели второй раз под BottomBar
                .consumeWindowInsets(innerPadding),
        )
    }
}

/** Вкладка для BottomBar или null, если панель на этом экране скрыта (в том числе каталог в режиме выбора). */
private fun NavBackStackEntry.bottomBarTab(): TopLevelTab? {
    val tab = destination.topLevelTab()
    if (tab == TopLevelTab.Exercises && toRoute<ExercisesRoute>().pickMode) return null
    return tab
}
