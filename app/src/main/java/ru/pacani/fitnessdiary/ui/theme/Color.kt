package ru.pacani.fitnessdiary.ui.theme

import androidx.compose.ui.graphics.Color

// Токены из макетов (раздел 12 context.md). Новые цвета на экранах не придумываем.

val Primary = Color(0xFF2557D6)
val PrimaryDark = Color(0xFF1B44AE)        // текст на светлом фоне, активные элементы
val PrimaryContainer = Color(0xFFE6EDFC)   // выбранные чипы, активная вкладка
val ChartInactive = Color(0xFFC9D7F7)      // неактивные столбцы графика

val Background = Color(0xFFF5F6F8)
val Surface = Color(0xFFFFFFFF)
val Outline = Color(0xFFC9CED6)            // обводка полей и чипов
val OutlineVariant = Color(0xFFE2E5EA)     // разделители, обводка карточек
val GridLine = Color(0xFFEEF0F3)           // сетка графика, разделители строк внутри карточки

val TextPrimary = Color(0xFF181A1F)
val TextSecondary = Color(0xFF5A606B)

val Success = Color(0xFF1E7A45)            // выполненный подход
val SuccessContainer = Color(0xFFE8F5EE)   // фон строки выполненного подхода

val Error = Color(0xFFC62828)

val Warning = Color(0xFFE07A1F)            // рекорд: полосы
val OnWarningContainer = Color(0xFF9A520D) // рекорд: текст
val WarningContainer = Color(0xFFFDF0E3)   // рекорд: фон
