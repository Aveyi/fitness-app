package ru.pacani.fitnessdiary.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хеширование паролей: PBKDF2WithHmacSHA256, 120 000 итераций, 256 бит (раздел 8 context.md).
 * Соль и хеш храним в Base64. Пароль в открытом виде нигде не сохраняется.
 *
 * Хеширование намеренно медленное (сотни миллисекунд на телефоне),
 * поэтому вызывать его нужно не в главном потоке, а через `withContext(Dispatchers.Default)`.
 */
class PasswordHasher(private val random: SecureRandom = SecureRandom()) {

    /** Новая случайная соль для регистрации, Base64. */
    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return encoder.encodeToString(salt)
    }

    /** Хеш пароля с данной солью, Base64. */
    fun hash(password: String, salt: String): String =
        encoder.encodeToString(derive(password, decoder.decode(salt)))

    /** Совпадает ли пароль с сохранённым хешем. Сравнение за постоянное время. */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = derive(password, decoder.decode(salt))
        val expected = decoder.decode(expectedHash)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_BYTES = 16

        private val encoder = Base64.getEncoder()
        private val decoder = Base64.getDecoder()
    }
}
