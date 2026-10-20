package ru.pacani.fitnessdiary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.theme.Background
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.Outline
import ru.pacani.fitnessdiary.ui.theme.Primary
import ru.pacani.fitnessdiary.ui.theme.Success
import ru.pacani.fitnessdiary.ui.theme.SuccessContainer
import ru.pacani.fitnessdiary.ui.theme.Surface
import ru.pacani.fitnessdiary.ui.theme.TextPrimary
import ru.pacani.fitnessdiary.ui.theme.TextSecondary

/**
 * Строка подхода на экране «Тренировка»: номер, степпер веса, степпер повторений
 * и кнопка «выполнено» с анимацией.
 *
 * Компонент ничего не хранит сам: все значения и колбэки приходят снаружи.
 * Удаление подхода — долгим нажатием на строку (экран может показать Snackbar «Отменить»).
 *
 * @param number номер подхода для отображения, с 1 (не orderIndex: после удаления в нём бывают пропуски).
 * @param weightStep шаг степпера веса, по умолчанию 2,5 кг.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetRow(
    number: Int,
    weightKg: Double,
    reps: Int,
    isCompleted: Boolean,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    weightStep: Double = 2.5,
) {
    val rowColor by animateColorAsState(
        targetValue = if (isCompleted) SuccessContainer else Background,
        animationSpec = tween(250),
        label = "rowColor",
    )
    val numberColor by animateColorAsState(
        targetValue = if (isCompleted) Success else TextPrimary,
        animationSpec = tween(250),
        label = "numberColor",
    )
    val deleteLabel = stringResource(R.string.set_row_delete)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowColor)
            .combinedClickable(
                onClick = {},
                onLongClick = onDelete,
                onLongClickLabel = deleteLabel,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = number.toString(),
            modifier = Modifier.width(NumberColumnWidth),
            color = numberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        NumberStepper(
            displayValue = formatWeightKg(weightKg),
            keyboardType = KeyboardType.Decimal,
            isAllowedInput = { text -> text.length <= 6 && text.all { it.isDigit() || it == ',' || it == '.' } },
            onTextInput = { text -> parseWeightKg(text)?.let { onWeightChange(it.coerceAtLeast(0.0)) } },
            onDecrement = { onWeightChange((weightKg - weightStep).coerceAtLeast(0.0)) },
            onIncrement = { onWeightChange(weightKg + weightStep) },
            decrementLabel = stringResource(R.string.set_row_weight_decrease),
            incrementLabel = stringResource(R.string.set_row_weight_increase),
            fieldLabel = stringResource(R.string.set_row_weight),
            modifier = Modifier.weight(1f),
        )
        NumberStepper(
            displayValue = reps.toString(),
            keyboardType = KeyboardType.Number,
            isAllowedInput = { text -> text.length <= 3 && text.all { it.isDigit() } },
            onTextInput = { text -> text.toIntOrNull()?.let(onRepsChange) },
            onDecrement = { onRepsChange((reps - 1).coerceAtLeast(0)) },
            onIncrement = { onRepsChange(reps + 1) },
            decrementLabel = stringResource(R.string.set_row_reps_decrease),
            incrementLabel = stringResource(R.string.set_row_reps_increase),
            fieldLabel = stringResource(R.string.set_row_reps),
            modifier = Modifier.weight(1f),
        )
        CompleteButton(isCompleted = isCompleted, onToggle = onToggle)
    }
}

/**
 * Подписи колонок над подходами: «№ · Вес, кг · Повторы · Готово».
 * Ширины совпадают с SetRow, поэтому колонки ровно стоят друг под другом.
 */
