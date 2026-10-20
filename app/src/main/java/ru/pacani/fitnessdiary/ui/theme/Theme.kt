package ru.pacani.fitnessdiary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Только светлая тема: тёмную решили не делать.
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    // secondaryContainer — индикатор активной вкладки в NavigationBar и выбранные чипы
    secondary = PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = PrimaryContainer,
    onSecondaryContainer = PrimaryDark,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Background,
    onSurfaceVariant = TextSecondary,
    // Панели, карточки и меню в макетах белые
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Surface,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = Color.White,
)

@Composable
fun FitnessDiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
