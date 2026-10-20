package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

interface ExerciseRepository {

    /**
     * Каталог для экрана «Упражнения»: упражнения wger и свои упражнения пользователя.
     * Читается всегда из Room. [userId] = null — только упражнения wger.
     * Поиск по [query] без учёта регистра (в том числе для кириллицы), сортировка по алфавиту.
     */
    fun observeExercises(userId: Long?, query: String, category: ExerciseCategory?): Flow<List<Exercise>>

    fun observeExercise(exerciseId: Long): Flow<Exercise?>

    /** Загружает весь каталог из wger и обновляет кэш. Возвращает число упражнений. */
    suspend fun refresh(): Result<Int>

    /** Загружает каталог, только если кэш пуст (первый запуск). */
    suspend fun refreshIfEmpty(): Result<Unit>

    /** Создаёт своё упражнение. Возвращает его id. */
    suspend fun addCustomExercise(userId: Long, name: String, category: ExerciseCategory): Long
}
