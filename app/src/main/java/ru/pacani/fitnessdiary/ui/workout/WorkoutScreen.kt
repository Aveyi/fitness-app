package ru.pacani.fitnessdiary.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory
import ru.pacani.fitnessdiary.domain.model.Workout
import ru.pacani.fitnessdiary.domain.model.WorkoutDetails
import ru.pacani.fitnessdiary.domain.model.WorkoutExercise
import ru.pacani.fitnessdiary.domain.model.WorkoutSet
import ru.pacani.fitnessdiary.ui.components.SetRow
import ru.pacani.fitnessdiary.ui.components.SetRowHeader
import ru.pacani.fitnessdiary.ui.components.durationText
import ru.pacani.fitnessdiary.ui.theme.ButtonShape
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.ChartInactive
import ru.pacani.fitnessdiary.ui.theme.Dimens
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.PrimaryDark

/**
 * Экран «Тренировка»: активная тренировка или редактирование завершённой. Владелец: участник 2.
 *
 * [pickedExerciseId] — упражнение, выбранное в каталоге (режим выбора).
 * Добавив его в тренировку, вызываем [onPickedExerciseHandled], иначе оно добавится повторно.
 */
@Composable
fun WorkoutScreen(
    workoutId: Long,
    pickedExerciseId: Long?,
    onPickedExerciseHandled: () -> Unit,
    onAddExercise: () -> Unit,
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = viewModel(
        key = "workout_$workoutId",
        factory = WorkoutViewModel.factory(workoutId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pickedExerciseId) {
        if (pickedExerciseId != null) {
            viewModel.addExercise(pickedExerciseId)
            onPickedExerciseHandled()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WorkoutEvent.Finished -> onFinished()
            }
        }
    }

    WorkoutContent(
        state = state,
        actions = WorkoutActions(
            onBack = onBack,
            onRename = viewModel::rename,
            onAddExercise = onAddExercise,
            onRemoveExercise = viewModel::removeExercise,
            onAddSet = viewModel::addSet,
            onWeightChange = { set, weight -> viewModel.updateSet(set, weightKg = weight) },
            onRepsChange = { set, reps -> viewModel.updateSet(set, reps = reps) },
            onToggleSet = viewModel::toggleSet,
            onDeleteSet = viewModel::deleteSet,
            onAddRestTime = viewModel::addRestTime,
            onSkipRest = viewModel::skipRest,
            onFinish = viewModel::finish,
        ),
    )
}

