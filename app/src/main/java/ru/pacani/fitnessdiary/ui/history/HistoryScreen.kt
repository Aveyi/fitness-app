package ru.pacani.fitnessdiary.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.ui.components.durationText
import ru.pacani.fitnessdiary.ui.theme.Background
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.PrimaryContainer
import ru.pacani.fitnessdiary.ui.theme.PrimaryDark

/** История завершённых тренировок. Владелец: участник 2. */
@Composable
fun HistoryScreen(
    onOpenWorkout: (workoutId: Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    HistoryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onOpenWorkout = onOpenWorkout,
        onDelete = { workoutId ->
            viewModel.requestDelete(workoutId)
            scope.launch {
                var undone = false
                try {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.history_deleted),
                        actionLabel = context.getString(R.string.history_undo),
                        duration = SnackbarDuration.Short,
                    )
                    undone = result == SnackbarResult.ActionPerformed
                } finally {
                    // finally: если ушли с экрана, пока висел Snackbar, удаление всё равно подтверждается
                    if (undone) viewModel.undoDelete(workoutId) else viewModel.confirmDelete(workoutId)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onOpenWorkout: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_history)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.isEmpty -> EmptyHistory(Modifier.padding(innerPadding))

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "header_${group.workouts.first().id}") {
                        Text(
                            text = weekLabelText(group.label, state.today),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(group.workouts, key = { it.id }) { workout ->
                        SwipeToDelete(onDelete = { onDelete(workout.id) }) {
                            WorkoutCard(
                                workout = workout,
                                zone = state.zone,
                                highlighted = group.label == WeekLabel.ThisWeek,
                                onClick = { onOpenWorkout(workout.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun weekLabelText(label: WeekLabel, today: LocalDate): String = when (label) {
    WeekLabel.ThisWeek -> stringResource(R.string.history_this_week)
    WeekLabel.LastWeek -> stringResource(R.string.history_last_week)
    is WeekLabel.Range -> formatWeekRange(label.start, label.end, today)
}

/** Свайп справа налево удаляет карточку. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        // Свайп вправо выключен, так что сюда приходит только EndToStart; проверка — на всякий случай.
        // Карточка сразу скрывается из списка, а при «Отменить» возвращается с новым, несдвинутым состоянием.
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, CardShape)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.White,
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    zone: ZoneId,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val date = workout.startedAt.atZone(zone).toLocalDate()
    val minutes = workout.duration?.toMinutes() ?: 0L
    val subtitle = pluralStringResource(R.plurals.exercises_count, workout.exerciseCount, workout.exerciseCount) +
        " · " + durationText(minutes)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DateBlock(date = date, highlighted = highlighted)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Блок «19 / пн»: синий для текущей недели, серый для остальных. */
@Composable
fun DateBlock(date: LocalDate, highlighted: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(56.dp)
            .background(if (highlighted) PrimaryContainer else Background, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) PrimaryDark else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = formatWeekdayShort(date),
            fontSize = 12.sp,
            color = if (highlighted) PrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_empty_text),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------------------------

@Preview(name = "История", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HistoryPreview() {
    val zone = ZoneId.of("Europe/Moscow")
    val today = LocalDate.of(2026, 10, 20)
    fun workout(id: Long, title: String, day: Int, month: Int, minutes: Long, exercises: Int): Workout {
        val start = LocalDate.of(2026, month, day).atTime(18, 30).atZone(zone).toInstant()
        return Workout(id, 1, title, start, start.plus(Duration.ofMinutes(minutes)), null, exercises, exercises * 3)
    }
    val workouts = listOf(
        workout(1, "Грудь + трицепс", 19, 10, 58, 5),
        workout(2, "Спина + бицепс", 17, 10, 64, 5),
        workout(3, "Ноги", 15, 10, 72, 4),
        workout(4, "Спина", 10, 10, 49, 4),
        workout(5, "Ноги", 8, 10, 68, 4),
    )
    FitnessDiaryTheme {
        HistoryContent(
            state = HistoryUiState(isLoading = false, groups = groupByWeek(workouts, today, zone), today = today, zone = zone),
            snackbarHostState = remember { SnackbarHostState() },
            onOpenWorkout = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Пустая история", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun HistoryEmptyPreview() {
    FitnessDiaryTheme {
        HistoryContent(
            state = HistoryUiState(isLoading = false, today = LocalDate.of(2026, 10, 20), zone = ZoneId.systemDefault()),
            snackbarHostState = remember { SnackbarHostState() },
            onOpenWorkout = {},
            onDelete = {},
        )
    }
}

