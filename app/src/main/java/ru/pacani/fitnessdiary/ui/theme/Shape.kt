package ru.pacani.fitnessdiary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),  // поля ввода
    medium = RoundedCornerShape(16.dp), // карточки
    large = RoundedCornerShape(16.dp),  // плавающая кнопка
)

// Компоненты, у которых в Material 3 своя форма по умолчанию: передаём явно через shape = ...
val ButtonShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(18.dp)
val CardShape = RoundedCornerShape(16.dp)
