package ru.pacani.fitnessdiary.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    // Email

    @Test
    fun email_valid() {
        assertNull(Validators.validateEmail("ivan@example.com"))
        assertNull(Validators.validateEmail("ivan.petrov+gym@mail.co.uk"))
        assertNull(Validators.validateEmail("  ivan@example.com  ")) // пробелы по краям обрезаются
    }

    @Test
    fun email_empty() {
        assertEquals(ValidationError.REQUIRED, Validators.validateEmail(""))
        assertEquals(ValidationError.REQUIRED, Validators.validateEmail("   "))
    }

    @Test
    fun email_invalid() {
        listOf("ivan", "ivan@", "@example.com", "ivan@example", "ivan@@example.com", "iv an@example.com", "иван@почта.рф")
            .forEach { assertEquals(it, ValidationError.EMAIL_INVALID, Validators.validateEmail(it)) }
    }

    // Пароль

    @Test
    fun password_valid() {
        assertNull(Validators.validatePassword("abcdefg1"))     // ровно 8 символов
        assertNull(Validators.validatePassword("пароль123"))    // кириллица — тоже буквы
    }

    @Test
    fun password_empty() {
        assertEquals(ValidationError.REQUIRED, Validators.validatePassword(""))
    }

    @Test
    fun password_tooShort() {
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, Validators.validatePassword("abcdef1")) // 7 символов
    }

    @Test
    fun password_needsLetterAndDigit() {
        assertEquals(ValidationError.PASSWORD_NEEDS_LETTER_AND_DIGIT, Validators.validatePassword("abcdefgh"))
        assertEquals(ValidationError.PASSWORD_NEEDS_LETTER_AND_DIGIT, Validators.validatePassword("12345678"))
        assertEquals(ValidationError.PASSWORD_NEEDS_LETTER_AND_DIGIT, Validators.validatePassword("!!!!!!!!"))
    }

    @Test
    fun passwordRepeat() {
        assertNull(Validators.validatePasswordRepeat("abcdefg1", "abcdefg1"))
        assertEquals(ValidationError.REQUIRED, Validators.validatePasswordRepeat("abcdefg1", ""))
        assertEquals(ValidationError.PASSWORDS_DO_NOT_MATCH, Validators.validatePasswordRepeat("abcdefg1", "abcdefg2"))
    }

    // Имя

    @Test
    fun name_bounds() {
        assertNull(Validators.validateName("Ян"))               // 2 — нижняя граница
        assertNull(Validators.validateName("a".repeat(40)))     // 40 — верхняя граница
        assertNull(Validators.validateName("  Иван  "))
        assertEquals(ValidationError.NAME_LENGTH, Validators.validateName("Я"))
        assertEquals(ValidationError.NAME_LENGTH, Validators.validateName("a".repeat(41)))
        assertEquals(ValidationError.NAME_LENGTH, Validators.validateName(" Я "))
    }

    @Test
    fun name_empty() {
        assertEquals(ValidationError.REQUIRED, Validators.validateName(""))
        assertEquals(ValidationError.REQUIRED, Validators.validateName("   "))
    }

    // Рост

    @Test
    fun height_optional() {
        assertNull(Validators.validateHeight(""))
        assertNull(Validators.validateHeight("  "))
    }

    @Test
    fun height_bounds() {
        assertNull(Validators.validateHeight("100"))
        assertNull(Validators.validateHeight("250"))
        assertNull(Validators.validateHeight(" 180 "))
        assertEquals(ValidationError.HEIGHT_OUT_OF_RANGE, Validators.validateHeight("99"))
        assertEquals(ValidationError.HEIGHT_OUT_OF_RANGE, Validators.validateHeight("251"))
        assertEquals(ValidationError.HEIGHT_OUT_OF_RANGE, Validators.validateHeight("-180"))
    }

    @Test
    fun height_notANumber() {
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateHeight("abc"))
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateHeight("180.5"))
    }

    // Вес

    @Test
    fun weight_optional() {
        assertNull(Validators.validateWeight(""))
    }

    @Test
    fun weight_bounds() {
        assertNull(Validators.validateWeight("30"))
        assertNull(Validators.validateWeight("300"))
        assertNull(Validators.validateWeight("78.5"))
        assertNull(Validators.validateWeight("78,5"))           // запятая с русской клавиатуры
        assertEquals(ValidationError.WEIGHT_OUT_OF_RANGE, Validators.validateWeight("29.9"))
        assertEquals(ValidationError.WEIGHT_OUT_OF_RANGE, Validators.validateWeight("300.1"))
    }

    @Test
    fun weight_notANumber() {
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateWeight("abc"))
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateWeight("78,5,1"))
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateWeight("NaN"))
        assertEquals(ValidationError.NOT_A_NUMBER, Validators.validateWeight("Infinity"))
    }

    @Test
    fun parse() {
        assertEquals(180, Validators.parseHeight(" 180 "))
        assertEquals(78.5, Validators.parseWeight("78,5")!!, 0.0)
    }
}
