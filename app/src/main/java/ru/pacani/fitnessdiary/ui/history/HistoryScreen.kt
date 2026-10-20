package ru.pacani.fitnessdiary.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 2
@Composable
fun HistoryScreen(
    onOpenWorkout: (workoutId: Long) -> Unit,
) {
    ScreenStub(title = stringResource(R.string.tab_history)) {
        StubButton(R.string.stub_open_workout, onClick = { onOpenWorkout(1L) })
    }
}
