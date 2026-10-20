package ru.pacani.fitnessdiary.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 1
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.title_profile), onBack = onBack) {
        StubButton(R.string.stub_logout, onClick = onLoggedOut)
    }
}
