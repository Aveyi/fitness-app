package ru.pacani.fitnessdiary.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.model.WorkoutExercise
import ru.pacani.fitnessdiary.domain.model.WorkoutSet
import ru.pacani.fitnessdiary.ui.components.durationText
import ru.pacani.fitnessdiary.ui.components.formatWeightKg
import ru.pacani.fitnessdiary.ui.theme.Background
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.GridLine
import ru.pacani.fitnessdiary.ui.theme.OnWarningContainer
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.WarningContainer

/** Детали завершённой тренировки. Владелец: участник 2. */
@Composable
fun WorkoutDetailsScreen(
    workoutId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutDetailsViewModel = viewModel(
        key = "workout_details_$workoutId",
        factory = WorkoutDetailsViewModel.factory(workoutId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WorkoutDetailsEvent.Deleted -> onBack()
            }
        }
    }

    WorkoutDetailsContent(
        state = state,
        onEdit = onEdit,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetailsContent(
    state: WorkoutDetailsUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_workout)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state is WorkoutDetailsUiState.Loaded) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.details_edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                stringResource(R.string.details_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                WorkoutDetailsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                WorkoutDetailsUiState.NotFound -> Text(
                    text = stringResource(R.string.workout_not_found),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is WorkoutDetailsUiState.Loaded -> DetailsBody(state)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.details_delete_title)) },
            text = { Text(stringResource(R.string.details_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun DetailsBody(state: WorkoutDetailsUiState.Loaded) {
    val workout = state.details.workout
    val zone = ZoneId.systemDefault()
    val minutes = workout.duration?.toMinutes() ?: 0L
    val exercisesDone = state.details.completedExerciseCount
    val setsDone = state.details.completedSetCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailsCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column {
                    Text(text = workout.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatWorkoutDateLine(
                            start = workout.startedAt.atZone(zone),
                            end = workout.finishedAt?.atZone(zone),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        value = durationText(minutes),
                        label = stringResource(R.string.details_duration),
                        modifier = Modifier.weight(1f),
                    )
                    StatBox(
                        value = exercisesDone.toString(),
                        label = pluralStringResource(R.plurals.details_exercises_label, exercisesDone),
                        modifier = Modifier.weight(1f),
                    )
                    StatBox(
                        value = setsDone.toString(),
                        label = pluralStringResource(R.plurals.details_sets_label, setsDone),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.exercises.isNotEmpty()) {
            DetailsCard {
                Column {
                    state.exercises.forEachIndexed { index, summary ->
                        if (index > 0) HorizontalDivider(color = GridLine)
                        ExerciseBlock(summary)
                    }
                }
            }
        }

        val note = workout.note
        if (!note.isNullOrBlank()) {
            DetailsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.details_note),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = note, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseBlock(summary: ExerciseSummary) {
    val completedSets = summary.exercise.sets.filter { it.isCompleted }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = summary.exercise.exercise.name,
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            summary.recordWeightKg?.let { weight ->
                Text(
                    text = stringResource(R.string.details_record, formatWeightKg(weight)),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(WarningContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = OnWarningContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (completedSets.isEmpty()) {
            Text(
                text = stringResource(R.string.details_no_completed_sets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                completedSets.forEach { set -> SetChip(set) }
            }
        }
    }
}

/** «80 × 8»; для упражнений без веса — «12 повт.». */
@Composable
private fun SetChip(set: WorkoutSet) {
    val text = if (set.weightKg > 0.0) {
        stringResource(R.string.details_set_chip, formatWeightKg(set.weightKg), set.reps)
    } else {
        stringResource(R.string.details_set_chip_bodyweight, set.reps)
    }
    Text(
        text = text,
        modifier = Modifier
            .background(Background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Background, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------------------------

@Preview(name = "Детали тренировки", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WorkoutDetailsPreview() {
    val start = LocalDateTime.of(2026, 10, 19, 18, 30).atZone(ZoneId.systemDefault()).toInstant()
    fun sets(vararg pairs: Pair<Double, Int>) =
        pairs.mapIndexed { i, (w, r) -> WorkoutSet(i.toLong(), i, w, r, true) }
    fun exercise(id: Long, name: String, sets: List<WorkoutSet>) = WorkoutExercise(
        id = id,
        exercise = Exercise(id, null, name, null, ExerciseCategory.CHEST, null, false),
        orderIndex = id.toInt(),
        sets = sets,
    )
    val exercises = listOf(
        exercise(1, "Жим штанги лёжа", sets(80.0 to 8, 80.0 to 8, 82.5 to 6, 85.0 to 3)),
        exercise(2, "Жим гантелей на наклонной скамье", sets(34.0 to 10, 34.0 to 10, 34.0 to 10)),
        exercise(3, "Французский жим", sets(40.0 to 10, 40.0 to 10, 40.0 to 10)),
        exercise(4, "Скручивания", sets(0.0 to 20, 0.0 to 20)),
    )
    val details = WorkoutDetails(
        workout = Workout(
            id = 1,
            userId = 1,
            title = "Грудь + трицепс",
            startedAt = start,
            finishedAt = start.plus(Duration.ofMinutes(58)),
            note = "Последний подход жима тяжело. В следующий раз 82,5 кг на 3 подхода.",
        ),
        exercises = exercises,
    )
    FitnessDiaryTheme {
        WorkoutDetailsContent(
            state = WorkoutDetailsUiState.Loaded(
                details = details,
                exercises = exercises.mapIndexed { i, ex -> ExerciseSummary(ex, if (i == 0) 85.0 else null) },
            ),
            onEdit = {},
            onDelete = {},
            onBack = {},
        )
    }
}
