package ru.pacani.fitnessdiary.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Настройки JSON для wger: лишние поля игнорируем, чтобы изменения API не ломали приложение. */
val WgerJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Страница ответа: `count`, `next`, `previous`, `results`. */
@Serializable
data class PageDto<T>(
    val count: Int = 0,
    val next: String? = null,
    val results: List<T> = emptyList(),
)

/** `language/`: id языка и короткое имя (`ru`, `en`). */
@Serializable
data class LanguageDto(
    val id: Int,
    @SerialName("short_name") val shortName: String = "",
)

/** `exerciseinfo/`: упражнение с категорией, оборудованием и переводами. */
@Serializable
data class ExerciseInfoDto(
    val id: Int,
    val category: CategoryDto? = null,
    val equipment: List<EquipmentDto> = emptyList(),
    val translations: List<TranslationDto> = emptyList(),
    /** В старых версиях API переводы лежали в поле `exercises`. */
    @SerialName("exercises") val legacyTranslations: List<TranslationDto> = emptyList(),
) {
    val allTranslations: List<TranslationDto> get() = translations.ifEmpty { legacyTranslations }
}

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String = "",
)

@Serializable
data class EquipmentDto(
    val id: Int,
    val name: String = "",
)

@Serializable
data class TranslationDto(
    val name: String = "",
    val description: String = "",
    /** id языка из `language/`. */
    val language: Int = 0,
)
