package ru.pacani.fitnessdiary.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.components.ScreenStub
import ru.pacani.fitnessdiary.ui.components.StubButton

// Владелец: участник 1
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onOpenRegister: () -> Unit,
) {
    ScreenStub(title = stringResource(R.string.title_login)) {
        StubButton(R.string.stub_login, onClick = onLoggedIn)
        StubButton(R.string.stub_open_register, onClick = onOpenRegister)
    }
}
