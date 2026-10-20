package ru.pacani.fitnessdiary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.validation.PasswordStrength
import ru.pacani.fitnessdiary.ui.theme.Error
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.OnWarningContainer
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.Success
import ru.pacani.fitnessdiary.ui.theme.Surface
import ru.pacani.fitnessdiary.ui.theme.Warning

private const val SEGMENT_COUNT = 4
private const val ANIMATION_MS = 300

/**
 * Шкала надёжности пароля под полем «Пароль» на экране Регистрация.
 * Силу считает [ru.pacani.fitnessdiary.domain.validation.evaluatePasswordStrength], компонент только рисует.
 *
 * Сегменты заполняются плавно, цвет перетекает от красного через оранжевый к зелёному.
 */
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val filled by animateFloatAsState(
        targetValue = strength.filledSegments.toFloat(),
        animationSpec = tween(ANIMATION_MS),
        label = "filledSegments",
    )
    val barColor by animateColorAsState(strength.barColor(), tween(ANIMATION_MS), label = "barColor")
    val textColor by animateColorAsState(strength.textColor(), tween(ANIMATION_MS), label = "textColor")
    val label = strength.label()?.let { stringResource(it) }
    val description = strength.description()?.let { stringResource(it) }

    Row(
        modifier = modifier.clearAndSetSemantics {
            if (description != null) contentDescription = description
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(SEGMENT_COUNT) { index ->
                // Сегмент index заполнен на (filled - index): при анимации 1 → 2 второй растёт слева направо
                val fraction = (filled - index).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(OutlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(barColor),
                    )
                }
            }
        }
        // Место под подпись постоянное, чтобы шкала не дёргалась, когда подпись появляется
        Text(
            text = label.orEmpty(),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 68.dp),
        )
    }
}

private fun PasswordStrength.barColor(): Color = when (this) {
    PasswordStrength.NONE -> OutlineVariant
    PasswordStrength.WEAK -> Error
    PasswordStrength.MEDIUM -> Warning
    PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> Success
}

private fun PasswordStrength.textColor(): Color = when (this) {
    PasswordStrength.NONE -> Color.Transparent
    PasswordStrength.WEAK -> Error
    PasswordStrength.MEDIUM -> OnWarningContainer
    PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> Success
}

private fun PasswordStrength.label(): Int? = when (this) {
    PasswordStrength.NONE -> null
    PasswordStrength.WEAK -> R.string.password_strength_weak
    PasswordStrength.MEDIUM -> R.string.password_strength_medium
    PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> R.string.password_strength_strong
}

private fun PasswordStrength.description(): Int? = when (this) {
    PasswordStrength.NONE -> null
    PasswordStrength.WEAK -> R.string.password_strength_cd_weak
    PasswordStrength.MEDIUM -> R.string.password_strength_cd_medium
    PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> R.string.password_strength_cd_strong
}

@Preview(widthDp = 360)
@Composable
private fun PasswordStrengthIndicatorPreview() {
    FitnessDiaryTheme {
        Column(
            modifier = Modifier
                .background(Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PasswordStrength.entries.forEach { strength ->
                Text(strength.name, fontSize = 11.sp)
                PasswordStrengthIndicator(strength)
            }
        }
    }
}
