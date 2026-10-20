package ru.pacani.fitnessdiary.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 1
@Composable
fun HomeScreen(
    onOpenWorkout: (workoutId: Long) -> Unit,
    onOpenWorkoutDetails: (workoutId: Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.tab_home)) {
        StubButton(R.string.stub_start_workout, onClick = { onOpenWorkout(1L) })
        StubButton(R.string.stub_open_last_workout, onClick = { onOpenWorkoutDetails(1L) })
        StubButton(R.string.stub_open_stats, onClick = onOpenStats)
        StubButton(R.string.stub_open_profile, onClick = onOpenProfile)
    }
}
