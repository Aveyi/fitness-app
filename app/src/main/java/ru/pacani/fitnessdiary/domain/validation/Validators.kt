package ru.pacani.fitnessdiary.domain.validation

/** Ошибка поля. Текст для пользователя подбирает экран по этому значению (строки в strings.xml). */
enum class ValidationError {
    REQUIRED,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    PASSWORD_NEEDS_LETTER_AND_DIGIT,
    PASSWORDS_DO_NOT_MATCH,
    NAME_LENGTH,
    NOT_A_NUMBER,
    HEIGHT_OUT_OF_RANGE,
    WEIGHT_OUT_OF_RANGE,
}

/**
 * Проверка полей входа, регистрации и профиля.
 * Без классов Android (Patterns.EMAIL_ADDRESS в юнит-тестах не работает), поэтому обычные Regex.
 * Каждая функция возвращает null, если значение корректно.
 */
object Validators {

    const val PASSWORD_MIN_LENGTH = 8
    val NAME_LENGTH = 2..40
    val HEIGHT_CM = 100..250
    val WEIGHT_KG = 30.0..300.0

    // Тот же шаблон, что у android.util.Patterns.EMAIL_ADDRESS
    private val EMAIL = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
    )
    private val LETTER = Regex("\\p{L}")
    private val DIGIT = Regex("\\d")

    fun validateEmail(email: String): ValidationError? {
        val value = email.trim()
        return when {
            value.isEmpty() -> ValidationError.REQUIRED
            !EMAIL.matches(value) -> ValidationError.EMAIL_INVALID
            else -> null
        }
    }

    /** Правила для нового пароля. При входе проверяем только, что поле не пустое. */
    fun validatePassword(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.REQUIRED
        password.length < PASSWORD_MIN_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        !LETTER.containsMatchIn(password) || !DIGIT.containsMatchIn(password) ->
            ValidationError.PASSWORD_NEEDS_LETTER_AND_DIGIT
        else -> null
    }

    fun validatePasswordRepeat(password: String, repeat: String): ValidationError? = when {
        repeat.isEmpty() -> ValidationError.REQUIRED
        repeat != password -> ValidationError.PASSWORDS_DO_NOT_MATCH
        else -> null
    }

    fun validateName(name: String): ValidationError? {
        val value = name.trim()
        return when {
            value.isEmpty() -> ValidationError.REQUIRED
            value.length !in NAME_LENGTH -> ValidationError.NAME_LENGTH
            else -> null
        }
    }

    /** Рост необязателен: пустое поле — не ошибка. */
    fun validateHeight(input: String): ValidationError? {
        if (input.isBlank()) return null
        val height = parseHeight(input) ?: return ValidationError.NOT_A_NUMBER
        return if (height in HEIGHT_CM) null else ValidationError.HEIGHT_OUT_OF_RANGE
    }

    /** Вес необязателен: пустое поле — не ошибка. */
    fun validateWeight(input: String): ValidationError? {
        if (input.isBlank()) return null
        val weight = parseWeight(input) ?: return ValidationError.NOT_A_NUMBER
        return if (weight in WEIGHT_KG) null else ValidationError.WEIGHT_OUT_OF_RANGE
    }

    /** Рост в см из поля ввода или null, если там не целое число. */
    fun parseHeight(input: String): Int? = input.trim().toIntOrNull()

    /** Вес в кг из поля ввода; принимает и точку, и запятую («78,5»). */
    fun parseWeight(input: String): Double? =
        input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}
