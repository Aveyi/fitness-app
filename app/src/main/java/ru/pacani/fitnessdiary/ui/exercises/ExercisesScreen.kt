package ru.pacani.fitnessdiary.ui.exercises

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

/**
 * Владелец: участник 2.
 *
 * В режиме выбора ([pickMode]) нажатие на упражнение вызывает [onExercisePicked]:
 * навигация вернёт id на экран Тренировка. Вне режима выбора — [onOpenExercise].
 */
@Composable
fun ExercisesScreen(
    pickMode: Boolean,
    onOpenExercise: (exerciseId: Long) -> Unit,
    onExercisePicked: (exerciseId: Long) -> Unit,
    onBack: () -> Unit,
) {
    if (pickMode) {
        ScreenStub(title = stringResource(R.string.title_exercise_pick), onBack = onBack) {
            StubButton(R.string.stub_pick_exercise, onClick = { onExercisePicked(1L) })
        }
    } else {
        ScreenStub(title = stringResource(R.string.tab_exercises)) {
            StubButton(R.string.stub_open_exercise, onClick = { onOpenExercise(1L) })
        }
    }
}
