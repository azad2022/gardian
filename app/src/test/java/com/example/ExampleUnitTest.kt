package com.example

import com.example.utils.JalaliCalendarHelper
import com.example.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_sanitize_persian_phone_numbers() {
        // Persian digits ۰۹۱۲۳۴۵۶۷۸۹
        val rawPersian = "۰۹۱۲۳۴۵۶۷۸۹"
        val sanitized = SecurityUtils.sanitizePhoneNumber(rawPersian)
        assertEquals("09123456789", sanitized)

        // Arabic digits with +
        val arabicWithPlus = "+۹۸۹۱۲۳۴۵۶۷۸۹"
        val sanitizedPlus = SecurityUtils.sanitizePhoneNumber(arabicWithPlus)
        assertEquals("+989123456789", sanitizedPlus)
    }

    @Test
    fun test_security_utils_encryption_and_decryption() {
        val original = "09123456789"
        val encrypted = SecurityUtils.encrypt(original)
        assertNotEquals(original, encrypted)
        val decrypted = SecurityUtils.decrypt(encrypted)
        assertEquals(original, decrypted)
    }

    @Test
    fun test_security_utils_hash() {
        val hash1 = SecurityUtils.hashSha256("1234")
        val hash2 = SecurityUtils.hashSha256("1234")
        val hash3 = SecurityUtils.hashSha256("5678")
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
        assertTrue(hash1.isNotEmpty())
    }

    @Test
    fun test_jalali_calendar_conversion() {
        val timestamp = 1781614800000L // 2026-06-16 16:30
        val jalali = JalaliCalendarHelper.getJalaliDateString(timestamp)
        assertTrue(jalali.contains("۱۴۰۵"))
    }
}

