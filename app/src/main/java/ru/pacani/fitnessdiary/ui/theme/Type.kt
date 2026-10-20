package ru.pacani.fitnessdiary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto — системный шрифт Android, поэтому FontFamily.Default, без файлов шрифта.
// Размеры взяты из макетов, остальные стили — значения Material 3 по умолчанию.
private val Default = Typography()

val AppTypography = Typography(
    // «Привет, Иван!» на Главной
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    // Заголовок в верхней панели
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.Medium),
    // Заголовки карточек («Готов к тренировке?»)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
)
