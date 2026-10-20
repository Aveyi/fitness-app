package ru.pacani.fitnessdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pacani.fitnessdiary.ui.FitnessDiaryApp
import ru.pacani.fitnessdiary.ui.MainViewModel
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // До super.onCreate. Splash держится, пока не ясно, есть ли сессия:
        // так пользователь не видит мелькания экрана входа перед Главной.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.startDestination.value == null }
        enableEdgeToEdge()
        setContent {
            FitnessDiaryTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                startDestination?.let { FitnessDiaryApp(startDestination = it) }
            }
        }
    }
}
