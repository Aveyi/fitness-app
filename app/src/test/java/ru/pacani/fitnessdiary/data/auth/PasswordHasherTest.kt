package ru.pacani.fitnessdiary.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test
    fun samePasswordAndSalt_giveSameHash() {
        val salt = hasher.newSalt()
        assertEquals(hasher.hash("secret123", salt), hasher.hash("secret123", salt))
    }

    @Test
    fun differentSalt_givesDifferentHash() {
        val first = hasher.hash("secret123", hasher.newSalt())
        val second = hasher.hash("secret123", hasher.newSalt())
        assertNotEquals(first, second)
    }

    @Test
    fun newSalt_isRandom16Bytes() {
        val salt = hasher.newSalt()
        assertEquals(PasswordHasher.SALT_BYTES, Base64.getDecoder().decode(salt).size)
        assertNotEquals(salt, hasher.newSalt())
    }

    @Test
    fun hash_is256Bits() {
        val hash = hasher.hash("secret123", hasher.newSalt())
        assertEquals(PasswordHasher.KEY_LENGTH_BITS / 8, Base64.getDecoder().decode(hash).size)
    }

    @Test
    fun verify_acceptsCorrectPassword() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("secret123", salt)
        assertTrue(hasher.verify("secret123", salt, hash))
    }

    @Test
    fun verify_rejectsWrongPassword() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("secret123", salt)
        assertFalse(hasher.verify("secret124", salt, hash))
        assertFalse(hasher.verify("Secret123", salt, hash))
        assertFalse(hasher.verify("", salt, hash))
    }

    @Test
    fun verify_rejectsCorrectPasswordWithOtherSalt() {
        val hash = hasher.hash("secret123", hasher.newSalt())
        assertFalse(hasher.verify("secret123", hasher.newSalt(), hash))
    }

    @Test
    fun hash_doesNotContainPassword() {
        val hash = hasher.hash("secret123", hasher.newSalt())
        assertFalse(hash.contains("secret123"))
    }
}
