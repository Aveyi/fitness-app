package ru.pacani.fitnessdiary.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.domain.model.Goal
import ru.pacani.fitnessdiary.ui.theme.ChipShape
import ru.pacani.fitnessdiary.ui.theme.FieldLabel
import ru.pacani.fitnessdiary.ui.theme.Outline

/** Выбор цели тренировок чипами: Регистрация и Профиль. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalPicker(
    selected: Goal,
    onSelect: (Goal) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.field_goal),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = FieldLabel,
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Goal.entries.forEach { goal ->
                val isSelected = goal == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(goal) },
                    enabled = enabled,
                    label = { Text(stringResource(goal.labelRes()), fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                    shape = ChipShape,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = enabled,
                        selected = isSelected,
                        borderColor = Outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderWidth = 1.dp,
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = FieldLabel,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    // Выбор одного варианта из нескольких: TalkBack озвучит как переключатель
                    modifier = Modifier
                        .height(36.dp)
                        .semantics { role = Role.RadioButton },
                )
            }
        }
    }
}

/** Название цели для чипов Регистрации и Профиля. */
@StringRes
fun Goal.labelRes(): Int = when (this) {
    Goal.STRENGTH -> R.string.goal_strength
    Goal.MASS -> R.string.goal_mass
    Goal.WEIGHT_LOSS -> R.string.goal_weight_loss
    Goal.MAINTENANCE -> R.string.goal_maintenance
}
