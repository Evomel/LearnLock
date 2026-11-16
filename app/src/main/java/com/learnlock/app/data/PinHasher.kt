package com.learnlock.app.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хеширование родительского PIN через PBKDF2-HMAC-SHA256.
 * PIN в открытом виде нигде не хранится.
 */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    /** Генерирует случайную соль (Base64). */
    fun createSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Считает хеш PIN.
     * @param pin открытый PIN
     * @param saltBase64 соль из [createSalt]
     */
    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    /** Проверяет PIN против сохранённого [ParentAuthEntity]. */
    fun matches(pin: String, entity: ParentAuthEntity): Boolean {
        val candidate = hash(pin, entity.salt)
        return constantTimeEquals(candidate, entity.pinHash)
    }

    /** Сравнение строк за константное время (защита от timing attacks). */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        val left = a.toByteArray()
        val right = b.toByteArray()
        if (left.size != right.size) return false
        var result = 0
        for (i in left.indices) {
            result = result or (left[i].toInt() xor right[i].toInt())
        }
        return result == 0
    }
}
