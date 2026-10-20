package ru.pacani.fitnessdiary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.theme.ChartInactive
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.GridLine
import ru.pacani.fitnessdiary.ui.theme.Outline
import ru.pacani.fitnessdiary.ui.theme.Primary
import ru.pacani.fitnessdiary.ui.theme.Surface
import ru.pacani.fitnessdiary.ui.theme.TextPrimary
import ru.pacani.fitnessdiary.ui.theme.TextSecondary
import kotlin.math.ceil
import kotlin.math.min

/** Столбец графика: подпись недели («12 окт») и количество выполненных упражнений. */
@Immutable
data class ChartPoint(
    val label: String,
    val value: Int,
    /** Текущая (ещё не закончившаяся) неделя. */
    val isCurrent: Boolean = false,
)

enum class ProgressChartStyle {
    /** Статистика: ось Y с сеткой, выделена лучшая неделя, текущая обведена пунктиром. */
    Full,

    /** Главная: без осей, подпись под каждым столбцом, выделена текущая неделя. */
    Compact,
}

/**
 * График «Упражнений по неделям» на Canvas. Столбцы вырастают снизу при появлении и при смене данных.
 * Данные приходят снаружи; если их нет или все нули, показывается [emptyText].
 */
@Composable
fun ProgressChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    style: ProgressChartStyle = ProgressChartStyle.Full,
    contentDescription: String? = null,
    emptyText: String = stringResource(R.string.chart_empty),
) {
    val height = when (style) {
        ProgressChartStyle.Full -> 164.dp
        ProgressChartStyle.Compact -> 104.dp
    }
    val sizeModifier = modifier
        .fillMaxWidth()
        .height(height)

    if (points.none { it.value > 0 }) {
        Box(sizeModifier, contentAlignment = Alignment.Center) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // В @Preview анимация не идёт, поэтому там сразу рисуем итоговое состояние
    val inPreview = LocalInspectionMode.current
    val progress = remember { Animatable(if (inPreview) 1f else 0f) }
    LaunchedEffect(points) {
        if (inPreview) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = sizeModifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        when (style) {
            ProgressChartStyle.Full -> drawFullChart(points, progress.value, textMeasurer)
            ProgressChartStyle.Compact -> drawCompactChart(points, progress.value, textMeasurer)
        }
    }
}

private val LabelStyle = TextStyle(fontSize = 11.sp, color = TextSecondary)

private fun DrawScope.drawFullChart(points: List<ChartPoint>, progress: Float, textMeasurer: TextMeasurer) {
    val axisWidth = 20.dp.toPx()
    val axisGap = 8.dp.toPx()
    val labelGap = 6.dp.toPx()
    val labelHeight = textMeasurer.measure("0", LabelStyle).size.height
    val barsBottom = size.height - labelGap - labelHeight
    val chartLeft = axisWidth + axisGap
    val chartWidth = size.width - chartLeft

    val ticks = chartAxisTicks(points.maxOf { it.value })
    val axisMax = ticks.last().toFloat()
    fun yOf(value: Float) = barsBottom * (1f - value / axisMax)

    // Сетка и подписи оси Y
    ticks.forEach { tick ->
        val y = yOf(tick.toFloat())
        if (tick != 0) {
            drawLine(GridLine, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val text = textMeasurer.measure(tick.toString(), LabelStyle)
        val textY = (y - text.size.height / 2f).coerceIn(0f, barsBottom - text.size.height)
        drawText(text, topLeft = Offset(axisWidth - text.size.width, textY))
    }
    drawLine(Outline, Offset(chartLeft, barsBottom), Offset(size.width, barsBottom), strokeWidth = 1.dp.toPx())

    // Столбцы
    val slot = chartWidth / points.size
    val gap = min(6.dp.toPx(), slot * 0.3f)
    val barWidth = slot - gap + gap / points.size
    val radius = CornerRadius(4.dp.toPx())
    val best = bestIndex(points)
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    val strokeWidth = 1.dp.toPx()

    points.forEachIndexed { index, point ->
        val barHeight = (barsBottom - yOf(point.value.toFloat())) * progress
        if (barHeight <= 0f) return@forEachIndexed
        val rect = Rect(
            offset = Offset(chartLeft + index * (barWidth + gap), barsBottom - barHeight),
            size = Size(barWidth, barHeight),
        )
        val shape = topRoundedPath(rect, radius)
        when {
            point.isCurrent -> {
                drawPath(shape, ChartInactive)
                // В первых кадрах анимации столбец ниже толщины линии: пунктир рисовать рано
                if (rect.height <= strokeWidth) return@forEachIndexed
                val inset = strokeWidth / 2
                drawPath(
                    topRoundedPath(rect.deflate(inset), radius),
                    Primary,
                    style = Stroke(width = strokeWidth, pathEffect = dash),
                )
            }
            index == best -> drawPath(shape, Primary)
            else -> drawPath(shape, ChartInactive)
        }
    }

    // Подписи недель: не больше четырёх, крайние прижаты к краям
    chartLabelIndices(points.size).forEach { index ->
        val text = textMeasurer.measure(points[index].label, LabelStyle)
        val center = chartLeft + index * (barWidth + gap) + barWidth / 2
        val x = (center - text.size.width / 2f).coerceIn(chartLeft, size.width - text.size.width)
        drawText(text, topLeft = Offset(x, barsBottom + labelGap))
    }
}

private fun DrawScope.drawCompactChart(points: List<ChartPoint>, progress: Float, textMeasurer: TextMeasurer) {
    val labelGap = 6.dp.toPx()
    val labelHeight = textMeasurer.measure("0", LabelStyle).size.height
    val barsBottom = size.height - labelGap - labelHeight
    val maxValue = points.maxOf { it.value }.toFloat()

    val slot = size.width / points.size
    val gap = min(12.dp.toPx(), slot * 0.3f)
    val barWidth = slot - gap + gap / points.size
    val radius = CornerRadius(6.dp.toPx())

    points.forEachIndexed { index, point ->
        val left = index * (barWidth + gap)
        val barHeight = barsBottom * point.value / maxValue * progress
        if (barHeight > 0f) {
            drawRoundRect(
                color = if (point.isCurrent) Primary else ChartInactive,
                topLeft = Offset(left, barsBottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
        val labelStyle = if (point.isCurrent) {
            LabelStyle.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
        } else {
            LabelStyle
        }
        val text = textMeasurer.measure(point.label, labelStyle)
        val x = (left + barWidth / 2 - text.size.width / 2f).coerceIn(0f, size.width - text.size.width)
        drawText(text, topLeft = Offset(x, barsBottom + labelGap))
    }
}

/** Прямоугольник со скруглёнными только верхними углами: столбец стоит на оси. */
private fun topRoundedPath(rect: Rect, radius: CornerRadius): Path {
    // Невысокий столбец: радиус не больше его высоты, иначе углы вывернутся
    val r = CornerRadius(min(radius.x, rect.height), min(radius.y, rect.height))
    return Path().apply {
        addRoundRect(
            RoundRect(rect, topLeft = r, topRight = r, bottomRight = CornerRadius.Zero, bottomLeft = CornerRadius.Zero),
        )
    }
}

/** Лучшая завершённая неделя (при равенстве — более поздняя) или -1. */
private fun bestIndex(points: List<ChartPoint>): Int {
    var best = -1
    points.forEachIndexed { index, point ->
        if (!point.isCurrent && point.value > 0 && (best == -1 || point.value >= points[best].value)) best = index
    }
    return best
}

/**
 * Деления оси Y: 0 и ещё около трёх «круглых» значений с шагом 1, 2, 5, 10, 20, 50...
 * Например, для максимума 14 — [0, 5, 10, 15].
 */
internal fun chartAxisTicks(maxValue: Int, intervals: Int = 3): List<Int> {
    if (maxValue <= 0) return listOf(0, 1)
    val rawStep = maxValue.toDouble() / intervals
    var magnitude = 1
    while (magnitude * 10 <= rawStep) magnitude *= 10
    val step = listOf(1, 2, 5, 10).map { it * magnitude }.first { it >= rawStep }
    val top = ceil(maxValue.toDouble() / step).toInt() * step
    return (0..top step step).toList()
}

/**
 * Какие столбцы подписать, чтобы подписи не налезали друг на друга:
 * все, если их не больше [maxLabels], иначе первый, последний и равномерно между ними.
 * Для 12 недель — [0, 4, 8, 11].
 */
internal fun chartLabelIndices(count: Int, maxLabels: Int = 4): List<Int> {
    if (count <= maxLabels) return (0 until count).toList()
    val last = count - 1
    val step = ceil(last.toDouble() / (maxLabels - 1)).toInt()
    val indices = (0 until last step step).toMutableList()
    // Предпоследняя подпись слишком близко к последней — убираем её
    if (indices.size > 1 && last - indices.last() < step / 2.0) indices.removeAt(indices.lastIndex)
    return indices + last
}

private val PreviewWeeks = listOf(
    "3 авг" to 9, "10 авг" to 13, "17 авг" to 9, "24 авг" to 13, "31 авг" to 14, "7 сен" to 9,
    "14 сен" to 13, "21 сен" to 9, "28 сен" to 13, "5 окт" to 8, "12 окт" to 14, "19 окт" to 5,
)

@Preview(name = "Статистика", widthDp = 360)
@Composable
private fun ProgressChartFullPreview() {
    FitnessDiaryTheme {
        Box(Modifier.background(Surface).padding(16.dp)) {
            ProgressChart(
                points = PreviewWeeks.mapIndexed { i, (label, value) ->
                    ChartPoint(label, value, isCurrent = i == PreviewWeeks.lastIndex)
                },
            )
        }
    }
}

@Preview(name = "Главная", widthDp = 360)
@Composable
private fun ProgressChartCompactPreview() {
    FitnessDiaryTheme {
        Box(Modifier.background(Surface).padding(16.dp)) {
            ProgressChart(
                points = PreviewWeeks.takeLast(6).mapIndexed { i, (label, value) ->
                    ChartPoint(label, value, isCurrent = i == 5)
                },
                style = ProgressChartStyle.Compact,
            )
        }
    }
}

@Preview(name = "Пусто", widthDp = 360)
@Composable
private fun ProgressChartEmptyPreview() {
    FitnessDiaryTheme {
        Column(
            Modifier.background(Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProgressChart(points = emptyList())
        }
    }
}
