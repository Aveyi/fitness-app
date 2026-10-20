package ru.pacani.fitnessdiary.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val weightFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.forLanguageTag("ru")))

/** 80.0 → «80», 82.5 → «82,5». Используется в SetRow и в чипах «82,5 × 6». */
fun formatWeightKg(weightKg: Double): String = weightFormat.format(weightKg)

/** Принимает и запятую, и точку: «82,5» и «82.5» → 82.5. Пустая строка → null. */
fun parseWeightKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