/** Все действия экрана одним объектом, чтобы не тащить дюжину параметров через каждую функцию. */
private class WorkoutActions(
    val onBack: () -> Unit,
    val onRename: (String) -> Unit,
    val onAddExercise: () -> Unit,
    val onRemoveExercise: (workoutExerciseId: Long) -> Unit,
    val onAddSet: (workoutExerciseId: Long) -> Unit,
    val onWeightChange: (WorkoutSet, Double) -> Unit,
    val onRepsChange: (WorkoutSet, Int) -> Unit,
    val onToggleSet: (WorkoutSet) -> Unit,
    val onDeleteSet: (WorkoutSet) -> Unit,
    val onAddRestTime: () -> Unit,
    val onSkipRest: () -> Unit,
    val onFinish: (note: String?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutContent(
    state: WorkoutUiState,
    actions: WorkoutActions,
) {
    val details = state.details
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var exerciseToRemove by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (details != null) {
                        Column {
                            Text(
                                text = details.workout.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitle(state, details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(stringResource(R.string.title_workout))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (details != null) {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.workout_rename))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (details != null) {
                FinishBar(
                    isActive = state.isActive,
                    onClick = {
                        if (state.isActive) showFinishDialog = true else actions.onFinish(details.workout.note)
                    },
                )
            }
        },
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

            details == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.workout_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "rest") {
                    AnimatedVisibility(
                        visible = state.restSecondsLeft != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        RestTimerBanner(
                            secondsLeft = state.restSecondsLeft ?: 0,
                            onAddTime = actions.onAddRestTime,
                            onSkip = actions.onSkipRest,
                        )
                    }
                }
                if (details.exercises.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.workout_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                items(details.exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        actions = actions,
                        onRemoveClick = { exerciseToRemove = exercise.id },
                    )
                }
                item(key = "add") {
                    AddExerciseButton(onClick = actions.onAddExercise)
                }
            }
        }
    }

    if (showRenameDialog && details != null) {
        RenameDialog(
            currentTitle = details.workout.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { title ->
                actions.onRename(title)
                showRenameDialog = false
            },
        )
    }

    if (showFinishDialog && details != null) {
        FinishDialog(
            uncheckedSets = details.totalSetCount - details.completedSetCount,
            initialNote = details.workout.note.orEmpty(),
            onDismiss = { showFinishDialog = false },
            onConfirm = { note ->
                showFinishDialog = false
                actions.onFinish(note)
            },
        )
    }

    val removeId = exerciseToRemove
    val removeTarget = details?.exercises?.firstOrNull { it.id == removeId }
    if (removeTarget != null) {
        AlertDialog(
            onDismissRequest = { exerciseToRemove = null },
            title = { Text(stringResource(R.string.workout_remove_exercise_title, removeTarget.exercise.name)) },
            text = { Text(stringResource(R.string.workout_remove_exercise_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onRemoveExercise(removeTarget.id)
                        exerciseToRemove = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRemove = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** «Идёт 42 мин · 4 из 5 подходов» или «58 мин · 16 из 16 подходов» для завершённой. */
@Composable
private fun subtitle(state: WorkoutUiState, details: WorkoutDetails): String {
    val duration = durationText(state.elapsedMinutes)
    val time = if (state.isActive) stringResource(R.string.workout_in_progress, duration) else duration
    val total = details.totalSetCount
    if (total == 0) return time
    val sets = pluralStringResource(R.plurals.workout_sets_progress, total, details.completedSetCount, total)
    return "$time · $sets"
}

@Composable
private fun RestTimerBanner(
    secondsLeft: Int,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryDark, CardShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.workout_rest),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedButton(
            onClick = onAddTime,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.workout_rest_add))
        }
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryDark),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.workout_rest_skip), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    actions: WorkoutActions,
    onRemoveClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.exercise.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.workout_remove_exercise),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SetRowHeader()
                exercise.sets.forEachIndexed { index, set ->
                    key(set.id) {
                        SetRow(
                            number = index + 1,
                            weightKg = set.weightKg,
                            reps = set.reps,
                            isCompleted = set.isCompleted,
                            onWeightChange = { actions.onWeightChange(set, it) },
                            onRepsChange = { actions.onRepsChange(set, it) },
                            onToggle = { actions.onToggleSet(set) },
                            onDelete = { actions.onDeleteSet(set) },
                        )
                    }
                }
            }
            TextButton(
                onClick = { actions.onAddSet(exercise.id) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.workout_add_set),
                    modifier = Modifier.padding(start = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Кнопка с пунктирной рамкой, как в макете. */
@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = ChartInactive,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = PrimaryDark)
        Text(
            text = stringResource(R.string.workout_add_exercise),
            modifier = Modifier.padding(start = 8.dp),
            color = PrimaryDark,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FinishBar(isActive: Boolean, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = OutlineVariant)
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(Dimens.ButtonHeight),
                shape = ButtonShape,
            ) {
                Text(
                    text = stringResource(if (isActive) R.string.workout_finish else R.string.workout_done),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_rename_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun FinishDialog(
    uncheckedSets: Int,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String?) -> Unit,
) {
    var note by rememberSaveable { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_finish_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uncheckedSets > 0) {
                    Text(stringResource(R.string.workout_finish_unchecked, uncheckedSets))
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text(stringResource(R.string.workout_note)) },
                    minLines = 2,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) { Text(stringResource(R.string.workout_finish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ---------------------------------------------------------------------------------------------

private fun previewExercise(id: Long, name: String, sets: List<WorkoutSet>) = WorkoutExercise(
    id = id,
    exercise = Exercise(id, null, name, null, ExerciseCategory.CHEST, null, false),
    orderIndex = id.toInt(),
    sets = sets,
)

private val previewDetails = WorkoutDetails(
    workout = Workout(1, 1, "Грудь + трицепс", Instant.EPOCH, null, null),
    exercises = listOf(
        previewExercise(
            1,
            "Жим штанги лёжа",
            listOf(
                WorkoutSet(1, 0, 80.0, 8, true),
                WorkoutSet(2, 1, 80.0, 8, true),
                WorkoutSet(3, 2, 82.5, 6, false),
            ),
        ),
        previewExercise(
            2,
            "Французский жим",
            listOf(WorkoutSet(4, 0, 30.0, 10, true), WorkoutSet(5, 1, 30.0, 10, true)),
        ),
    ),
)

private val previewActions = WorkoutActions(
    onBack = {}, onRename = {}, onAddExercise = {}, onRemoveExercise = {}, onAddSet = {},
    onWeightChange = { _, _ -> }, onRepsChange = { _, _ -> }, onToggleSet = {}, onDeleteSet = {},
    onAddRestTime = {}, onSkipRest = {}, onFinish = {},
)

@Preview(name = "Тренировка идёт", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WorkoutPreview() {
    FitnessDiaryTheme {
        WorkoutContent(
            state = WorkoutUiState(
                isLoading = false,
                details = previewDetails,
                elapsedMinutes = 42,
                restSecondsLeft = 72,
            ),
            actions = previewActions,
        )
    }
}

@Preview(name = "Пустая тренировка", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun WorkoutEmptyPreview() {
    FitnessDiaryTheme {
        WorkoutContent(
            state = WorkoutUiState(
                isLoading = false,
                details = previewDetails.copy(exercises = emptyList()),
            ),
            actions = previewActions,
        )
    }
}
