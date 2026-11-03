package com.example.utils

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Base64
import android.util.Log
import com.example.data.prefs.GuardianPrefs
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    // Computes SHA-256 hash of a string (SMS secret code, decoy PIN etc.) for non-reversible storage
    fun hashSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    // A lightweight AES offline encryption for reversible parameters (like Emergency Number)
    // Uses a static local key that doesn't depend on unreliable KeyStore operations offline.
    private const val ALGORITHM = "AES"
    private val keyBytes = byteArrayOf(
        0x47, 0x75, 0x61, 0x72, 0x64, 0x69, 0x61, 0x6e, // "Guardian"
        0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65  // "SecretKe"
    )

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            java.util.Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            plainText // Fallback in extreme case to ensure app doesn't crash as specified
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = java.util.Base64.getDecoder().decode(encryptedText)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback in extreme case
        }
    }

    fun sanitizePhoneNumber(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], (i + 48).toChar())
            result = result.replace(arabicDigits[i], (i + 48).toChar())
        }
        return result.filter { it.isDigit() || it == '+' }.trim()
    }

    fun sendDirectSms(context: Context, to: String, text: String, prefs: GuardianPrefs? = null): Boolean {
        try {
            val sanitizedTo = sanitizePhoneNumber(to)
            if (sanitizedTo.isEmpty() || text.isEmpty()) return false

            val smsManager: SmsManager = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                    if (subId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        @Suppress("DEPRECATION")
                        SmsManager.getSmsManagerForSubscriptionId(subId)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } catch (ex: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(text)
            if (parts != null && parts.size > 1) {
                smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(sanitizedTo, null, text, null, null)
            }

            prefs?.incrementSmsSentCount()
            return true
        } catch (e: Exception) {
            Log.e("SecurityUtils", "Failed to send SMS to $to: ${e.message}")
            return false
        }
    }
}

