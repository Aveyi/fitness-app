package ru.pacani.fitnessdiary.data.repository

import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.pacani.fitnessdiary.data.local.TransactionRunner
import ru.pacani.fitnessdiary.data.local.dao.ExerciseDao
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.toDomain
import ru.pacani.fitnessdiary.data.remote.WgerApi
import ru.pacani.fitnessdiary.data.remote.WgerMapper
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.domain.model.Exercise
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

class DefaultExerciseRepository(
    private val api: WgerApi,
    private val dao: ExerciseDao,
    private val transaction: TransactionRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExerciseRepository {

    private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("ru")).apply {
        strength = Collator.PRIMARY
    }

    override fun observeExercises(
        userId: Long?,
        query: String,
        category: ExerciseCategory?,
    ): Flow<List<Exercise>> {
        val needle = query.trim()
        // -1: такого пользователя нет, остаются только упражнения wger
        return dao.observeAll(userId ?: NO_USER, category?.key)
            .map { entities ->
                entities
                    .asSequence()
                    .filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) }
                    .sortedWith(compareBy(collator) { it.name })
                    .map { it.toDomain() }
                    .toList()
            }
            .flowOn(Dispatchers.Default)
    }

    override fun observeExercise(exerciseId: Long): Flow<Exercise?> =
        dao.observeById(exerciseId).map { it?.toDomain() }

    override suspend fun refresh(): Result<Int> = withContext(ioDispatcher) {
        try {
            val languages = api.getLanguages().results
            val russianId = languages.firstOrNull { it.shortName == "ru" }?.id
            val englishId = languages.firstOrNull { it.shortName == "en" }?.id ?: WgerMapper.DEFAULT_ENGLISH_ID

            val entities = loadAllPages()
                .mapNotNull { WgerMapper.toEntity(it, russianId, englishId) }
                .distinctBy { it.remoteId }

            upsert(entities)
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshIfEmpty(): Result<Unit> =
        if (dao.countRemote() == 0) refresh().map { } else Result.success(Unit)

    override suspend fun addCustomExercise(userId: Long, name: String, category: ExerciseCategory): Long =
        dao.insertCustom(
            ExerciseEntity(
                name = name.trim().replaceFirstChar { it.uppercase() },
                category = category.key,
                isCustom = true,
                userId = userId,
            ),
        )

    private suspend fun loadAllPages(): List<ExerciseInfoDto> {
        val result = mutableListOf<ExerciseInfoDto>()
        var offset = 0
        do {
            val page = api.getExercises(limit = PAGE_SIZE, offset = offset)
            result += page.results
            offset += PAGE_SIZE
        } while (page.next != null && page.results.isNotEmpty() && offset < MAX_ITEMS)
        return result
    }

    /**
     * Upsert по remoteId: существующие строки обновляем по их локальному id, новые вставляем.
     * Не через REPLACE: он удаляет строку и создаёт новую с другим id, а упражнение
     * уже может быть в истории тренировок (FK RESTRICT).
     */
    private suspend fun upsert(entities: List<ExerciseEntity>) = transaction.inTransaction {
        entities.chunked(SQL_CHUNK).forEach { chunk ->
            val existing = dao.getIdsByRemoteIds(chunk.mapNotNull { it.remoteId })
                .associate { it.remoteId to it.id }
            val (toUpdate, toInsert) = chunk.partition { it.remoteId in existing }
            if (toUpdate.isNotEmpty()) {
                dao.updateAll(toUpdate.map { it.copy(id = existing.getValue(it.remoteId!!)) })
            }
            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert)
            }
        }
    }

    private companion object {
        const val NO_USER = -1L
        const val PAGE_SIZE = 100
        /** Защита от бесконечного цикла, если API начнёт отдавать странные страницы. */
        const val MAX_ITEMS = 5_000
        /** Лимит параметров SQLite на старых Android — 999. */
        const val SQL_CHUNK = 500
    }
}
