package ru.pacani.fitnessdiary.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    @Test
    fun empty_isNone() {
        assertEquals(PasswordStrength.NONE, evaluatePasswordStrength(""))
    }

    @Test
    fun failsRules_isWeak() {
        assertEquals(PasswordStrength.WEAK, evaluatePasswordStrength("abc1"))           // короткий
        assertEquals(PasswordStrength.WEAK, evaluatePasswordStrength("abcdefghijklmnop")) // нет цифры
        assertEquals(PasswordStrength.WEAK, evaluatePasswordStrength("1234567890123"))    // нет буквы
        // Бонусы не спасают пароль, который не проходит правила
        assertEquals(PasswordStrength.WEAK, evaluatePasswordStrength("Ab!1"))
    }

    @Test
    fun passesRules_noBonuses_isMedium() {
        assertEquals(PasswordStrength.MEDIUM, evaluatePasswordStrength("abcdefg1"))
    }

    @Test
    fun oneBonus_isStrong() {
        assertEquals(PasswordStrength.STRONG, evaluatePasswordStrength("abcdefghijk1")) // длина 12
        assertEquals(PasswordStrength.STRONG, evaluatePasswordStrength("Abcdefg1"))     // разный регистр
        assertEquals(PasswordStrength.STRONG, evaluatePasswordStrength("abcdef!1"))     // спецсимвол
    }

    @Test
    fun twoOrMoreBonuses_isVeryStrong() {
        assertEquals(PasswordStrength.VERY_STRONG, evaluatePasswordStrength("Abcdef!1"))
        assertEquals(PasswordStrength.VERY_STRONG, evaluatePasswordStrength("Abcdefghijk!1"))
    }

    @Test
    fun segments_growWithStrength() {
        val segments = PasswordStrength.entries.map { it.filledSegments }
        assertEquals(listOf(0, 1, 2, 3, 4), segments)
    }
}
