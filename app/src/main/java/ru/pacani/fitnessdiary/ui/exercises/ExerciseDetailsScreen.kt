package ru.pacani.fitnessdiary.ui.exercises

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub

// Владелец: участник 2
@Composable
fun ExerciseDetailsScreen(
    exerciseId: Long,
    onBack: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.title_exercise_details), onBack = onBack) {
        Text(stringResource(R.string.stub_id, exerciseId))
    }
}
