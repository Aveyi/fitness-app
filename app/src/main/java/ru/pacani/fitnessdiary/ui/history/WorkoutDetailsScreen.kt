package ru.pacani.fitnessdiary.ui.history

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 2
@Composable
fun WorkoutDetailsScreen(
    workoutId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.title_workout_details), onBack = onBack) {
        Text(stringResource(R.string.stub_id, workoutId))
        StubButton(R.string.stub_edit_workout, onClick = onEdit)
    }
}
