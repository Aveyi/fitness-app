package ru.pacani.fitnessdiary.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.pacani.fitnessdiary.R
import ru.pacani.fitnessdiary.ui.theme.ButtonShape
import ru.pacani.fitnessdiary.ui.theme.Dimens
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme

/**
 * ВРЕМЕННО: каркас экрана-заглушки, чтобы проверить навигацию.
 * Удаляется, когда все экраны свёрстаны.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenStub(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.stub_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/** ВРЕМЕННО: кнопка перехода на заглушке. */
@Composable
fun StubButton(@StringRes text: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight),
    ) {
        Text(stringResource(text))
    }
}

@Preview
@Composable
private fun ScreenStubPreview() {
    FitnessDiaryTheme {
        ScreenStub(title = "История", onBack = {}) {
            StubButton(R.string.stub_open_workout, onClick = {})
        }
    }
}
