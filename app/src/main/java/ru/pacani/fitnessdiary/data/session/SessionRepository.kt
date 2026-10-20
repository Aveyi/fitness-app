package ru.pacani.fitnessdiary.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Один DataStore на всё приложение: создавать второй на тот же файл нельзя. */
val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Сессия: id вошедшего пользователя. Пароль и прочие данные сюда не пишем.
 *
 * DataStore передаётся снаружи, чтобы в тестах подставить DataStore во временном файле.
 */
class SessionRepository(private val dataStore: DataStore<Preferences>) {

    /** id текущего пользователя или null, если никто не вошёл. */
    val currentUserId: Flow<Long?> = dataStore.data
        // Повреждённый файл не должен ронять приложение: считаем, что сессии нет
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[CURRENT_USER_ID] }
        .distinctUntilChanged()

    suspend fun setCurrentUser(userId: Long) {
        dataStore.edit { prefs -> prefs[CURRENT_USER_ID] = userId }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(CURRENT_USER_ID) }
    }

    private companion object {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }
}
