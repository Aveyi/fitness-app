package ru.pacani.fitnessdiary.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.stats.StatsSummary
import ru.pacani.fitnessdiary.domain.stats.WeekCount
import ru.pacani.fitnessdiary.ui.components.ChartPoint
import ru.pacani.fitnessdiary.ui.components.ProgressChart
import ru.pacani.fitnessdiary.ui.components.ProgressChartStyle
import ru.pacani.fitnessdiary.ui.components.StatTile
import ru.pacani.fitnessdiary.ui.components.durationText
import ru.pacani.fitnessdiary.ui.components.headerDate
import ru.pacani.fitnessdiary.ui.components.shortDate
import ru.pacani.fitnessdiary.ui.components.timeOfDay
import ru.pacani.fitnessdiary.ui.components.weekdayShort
import ru.pacani.fitnessdiary.ui.theme.ButtonShape
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.Dimens
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    onOpenWorkout: (workoutId: Long) -> Unit,
    onOpenWorkoutDetails: (workoutId: Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultTitle = stringResource(R.string.home_default_workout_title)

    LaunchedEffect(state.openWorkoutId) {
        state.openWorkoutId?.let { workoutId ->
            viewModel.onWorkoutOpened()
            onOpenWorkout(workoutId)
        }
    }

    HomeContent(
        state = state,
        onStartWorkout = { viewModel.onStartWorkout(defaultTitle) },
        onOpenWorkoutDetails = onOpenWorkoutDetails,
        onOpenStats = onOpenStats,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onStartWorkout: () -> Unit,
    onOpenWorkoutDetails: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Greeting(name = state.user?.name.orEmpty(), today = state.today, onOpenProfile = onOpenProfile)
        WorkoutCard(state = state, onStartWorkout = onStartWorkout)
        ThisWeek(summary = state.thisWeek)
        WeeklyChartCard(weeks = state.weeks, onClick = onOpenStats)
        state.lastWorkout?.let { workout ->
            LastWorkout(workout = workout, onClick = { onOpenWorkoutDetails(workout.id) })
        }
    }
}

@Composable
private fun Greeting(name: String, today: LocalDate, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = headerDate(today),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_greeting, name),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        val profileLabel = stringResource(R.string.title_profile)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable(role = Role.Button, onClickLabel = profileLabel, onClick = onOpenProfile)
                .semantics { contentDescription = profileLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase().orEmpty(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WorkoutCard(state: HomeUiState, onStartWorkout: () -> Unit) {
    val active = state.activeWorkout
    val title = if (active != null) R.string.home_active_title else R.string.home_ready_title
    val subtitle = when {
        active != null -> stringResource(R.string.home_active_subtitle, active.title, timeOfDay(active.startedAt))
        state.lastWorkout != null -> lastWorkoutText(state.lastWorkout, state.today)
        else -> stringResource(R.string.home_first_workout)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
            Button(
                onClick = onStartWorkout,
                enabled = !state.isStartingWorkout,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = Color.White,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
            ) {
                if (state.isStartingWorkout) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(if (active != null) R.string.home_continue else R.string.home_start),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** «Последняя была вчера: Грудь + трицепс». */
@Composable
private fun lastWorkoutText(workout: Workout, today: LocalDate): String {
    val date = workout.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(date, today).toInt().coerceAtLeast(0)
    return when (daysAgo) {
        0 -> stringResource(R.string.home_last_today, workout.title)
        1 -> stringResource(R.string.home_last_yesterday, workout.title)
        else -> pluralStringResource(R.plurals.home_last_days_ago, daysAgo, daysAgo, workout.title)
    }
}

@Composable
private fun ThisWeek(summary: StatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.home_this_week))
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
                value = durationText(summary.totalDuration.toMinutes()),
                label = stringResource(R.string.home_time_in_gym),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(weeks: List<WeekCount>, onClick: () -> Unit) {
    val points = weeks.mapIndexed { index, week ->
        ChartPoint(label = shortDate(week.weekStart), value = week.exerciseCount, isCurrent = index == weeks.lastIndex)
    }
    val description = stringResource(
        R.string.home_chart_description,
        weeks.size,
        weeks.lastOrNull()?.exerciseCount ?: 0,
    )
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_chart_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressChart(
                points = points,
                style = ProgressChartStyle.Compact,
                contentDescription = description,
            )
        }
    }
}

@Composable
private fun LastWorkout(workout: Workout, onClick: () -> Unit) {
    val date = workout.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
    val minutes = workout.duration?.toMinutes() ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.home_last_workout))
        Surface(
            onClick = onClick,
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(date.dayOfMonth.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                    Text(weekdayShort(date), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = workout.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_workout_summary,
                            pluralStringResource(R.plurals.exercise_count, workout.exerciseCount, workout.exerciseCount),
                            durationText(minutes),
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() },
    )
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun HomePreview() {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.of(2026, 10, 20)
    val lastStart = LocalDateTime.of(2026, 10, 19, 18, 0).atZone(zone).toInstant()
    FitnessDiaryTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                user = User(1, "Иван", "ivan@mail.ru", 180, 78.0, Goal.STRENGTH, Instant.EPOCH),
                today = today,
                lastWorkout = Workout(1, 1, "Грудь + трицепс", lastStart, lastStart.plus(Duration.ofMinutes(58)), null, 5, 15),
                thisWeek = StatsSummary(1, 5, Duration.ofMinutes(58)),
                weeks = listOf(13, 9, 13, 8, 14, 5).mapIndexed { i, count ->
                    WeekCount(LocalDate.of(2026, 9, 14).plusWeeks(i.toLong()), count)
                },
            ),
            onStartWorkout = {},
            onOpenWorkoutDetails = {},
            onOpenStats = {},
            onOpenProfile = {},
        )
    }
}
