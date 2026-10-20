package ru.pacani.fitnessdiary.data.repository

import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.pacani.fitnessdiary.data.remote.dto.CategoryDto
import ru.pacani.fitnessdiary.data.remote.dto.EquipmentDto
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.data.remote.dto.TranslationDto
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

class DefaultExerciseRepositoryTest {

    private lateinit var dao: FakeExerciseDao
    private lateinit var api: FakeWgerApi
    private lateinit var repository: DefaultExerciseRepository

    private fun wger(id: Int, ru: String?, en: String, category: String = "Chest") = ExerciseInfoDto(
        id = id,
        category = CategoryDto(1, category),
        equipment = listOf(EquipmentDto(1, "Barbell")),
        translations = listOfNotNull(
            TranslationDto(name = en, language = 2),
            ru?.let { TranslationDto(name = it, language = 7) },
        ),
    )

    @Before
    fun setUp() {
        dao = FakeExerciseDao()
        api = FakeWgerApi()
    }

    private fun createRepository(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        repository = DefaultExerciseRepository(api, dao, ImmediateTransactionRunner(), StandardTestDispatcher(testScheduler))
    }

    @Test
    fun `refresh загружает все страницы`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(
            (1..100).map { wger(it, null, "Exercise $it") },
            listOf(wger(101, "Жим лёжа", "Bench Press")),
        )

        val result = repository.refresh()

        assertEquals(101, result.getOrThrow())
        assertEquals(2, api.exerciseRequests)
        assertEquals(101, dao.exercises.value.size)
    }

    @Test
    fun `повторный refresh обновляет упражнение и сохраняет его локальный id`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(listOf(wger(192, null, "Bench Press")))
        repository.refresh()
        val localId = dao.exercises.value.single().id

        // В wger появился русский перевод
        api.pages = listOf(listOf(wger(192, "Жим штанги лёжа", "Bench Press")))
        repository.refresh()

        val exercise = dao.exercises.value.single()
        assertEquals(localId, exercise.id)
        assertEquals("Жим штанги лёжа", exercise.name)
    }

    @Test
    fun `ошибка сети возвращается как failure, кэш не трогается`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(listOf(wger(1, "Жим", "Press")))
        repository.refresh()
        api.error = IOException("нет сети")

        val result = repository.refresh()

        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(1, dao.exercises.value.size)
    }

    @Test
    fun `refreshIfEmpty не ходит в сеть, если кэш уже есть`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(listOf(wger(1, "Жим", "Press")))
        repository.refreshIfEmpty()
        repository.refreshIfEmpty()

        assertEquals(1, api.exerciseRequests)
    }

    @Test
    fun `поиск не зависит от регистра кириллицы`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(
            listOf(
                wger(1, "Жим штанги лёжа", "Bench Press"),
                wger(2, "Приседания", "Squat", category = "Legs"),
            ),
        )
        repository.refresh()

        val found = repository.observeExercises(userId = null, query = "жим", category = null).first()

        assertEquals(listOf("Жим штанги лёжа"), found.map { it.name })
    }

    @Test
    fun `фильтр по категории и сортировка по алфавиту`() = runTest {
        createRepository(testScheduler)
        api.pages = listOf(
            listOf(
                wger(1, "Разводка гантелей", "Fly"),
                wger(2, "Жим штанги лёжа", "Bench Press"),
                wger(3, "Приседания", "Squat", category = "Legs"),
            ),
        )
        repository.refresh()

        val chest = repository.observeExercises(null, "", ExerciseCategory.CHEST).first()

        assertEquals(listOf("Жим штанги лёжа", "Разводка гантелей"), chest.map { it.name })
    }

    @Test
    fun `свои упражнения видит только их владелец`() = runTest {
        createRepository(testScheduler)
        repository.addCustomExercise(userId = 1, name = "  жим в Смите ", category = ExerciseCategory.CHEST)

        val mine = repository.observeExercises(userId = 1, query = "", category = null).first()
        val others = repository.observeExercises(userId = 2, query = "", category = null).first()
        val guest = repository.observeExercises(userId = null, query = "", category = null).first()

        assertEquals("Жим в Смите", mine.single().name)
        assertTrue(mine.single().isCustom)
        assertTrue(others.isEmpty())
        assertTrue(guest.isEmpty())
    }
}
