package ru.pacani.fitnessdiary.ui.workout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

/**
 * Владелец: участник 2.
 *
 * [pickedExerciseId] — упражнение, выбранное в каталоге (режим выбора).
 * Добавив его в тренировку, вызвать [onPickedExerciseHandled], иначе оно добавится повторно.
 */
@Composable
fun WorkoutScreen(
    workoutId: Long,
    pickedExerciseId: Long?,
    onPickedExerciseHandled: () -> Unit,
    onAddExercise: () -> Unit,
    onFinished: () -> Unit,
    onBack: () -> Unit,
) {
    // В заглушке просто показываем выбранные упражнения, чтобы проверить механику возврата
    val picked = remember { mutableStateListOf<Long>() }
    LaunchedEffect(pickedExerciseId) {
        if (pickedExerciseId != null) {
            picked += pickedExerciseId
            onPickedExerciseHandled()
        }
    }

    ScreenStub(title = stringResource(R.string.title_workout), onBack = onBack) {
        Text(stringResource(R.string.stub_id, workoutId))
        picked.forEach { Text(stringResource(R.string.stub_picked_exercise, it)) }
        StubButton(R.string.stub_add_exercise, onClick = onAddExercise)
        StubButton(R.string.stub_finish_workout, onClick = onFinished)
    }
}