@Composable
fun SetRowHeader(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.set_row_header_number), Modifier.width(NumberColumnWidth), style = style, textAlign = TextAlign.Center)
        Text(stringResource(R.string.set_row_weight), Modifier.weight(1f), style = style, textAlign = TextAlign.Center)
        Text(stringResource(R.string.set_row_reps), Modifier.weight(1f), style = style, textAlign = TextAlign.Center)
        Text(stringResource(R.string.set_row_header_done), Modifier.width(CompleteButtonSize), style = style, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------

private val NumberColumnWidth = 24.dp
private val CompleteButtonSize = 48.dp
private val StepperHeight = 44.dp

/**
 * Поле «− значение +». Значение можно ввести с клавиатуры: пока поле в фокусе,
 * показываем то, что набрал пользователь, а после потери фокуса — значение из состояния.
 */
@Composable
private fun NumberStepper(
    displayValue: String,
    keyboardType: KeyboardType,
    isAllowedInput: (String) -> Boolean,
    onTextInput: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementLabel: String,
    incrementLabel: String,
    fieldLabel: String,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Значение изменилось снаружи (кнопки ±, база) — показываем его, если пользователь сейчас не печатает.
    LaunchedEffect(displayValue, isFocused) {
        if (!isFocused) text = displayValue
    }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Primary else Outline,
        label = "stepperBorder",
    )

    Row(
        modifier = modifier
            .height(StepperHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(symbol = "−", label = decrementLabel, onClick = onDecrement)
        BasicTextField(
            value = text,
            onValueChange = { newText ->
                if (isAllowedInput(newText)) {
                    text = newText
                    onTextInput(newText)
                }
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused }
                .semantics { contentDescription = fieldLabel },
            singleLine = true,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(Primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        StepperButton(symbol = "+", label = incrementLabel, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, color = TextPrimary, fontSize = 22.sp)
    }
}

/** Квадратная кнопка с галочкой: заливается зелёным и «подпрыгивает» при отметке. */
@Composable
private fun CompleteButton(isCompleted: Boolean, onToggle: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (isCompleted) Success else Surface,
        animationSpec = tween(250),
        label = "checkContainer",
    )
    val border by animateColorAsState(
        targetValue = if (isCompleted) Success else Outline,
        animationSpec = tween(250),
        label = "checkBorder",
    )
    val checkColor by animateColorAsState(
        targetValue = if (isCompleted) Color.White else TextSecondary,
        animationSpec = tween(250),
        label = "checkColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    val doneLabel = stringResource(R.string.set_row_done)
    val stateLabel = stringResource(if (isCompleted) R.string.set_row_state_done else R.string.set_row_state_not_done)

    Box(
        modifier = Modifier
            .size(CompleteButtonSize)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = doneLabel
                stateDescription = stateLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(22.dp)
                .scale(scale),
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.52f)
                lineTo(size.width * 0.40f, size.height * 0.76f)
                lineTo(size.width * 0.86f, size.height * 0.26f)
            }
            drawPath(
                path = path,
                color = checkColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Preview(name = "Подход не выполнен", showBackground = true, widthDp = 360)
@Composable
private fun SetRowPreview() {
    FitnessDiaryTheme {
        SetRow(
            number = 3, weightKg = 82.5, reps = 6, isCompleted = false,
            onWeightChange = {}, onRepsChange = {}, onToggle = {}, onDelete = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(name = "Подход выполнен", showBackground = true, widthDp = 360)
@Composable
private fun SetRowCompletedPreview() {
    FitnessDiaryTheme {
        SetRow(
            number = 1, weightKg = 80.0, reps = 8, isCompleted = true,
            onWeightChange = {}, onRepsChange = {}, onToggle = {}, onDelete = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

/** Интерактивное превью: в режиме Interactive можно нажимать ± и галочку. */
@Preview(name = "Карточка упражнения", showBackground = true, widthDp = 360)
@Composable
private fun SetRowListPreview() {
    FitnessDiaryTheme {
        var sets by remember {
            mutableStateOf(listOf(Triple(80.0, 8, true), Triple(80.0, 8, true), Triple(82.5, 6, false)))
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SetRowHeader()
            sets.forEachIndexed { index, (weight, reps, done) ->
                SetRow(
                    number = index + 1,
                    weightKg = weight,
                    reps = reps,
                    isCompleted = done,
                    onWeightChange = { w -> sets = sets.toMutableList().also { it[index] = Triple(w, reps, done) } },
                    onRepsChange = { r -> sets = sets.toMutableList().also { it[index] = Triple(weight, r, done) } },
                    onToggle = { sets = sets.toMutableList().also { it[index] = Triple(weight, reps, !done) } },
                    onDelete = { sets = sets.filterIndexed { i, _ -> i != index } },
                )
            }
        }
    }
}
