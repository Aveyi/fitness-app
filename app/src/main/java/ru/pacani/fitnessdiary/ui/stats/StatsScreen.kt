package ru.pacani.fitnessdiary.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.stats.PersonalRecord
import ru.pacani.fitnessdiary.domain.stats.StatsPeriod
import ru.pacani.fitnessdiary.domain.stats.StatsSummary
import ru.pacani.fitnessdiary.domain.stats.WeekCount
import ru.pacani.fitnessdiary.ui.components.ChartPoint
import ru.pacani.fitnessdiary.ui.components.ProgressChart
import ru.pacani.fitnessdiary.ui.components.StatTile
import ru.pacani.fitnessdiary.ui.components.dayMonth
import ru.pacani.fitnessdiary.ui.components.formatWeightKg
import ru.pacani.fitnessdiary.ui.components.shortDate
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.GridLine
import ru.pacani.fitnessdiary.ui.theme.OnWarningContainer
import ru.pacani.fitnessdiary.ui.theme.WarningContainer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** 4.4 → «4,4», 4.0 → «4». */
private val OneDecimal = DecimalFormat("0.#", DecimalFormatSymbols(Locale.forLanguageTag("ru")))

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(state = state, onPeriodChange = viewModel::onPeriodChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsContent(state: StatsUiState, onPeriodChange: (StatsPeriod) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_stats)) }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodSelector(selected = state.period, onSelect = onPeriodChange)
            if (state.isEmpty) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 48.dp),
                )
                return@Column
            }
            SummaryTiles(state.summary)
            WeeklyChartCard(state)
            Records(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val periods = StatsPeriod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                // В макете без галочки: выбранный сегмент виден по заливке
                icon = {},
                modifier = Modifier.height(44.dp),
            ) {
                Text(stringResource(period.labelRes()), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun StatsPeriod.labelRes(): Int = when (this) {
    StatsPeriod.FOUR_WEEKS -> R.string.stats_period_four_weeks
    StatsPeriod.THREE_MONTHS -> R.string.stats_period_three_months
    StatsPeriod.ALL_TIME -> R.string.stats_period_all_time
}

@Composable
private fun SummaryTiles(summary: StatsSummary) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            value = summary.workoutCount.toString(),
            label = pluralStringResource(R.plurals.stat_workouts_label, summary.workoutCount),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            value = summary.exerciseCount.toString(),
            label = pluralStringResource(R.plurals.stat_exercises_label, summary.exerciseCount),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            value = OneDecimal.format(summary.averageExercisesPerWorkout),
            label = stringResource(R.string.stats_average_label),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun WeeklyChartCard(state: StatsUiState) {
    // От 10 — целым, как «≈ 11» в макете; меньше — с десятыми, иначе 0,1 в неделю показалось бы нулём
    val average = state.averageExercisesPerWeek
    val perWeek = if (average >= 10) Math.round(average).toString() else OneDecimal.format(average)
    val points = state.weeks.mapIndexed { index, week ->
        ChartPoint(shortDate(week.weekStart), week.exerciseCount, isCurrent = index == state.weeks.lastIndex)
    }
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.home_chart_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.stats_per_week, perWeek),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressChart(
                points = points,
                contentDescription = stringResource(R.string.stats_chart_description, perWeek),
            )
            val best = state.bestWeek
            val caption = buildString {
                if (best != null) {
                    append(
                        stringResource(
                            R.string.stats_best_week,
                            dayMonth(best.weekStart),
                            pluralStringResource(R.plurals.exercise_count, best.exerciseCount, best.exerciseCount),
                        ),
                    )
                    append(' ')
                }
                append(stringResource(R.string.stats_current_week_hint))
            }
            Text(caption, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Records(state: StatsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stats_records),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                .semantics { heading() },
        )
        if (state.records.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_records_empty),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            return@Column
        }
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                state.records.forEachIndexed { index, record ->
                    if (index > 0) HorizontalDivider(color = GridLine)
                    RecordRow(record = record, isNew = state.isNew(record))
                }
            }
        }
    }
}

@Composable
private fun RecordRow(record: PersonalRecord, isNew: Boolean) {
    val date = dayMonth(record.achievedAt.atZone(ZoneId.systemDefault()).toLocalDate())
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isNew) WarningContainer else MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = if (isNew) OnWarningContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = record.exerciseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isNew) stringResource(R.string.stats_record_new, date) else date,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.stats_weight_kg, formatWeightKg(record.weightKg)),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun StatsPreview() {
    val zone = ZoneId.systemDefault()
    val thisWeek = LocalDate.of(2026, 10, 19)
    val counts = listOf(9, 13, 9, 13, 14, 9, 13, 9, 13, 8, 14, 5)
    FitnessDiaryTheme {
        StatsContent(
            state = StatsUiState(
                isLoading = false,
                summary = StatsSummary(29, 129, Duration.ofHours(30)),
                weeks = counts.mapIndexed { i, c -> WeekCount(thisWeek.minusWeeks(11L - i), c) },
                bestWeek = WeekCount(LocalDate.of(2026, 10, 12), 14),
                averageExercisesPerWeek = 10.75,
                records = listOf(
                    PersonalRecord(1, "Жим штанги лёжа", 85.0, thisWeek.atStartOfDay(zone).toInstant()),
                    PersonalRecord(2, "Приседания со штангой", 110.0, LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant()),
                ),
                newRecordsSince = thisWeek.atStartOfDay(zone).toInstant(),
            ),
            onPeriodChange = {},
        )
    }
}
