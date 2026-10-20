package ru.pacani.fitnessdiary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R

/** 42 → «42 мин», 64 → «1 ч 04 мин». Для шапки тренировки, Истории и Деталей. */
@Composable
fun durationText(totalMinutes: Long): String {
    val minutes = totalMinutes.coerceAtLeast(0)
    return if (minutes < 60) {
        stringResource(R.string.duration_minutes, minutes.toInt())
    } else {
        stringResource(R.string.duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}
