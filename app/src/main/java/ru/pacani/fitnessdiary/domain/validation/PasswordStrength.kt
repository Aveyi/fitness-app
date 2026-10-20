package ru.pacani.fitnessdiary.domain.validation

/** Надёжность пароля для PasswordStrengthIndicator: сколько из 4 сегментов заполнено. */
enum class PasswordStrength(val filledSegments: Int) {
    NONE(0),
    WEAK(1),
    MEDIUM(2),
    STRONG(3),
    VERY_STRONG(4),
}

/**
 * Оценка надёжности:
 * - не проходит [Validators.validatePassword] (короче 8 символов, нет буквы или цифры) — слабый;
 * - проходит — средний, и +1 уровень за каждый бонус: длина от 12 символов,
 *   есть заглавные и строчные буквы, есть спецсимвол.
 */
fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.NONE
    if (Validators.validatePassword(password) != null) return PasswordStrength.WEAK

    var bonuses = 0
    if (password.length >= LONG_PASSWORD_LENGTH) bonuses++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) bonuses++
    if (password.any { !it.isLetterOrDigit() }) bonuses++

    return when (bonuses) {
        0 -> PasswordStrength.MEDIUM
        1 -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

private const val LONG_PASSWORD_LENGTH = 12
