package ru.pacani.fitnessdiary

import android.app.Application
import ru.pacani.fitnessdiary.di.AppContainer
import ru.pacani.fitnessdiary.di.DefaultAppContainer

class FitnessApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
