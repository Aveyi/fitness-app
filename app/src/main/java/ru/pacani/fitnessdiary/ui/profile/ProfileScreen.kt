package ru.pacani.fitnessdiary.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.domain.model.User
import ru.pacani.fitnessdiary.ui.components.GoalPicker
import ru.pacani.fitnessdiary.ui.components.LabeledTextField
import ru.pacani.fitnessdiary.ui.components.PrimaryButton
import ru.pacani.fitnessdiary.ui.components.longDate
import ru.pacani.fitnessdiary.ui.theme.ButtonShape
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.Dimens
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_saved)

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }
    LaunchedEffect(state.showSavedMessage) {
        if (state.showSavedMessage) {
            viewModel.onSavedMessageShown()
            snackbarHostState.showSnackbar(savedMessage)
        }
    }

    ProfileContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onGoalChange = viewModel::onGoalChange,
        onSave = viewModel::onSave,
        onLogout = viewModel::onLogout,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGoalChange: (Goal) -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val user = state.user
        if (user == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        // Как на экране входа: колонка на весь экран, чтобы кнопка выхода стояла внизу
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileHeader(user = user, workoutCount = state.finishedWorkoutCount)
                ParametersCard(
                    form = state.form,
                    hasChanges = state.hasChanges,
                    isSaving = state.isSaving,
                    onHeightChange = onHeightChange,
                    onWeightChange = onWeightChange,
                    onGoalChange = onGoalChange,
                    onSave = onSave,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onLogout,
                    shape = ButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.profile_logout),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User, workoutCount: Int) {
    val since = longDate(user.createdAt.atZone(ZoneId.systemDefault()).toLocalDate())
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.name.firstOrNull()?.uppercase().orEmpty(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = user.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(user.email, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(
                R.string.profile_since,
                since,
                pluralStringResource(R.plurals.workout_count, workoutCount, workoutCount),
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ParametersCard(
    form: ProfileForm,
    hasChanges: Boolean,
    isSaving: Boolean,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGoalChange: (Goal) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_parameters),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    value = form.height,
                    onValueChange = onHeightChange,
                    label = stringResource(R.string.field_height),
                    error = form.heightError?.let { stringResource(it) },
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    value = form.weight,
                    onValueChange = onWeightChange,
                    label = stringResource(R.string.field_weight),
                    error = form.weightError?.let { stringResource(it) },
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f),
                )
            }
            GoalPicker(selected = form.goal, onSelect = onGoalChange, enabled = !isSaving)
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                isLoading = isSaving,
                enabled = hasChanges,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfilePreview() {
    val createdAt = LocalDate.of(2026, 8, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()
    FitnessDiaryTheme {
        ProfileContent(
            state = ProfileUiState(
                isLoading = false,
                user = User(1, "Иван", "ivan@mail.ru", 180, 78.0, Goal.STRENGTH, createdAt),
                finishedWorkoutCount = 29,
                form = ProfileForm(height = "180", weight = "78", goal = Goal.STRENGTH),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onHeightChange = {},
            onWeightChange = {},
            onGoalChange = {},
            onSave = {},
            onLogout = {},
            onBack = {},
        )
    }
}
