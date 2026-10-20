package ru.pacani.fitnessdiary.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory
import ru.pacani.fitnessdiary.ui.theme.Background
import ru.pacani.fitnessdiary.ui.theme.CardShape
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme
import ru.pacani.fitnessdiary.ui.theme.OutlineVariant
import ru.pacani.fitnessdiary.ui.theme.Primary
import ru.pacani.fitnessdiary.ui.theme.PrimaryContainer

/** Детали упражнения. Владелец: участник 2. */
@Composable
fun ExerciseDetailsScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    viewModel: ExerciseDetailsViewModel = viewModel(
        key = "exercise_$exerciseId",
        factory = ExerciseDetailsViewModel.factory(exerciseId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseDetailsContent(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailsContent(
    state: ExerciseDetailsUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_exercise_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                ExerciseDetailsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                ExerciseDetailsUiState.NotFound -> Text(
                    text = stringResource(R.string.exercise_details_not_found),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is ExerciseDetailsUiState.Loaded -> ExerciseDetailsBody(state.exercise)
            }
        }
    }
}

@Composable
private fun ExerciseDetailsBody(exercise: Exercise) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(PrimaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Primary)
                }
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoBox(
                    label = stringResource(R.string.exercise_details_category),
                    value = stringResource(exercise.category.labelRes()),
                    modifier = Modifier.weight(1f),
                )
                InfoBox(
                    label = stringResource(R.string.exercise_details_equipment),
                    value = exercise.equipment ?: stringResource(R.string.exercise_details_no_equipment),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DetailsCard {
            Text(
                text = stringResource(R.string.exercise_details_technique),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val lines = exercise.description?.lines()?.filter { it.isNotBlank() }.orEmpty()
            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.exercise_details_no_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lines.forEach { line ->
                        Text(text = line, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Text(
            text = stringResource(
                if (exercise.remoteId != null) {
                    R.string.exercise_details_source_wger
                } else {
                    R.string.exercise_details_source_custom
                },
            ),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Background, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "Детали упражнения", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ExerciseDetailsPreview() {
    FitnessDiaryTheme {
        ExerciseDetailsContent(
            state = ExerciseDetailsUiState.Loaded(
                Exercise(
                    id = 1,
                    remoteId = 192,
                    name = "Жим штанги лёжа",
                    description = "• Лягте на скамью, глаза под грифом. Стопы плотно упираются в пол, лопатки сведены.\n" +
                        "• Возьмите гриф хватом чуть шире плеч и снимите его со стоек на прямые руки.\n" +
                        "• На вдохе медленно опустите штангу к нижней части груди.\n" +
                        "• На выдохе выжмите штангу вверх до выпрямления рук.",
                    category = ExerciseCategory.CHEST,
                    equipment = "Штанга, скамья",
                    isCustom = false,
                ),
            ),
            onBack = {},
        )
    }
}
