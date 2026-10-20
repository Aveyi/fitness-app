package ru.pacani.fitnessdiary.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.ui.components.GoalPicker
import ru.pacani.fitnessdiary.ui.components.LabeledTextField
import ru.pacani.fitnessdiary.ui.components.PasswordStrengthIndicator
import ru.pacani.fitnessdiary.ui.components.PasswordTextField
import ru.pacani.fitnessdiary.ui.components.PrimaryButton
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegistered()
    }

    RegisterContent(
        state = state,
        actions = RegisterActions(
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onPasswordRepeatChange = viewModel::onPasswordRepeatChange,
            onHeightChange = viewModel::onHeightChange,
            onWeightChange = viewModel::onWeightChange,
            onGoalChange = viewModel::onGoalChange,
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onSubmit = viewModel::onSubmit,
            onBack = onBack,
        ),
    )
}

/** Все события формы одним объектом, чтобы не тащить десяток лямбд через параметры. */
private class RegisterActions(
    val onNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onPasswordRepeatChange: (String) -> Unit,
    val onHeightChange: (String) -> Unit,
    val onWeightChange: (String) -> Unit,
    val onGoalChange: (Goal) -> Unit,
    val onTogglePasswordVisibility: () -> Unit,
    val onSubmit: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterContent(state: RegisterUiState, actions: RegisterActions) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onSubmit()
    }
    val enabled = !state.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_register)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, enabled = enabled) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LabeledTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = stringResource(R.string.field_name),
                error = state.nameError?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.field_email),
                error = state.emailError?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordTextField(
                    value = state.password,
                    onValueChange = actions.onPasswordChange,
                    label = stringResource(R.string.field_password),
                    isPasswordVisible = state.isPasswordVisible,
                    onTogglePasswordVisibility = actions.onTogglePasswordVisibility,
                    error = state.passwordError?.let { stringResource(it) },
                    enabled = enabled,
                    imeAction = ImeAction.Next,
                )
                PasswordStrengthIndicator(state.passwordStrength)
            }
            PasswordTextField(
                value = state.passwordRepeat,
                onValueChange = actions.onPasswordRepeatChange,
                label = stringResource(R.string.field_password_repeat),
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = actions.onTogglePasswordVisibility,
                error = state.passwordRepeatError?.let { stringResource(it) },
                enabled = enabled,
                imeAction = ImeAction.Next,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    value = state.height,
                    onValueChange = actions.onHeightChange,
                    label = stringResource(R.string.field_height),
                    error = state.heightError?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    value = state.weight,
                    onValueChange = actions.onWeightChange,
                    label = stringResource(R.string.field_weight),
                    error = state.weightError?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f),
                )
            }
            GoalPicker(selected = state.goal, onSelect = actions.onGoalChange, enabled = enabled)

            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = stringResource(R.string.register_button),
                onClick = submit,
                isLoading = state.isLoading,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun RegisterPreview() {
    FitnessDiaryTheme {
        RegisterContent(
            state = RegisterUiState(
                name = "Иван",
                email = "ivan@mail.ru",
                password = "abcdefg1",
                passwordRepeat = "abcdefg2",
                passwordRepeatError = R.string.error_passwords_do_not_match,
                height = "180",
                weight = "78",
            ),
            actions = RegisterActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
