package ru.pacani.fitnessdiary.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 1
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.title_register), onBack = onBack) {
        StubButton(R.string.stub_register, onClick = onRegistered)
        StubButton(R.string.stub_open_login, onClick = onBack)
    }
}
