package ru.pacani.fitnessdiary.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.data.remote.dto.PageDto
import ru.pacani.fitnessdiary.data.remote.dto.WgerJson
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

class WgerMapperTest {

    private val english = 2
    private val russian = 7

    /** Ответ `exerciseinfo/` в формате wger (лишние поля маппер должен игнорировать). */
    private val json = """
        {
          "count": 3,
          "next": null,
          "previous": null,
          "results": [
            {
              "id": 192,
              "uuid": "a1b2",
              "category": { "id": 11, "name": "Chest" },
              "muscles": [ { "id": 4, "name": "Pectoralis major" } ],
              "equipment": [ { "id": 1, "name": "Barbell" }, { "id": 8, "name": "Bench" } ],
              "images": [],
              "translations": [
                { "id": 1, "name": "Bench Press", "description": "<p>Lie on the bench.</p>", "language": 2, "aliases": [] },
                { "id": 2, "name": "жим штанги лёжа", "description": "<ol><li>Лягте на скамью</li><li>Выжмите штангу</li></ol>", "language": 7 }
              ]
            },
            {
              "id": 111,
              "category": { "id": 14, "name": "Calves" },
              "equipment": [ { "id": 7, "name": "none (bodyweight exercise)" } ],
              "translations": [
                { "name": "Calf Raises", "description": "<p>Stand on your toes.</p>", "language": 2 },
                { "name": "Подъёмы на носки", "description": "", "language": 7 }
              ]
            },
            {
              "id": 999,
              "category": { "id": 10, "name": "Abs" },
              "equipment": [],
              "translations": [ { "name": "Bauchpresse", "description": "", "language": 1 } ]
            }
          ]
        }
    """.trimIndent()

    private val page: PageDto<ExerciseInfoDto> = WgerJson.decodeFromString(json)

    private fun dto(id: Int) = page.results.first { it.id == id }

    @Test
    fun `ответ wger разбирается, лишние поля игнорируются`() {
        assertEquals(3, page.count)
        assertNull(page.next)
        assertEquals(3, page.results.size)
    }

    @Test
    fun `русский перевод в приоритете`() {
        val entity = WgerMapper.toEntity(dto(192), russian, english)!!
        assertEquals(192, entity.remoteId)
        assertEquals("Жим штанги лёжа", entity.name)
        assertEquals("• Лягте на скамью\n• Выжмите штангу", entity.description)
        assertEquals(ExerciseCategory.CHEST.key, entity.category)
        assertEquals("Штанга, скамья", entity.equipment)
        assertEquals(false, entity.isCustom)
        assertNull(entity.userId)
    }

    @Test
    fun `без русского перевода берётся английский`() {
        val entity = WgerMapper.toEntity(dto(192), russianId = null, englishId = english)!!
        assertEquals("Bench Press", entity.name)
        assertEquals("Lie on the bench.", entity.description)
    }

    @Test
    fun `пустое русское описание заменяется английским`() {
        val entity = WgerMapper.toEntity(dto(111), russian, english)!!
        assertEquals("Подъёмы на носки", entity.name)
        assertEquals("Stand on your toes.", entity.description)
    }

    @Test
    fun `икры попадают в ноги, без оборудования переводится`() {
        val entity = WgerMapper.toEntity(dto(111), russian, english)!!
        assertEquals(ExerciseCategory.LEGS.key, entity.category)
        assertEquals("Без оборудования", entity.equipment)
    }

    @Test
    fun `упражнение без русского и английского перевода пропускается`() {
        assertNull(WgerMapper.toEntity(dto(999), russian, english))
    }

    @Test
    fun `переводы из старого поля exercises тоже читаются`() {
        val legacy = """
            { "id": 5, "category": { "id": 8, "name": "Arms" },
              "exercises": [ { "name": "Biceps Curls", "description": "", "language": 2 } ] }
        """.trimIndent()
        val entity = WgerMapper.toEntity(WgerJson.decodeFromString<ExerciseInfoDto>(legacy), russian, english)!!
        assertEquals("Biceps Curls", entity.name)
        assertEquals(ExerciseCategory.ARMS.key, entity.category)
        assertNull(entity.description)
        assertNull(entity.equipment)
    }

    @Test
    fun `неизвестное оборудование остаётся как есть`() {
        assertEquals("Кабель", WgerMapper.formatEquipment(listOf("Кабель")))
        assertEquals("Гантели, гиря", WgerMapper.formatEquipment(listOf("Dumbbell", "Kettlebell", "dumbbell")))
        assertTrue(WgerMapper.formatEquipment(emptyList()) == null)
    }
}
