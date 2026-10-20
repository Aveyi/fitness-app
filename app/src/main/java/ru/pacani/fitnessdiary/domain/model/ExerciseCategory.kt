package ru.pacani.fitnessdiary.domain.model

/**
 * Категория упражнения. В базе хранится [key] — английское имя категории wger,
 * подписи на русском берутся из strings.xml (ui/exercises/ExerciseCategoryLabels.kt).
 */
enum class ExerciseCategory(val key: String) {
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    ABS("Abs"),
    CARDIO("Cardio"),
    OTHER("Other"),
    ;

    companion object {
        /** Категории для чипов-фильтров и выбора при создании своего упражнения. */
        val filterable: List<ExerciseCategory> = listOf(CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS)

        fun fromKey(key: String): ExerciseCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER

        /** Категория wger → наша. Икры (Calves) показываем в «Ногах». */
        fun fromWger(name: String?): ExerciseCategory = when {
            name == null -> OTHER
            name.equals("Calves", ignoreCase = true) -> LEGS
            else -> fromKey(name)
        }
    }
}
