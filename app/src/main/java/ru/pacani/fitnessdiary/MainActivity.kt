package ru.pacani.fitnessdiary

import android.content.Context
import android.content.res.Configuration
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
import java.util.Locale

private val AppLocale: Locale = Locale.forLanguageTag("ru")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    /**
     * Приложение только на русском. Без этого на телефоне с другим языком системы
     * Android склоняет по чужим правилам: «0 тренировки», «5 упражнения».
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(AppLocale) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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
