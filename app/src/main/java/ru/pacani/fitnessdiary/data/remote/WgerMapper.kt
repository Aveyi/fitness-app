package ru.pacani.fitnessdiary.data.remote

import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.domain.model.ExerciseCategory

/** DTO wger → строка таблицы `exercises`. */
object WgerMapper {

    /** id английского языка в wger на случай, если `language/` не ответил как ожидалось. */
    const val DEFAULT_ENGLISH_ID = 2

    /**
     * Название: русский перевод, если есть, иначе английский. Описание: из того же перевода,
     * а если оно пустое — из английского. Без русского и английского перевода упражнение пропускаем.
     */
    fun toEntity(dto: ExerciseInfoDto, russianId: Int?, englishId: Int): ExerciseEntity? {
        val translations = dto.allTranslations.filter { it.name.isNotBlank() }
        val english = translations.firstOrNull { it.language == englishId }
        val russian = russianId?.let { id -> translations.firstOrNull { it.language == id } }
        val chosen = russian ?: english ?: return null

        val description = HtmlCleaner.clean(chosen.description)
            .ifEmpty { HtmlCleaner.clean(english?.description) }

        return ExerciseEntity(
            remoteId = dto.id,
            name = chosen.name.trim().replaceFirstChar { it.uppercase() },
            description = description.ifEmpty { null },
            category = ExerciseCategory.fromWger(dto.category?.name).key,
            equipment = formatEquipment(dto.equipment.map { it.name }),
            isCustom = false,
            userId = null,
        )
    }

    /** ["Barbell", "Bench"] → «Штанга, скамья». */
    fun formatEquipment(names: List<String>): String? {
        val russian = names
            .map { translateEquipment(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (russian.isEmpty()) return null
        return russian.mapIndexed { index, name ->
            if (index == 0) name.replaceFirstChar { it.uppercase() } else name.replaceFirstChar { it.lowercase() }
        }.joinToString(", ")
    }

    private fun translateEquipment(name: String): String =
        equipmentNames[name.trim().lowercase()] ?: name.trim()

    private val equipmentNames = mapOf(
        "barbell" to "Штанга",
        "sz-bar" to "EZ-гриф",
        "dumbbell" to "Гантели",
        "gym mat" to "Коврик",
        "swiss ball" to "Фитбол",
        "pull-up bar" to "Турник",
        "none (bodyweight exercise)" to "Без оборудования",
        "bench" to "Скамья",
        "incline bench" to "Наклонная скамья",
        "kettlebell" to "Гиря",
        "resistance band" to "Резиновая лента",
    )
}
