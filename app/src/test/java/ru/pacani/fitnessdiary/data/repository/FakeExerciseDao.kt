package ru.pacani.fitnessdiary.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.pacani.fitnessdiary.data.local.dao.ExerciseDao
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.relation.RemoteIdMapping
import ru.pacani.fitnessdiary.data.remote.WgerApi
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.data.remote.dto.LanguageDto
import ru.pacani.fitnessdiary.data.remote.dto.PageDto

/** ExerciseDao в памяти. */
class FakeExerciseDao : ExerciseDao {

    val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(userId: Long, category: String?): Flow<List<ExerciseEntity>> =
        exercises.map { list ->
            list.filter { (it.userId == null || it.userId == userId) && (category == null || it.category == category) }
        }

    override fun observeById(id: Long): Flow<ExerciseEntity?> =
        exercises.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun countRemote(): Int = exercises.value.count { it.remoteId != null }

    override suspend fun getIdsByRemoteIds(remoteIds: List<Int>): List<RemoteIdMapping> =
        exercises.value.filter { it.remoteId in remoteIds }.map { RemoteIdMapping(it.id, it.remoteId!!) }

    override suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long> = exercises.map { insertCustom(it) }

    override suspend fun updateAll(exercises: List<ExerciseEntity>) {
        val byId = exercises.associateBy { it.id }
        this.exercises.value = this.exercises.value.map { byId[it.id] ?: it }
    }

    override suspend fun insertCustom(exercise: ExerciseEntity): Long {
        val id = nextId++
        exercises.value = exercises.value + exercise.copy(id = id)
        return id
    }
}

/** WgerApi с заранее заданными страницами. [error] — бросить при запросе (нет сети и т.п.). */
class FakeWgerApi(
    var pages: List<List<ExerciseInfoDto>> = emptyList(),
    var error: Exception? = null,
) : WgerApi {

    var exerciseRequests = 0
        private set

    override suspend fun getLanguages(limit: Int): PageDto<LanguageDto> {
        error?.let { throw it }
        return PageDto(count = 2, results = listOf(LanguageDto(2, "en"), LanguageDto(7, "ru")))
    }

    override suspend fun getExercises(limit: Int, offset: Int): PageDto<ExerciseInfoDto> {
        error?.let { throw it }
        exerciseRequests++
        val index = offset / limit
        val results = pages.getOrElse(index) { emptyList() }
        val next = if (index < pages.lastIndex) "next" else null
        return PageDto(count = pages.sumOf { it.size }, next = next, results = results)
    }
}
