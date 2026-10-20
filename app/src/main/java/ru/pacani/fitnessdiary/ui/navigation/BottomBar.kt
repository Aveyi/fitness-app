package ru.pacani.fitnessdiary.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import kotlin.reflect.KClass

enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(HomeRoute, HomeRoute::class, R.string.tab_home, Icons.Outlined.Home),
    History(HistoryRoute, HistoryRoute::class, R.string.tab_history, Icons.Outlined.History),
    Stats(StatsRoute, StatsRoute::class, R.string.tab_stats, Icons.Outlined.BarChart),
    Exercises(
        ExercisesRoute(),
        ExercisesRoute::class,
        R.string.tab_exercises,
        Icons.Outlined.FitnessCenter,
    ),
}

@Composable
fun BottomBar(
    selected: TopLevelTab?,
    onTabClick: (TopLevelTab) -> Unit,
) {
    NavigationBar {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onTabClick(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/** Вкладка, которой принадлежит экран, или null, если на этом экране панель скрыта. */
fun NavDestination.topLevelTab(): TopLevelTab? =
    TopLevelTab.entries.firstOrNull { hasRoute(it.routeClass) }

@Preview
@Composable
private fun BottomBarPreview() {
    FitnessDiaryTheme {
        BottomBar(selected = TopLevelTab.Home, onTabClick = {})
    }
}
