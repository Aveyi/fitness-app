package ru.pacani.fitnessdiary.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory
import ru.pacani.fitnessdiary.ui.theme.ChipShape
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.GridLine
import ru.pacani.fitnessdiary.ui.theme.Outline
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.Primary
import ru.pacani.fitnessdiary.ui.theme.PrimaryContainer
import ru.pacani.fitnessdiary.ui.theme.PrimaryDark

/**
 * Каталог упражнений. Владелец: участник 2.
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
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    ExercisesContent(
        state = state,
        pickMode = pickMode,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onRefresh = viewModel::refresh,
        onExerciseClick = { id -> if (pickMode) onExercisePicked(id) else onOpenExercise(id) },
        onAddCustomClick = { showCustomDialog = true },
        onBack = onBack,
    )

    if (showCustomDialog) {
        CustomExerciseDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, category ->
                viewModel.addCustomExercise(name, category)
                showCustomDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisesContent(
    state: ExercisesUiState,
    pickMode: Boolean,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (ExerciseCategory?) -> Unit,
    onRefresh: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onAddCustomClick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (pickMode) R.string.title_exercise_pick else R.string.tab_exercises))
                },
                navigationIcon = {
                    if (pickMode) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomClick,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.exercises_add_custom)) },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            CategoryChips(
                selected = state.selectedCategory,
                onSelected = onCategorySelected,
            )
            HorizontalDivider(color = OutlineVariant)

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Чтобы последний элемент не прятался под плавающей кнопкой
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        if (state.exercises.isEmpty()) {
                            item {
                                // Внутри LazyColumn, чтобы pull-to-refresh работал и на пустом списке
                                EmptyState(
                                    text = stringResource(
                                        if (state.query.isNotBlank() || state.selectedCategory != null) {
                                            R.string.exercises_empty_search
                                        } else {
                                            R.string.exercises_empty_catalog
                                        },
                                    ),
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                        items(state.exercises, key = { it.id }) { exercise ->
                            ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                            HorizontalDivider(color = OutlineVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Поле поиска в виде «таблетки», как в макете. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.exercises_search_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.exercises_search_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GridLine,
            unfocusedContainerColor = GridLine,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CategoryChips(
    selected: ExerciseCategory?,
    onSelected: (ExerciseCategory?) -> Unit,
) {
    val options: List<ExerciseCategory?> = listOf(null) + ExerciseCategory.filterable
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it?.key ?: "all" }) { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(category) },
                label = { Text(stringResource(category?.labelRes() ?: R.string.category_all)) },
                shape = ChipShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryContainer,
                    selectedLabelColor = PrimaryDark,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Outline,
                    selectedBorderColor = Primary,
                ),
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    val categoryLabel = stringResource(exercise.category.labelRes())
    val subtitle = listOfNotNull(categoryLabel, exercise.equipment).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = exercise.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (exercise.isCustom) CustomBadge()
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CustomBadge() {
    Text(
        text = stringResource(R.string.exercises_custom_badge),
        modifier = Modifier
            .background(PrimaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = PrimaryDark,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Диалог «Своё упражнение»: название и категория. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: ExerciseCategory) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ExerciseCategory.CHEST) }
    val isValid = name.trim().length in 2..60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_exercise_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.custom_exercise_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.custom_exercise_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExerciseCategory.filterable.forEach { option ->
                        FilterChip(
                            selected = option == category,
                            onClick = { category = option },
                            label = { Text(stringResource(option.labelRes())) },
                            shape = ChipShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryContainer,
                                selectedLabelColor = PrimaryDark,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), category) }, enabled = isValid) {
                Text(stringResource(R.string.custom_exercise_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------

private val previewExercises = listOf(
    Exercise(1, 192, "Жим штанги лёжа", null, ExerciseCategory.CHEST, "Штанга, скамья", false),
    Exercise(2, 111, "Приседания со штангой", null, ExerciseCategory.LEGS, "Штанга", false),
    Exercise(3, 184, "Становая тяга", null, ExerciseCategory.BACK, "Штанга", false),
    Exercise(4, 107, "Подтягивания", null, ExerciseCategory.BACK, "Турник", false),
    Exercise(5, null, "Жим в тренажёре Смита", null, ExerciseCategory.CHEST, null, true),
    Exercise(6, 91, "Скручивания", null, ExerciseCategory.ABS, "Без оборудования", false),
)

@Preview(name = "Каталог", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ExercisesPreview() {
    FitnessDiaryTheme {
        ExercisesContent(
            state = ExercisesUiState(exercises = previewExercises, isLoading = false),
            pickMode = false,
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {}, onCategorySelected = {}, onRefresh = {},
            onExerciseClick = {}, onAddCustomClick = {}, onBack = {},
        )
    }
}

@Preview(name = "Режим выбора, ничего не найдено", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ExercisesPickEmptyPreview() {
    FitnessDiaryTheme {
        ExercisesContent(
            state = ExercisesUiState(query = "абв", isLoading = false),
            pickMode = true,
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {}, onCategorySelected = {}, onRefresh = {},
            onExerciseClick = {}, onAddCustomClick = {}, onBack = {},
        )
    }
}

@Preview(name = "Диалог своего упражнения", showBackground = true)
@Composable
private fun CustomExerciseDialogPreview() {
    FitnessDiaryTheme {
        CustomExerciseDialog(onDismiss = {}, onConfirm = { _, _ -> })
    }
}
