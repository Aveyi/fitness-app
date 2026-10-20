package ru.pacani.fitnessdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.pacani.fitnessdiary.ui.FitnessDiaryApp
import ru.pacani.fitnessdiary.ui.theme.FitnessDiaryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // До super.onCreate: системный splash держится, пока не отрисуется первый кадр.
        // Когда появится SessionRepository, здесь будет setKeepOnScreenCondition,
        // пока не придёт первое значение currentUserId.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessDiaryTheme {
                FitnessDiaryApp()
            }
        }
    }
}
