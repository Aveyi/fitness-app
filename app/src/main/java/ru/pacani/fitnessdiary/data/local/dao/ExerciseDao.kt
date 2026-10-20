package ru.pacani.fitnessdiary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.relation.RemoteIdMapping

@Dao
interface ExerciseDao {

    /**
     * Упражнения из wger и свои упражнения пользователя, [category] = null — все категории.
     *
     * Поиск по названию делает ExerciseRepository в Kotlin, а не через LIKE:
     * в SQLite LIKE и COLLATE NOCASE не учитывают регистр только для латиницы,
     * и «жим» не нашёл бы «Жим». Упражнений около тысячи, фильтр в памяти быстрый.
     */
    @Query(
        """
        SELECT * FROM exercises
        WHERE (userId IS NULL OR userId = :userId)
            AND (:category IS NULL OR category = :category)
        """,
    )
    fun observeAll(userId: Long, category: String?): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    /** Сколько упражнений из wger в кэше; 0 — пора загрузить из сети. */
    @Query("SELECT COUNT(*) FROM exercises WHERE remoteId IS NOT NULL")
    suspend fun countRemote(): Int

    /**
     * Для upsert по remoteId. Вызывать порциями до 500 id:
     * у SQLite на старых Android лимит 999 параметров в запросе.
     */
    @Query("SELECT id, remoteId FROM exercises WHERE remoteId IN (:remoteIds)")
    suspend fun getIdsByRemoteIds(remoteIds: List<Int>): List<RemoteIdMapping>

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    /**
     * Обновление по id. Не заменяем REPLACE-вставкой: REPLACE удаляет строку и создаёт
     * новую с другим id, а упражнение уже может быть в истории (FK RESTRICT).
     */
    @Update
    suspend fun updateAll(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insertCustom(exercise: ExerciseEntity): Long
}
