package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.utils.SecurityUtils

class GuardianPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("guardian_secure_prefs", Context.MODE_PRIVATE)

    var isOnboarded: Boolean
        get() = prefs.getBoolean("is_onboarded", false)
        set(value) = prefs.edit().putBoolean("is_onboarded", value).apply()

    var isArmed: Boolean
        get() = prefs.getBoolean("is_armed", false)
        set(value) = prefs.edit().putBoolean("is_armed", value).apply()

    private fun sanitizePhoneNumber(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], (i + 48).toChar())
            result = result.replace(arabicDigits[i], (i + 48).toChar())
        }
        return result.filter { it.isDigit() || it == '+' }.trim()
    }

    var emergencyNumber: String
        get() {
            val decrypted = SecurityUtils.decrypt(prefs.getString("emergency_number", "") ?: "")
            return sanitizePhoneNumber(decrypted)
        }
        set(value) {
            val sanitized = sanitizePhoneNumber(value)
            prefs.edit().putString("emergency_number", SecurityUtils.encrypt(sanitized)).apply()
        }

    var secretSmsCode: String
        get() = prefs.getString("secret_sms_code_hash", "") ?: ""
        set(value) = prefs.edit().putString("secret_sms_code_hash", SecurityUtils.hashSha256(value)).apply()

    // Plain 4-digit code stored to verify without needing hashing when reading in SMS or screen overlay
    var rawSecretCode: String
        get() = prefs.getString("raw_secret_code", "1234") ?: "1234"
        set(value) = prefs.edit().putString("raw_secret_code", value).apply()

    var decoyPin: String
        get() = prefs.getString("decoy_pin", "") ?: ""
        set(value) = prefs.edit().putString("decoy_pin", value).apply()

    var lastSimIccid: String
        get() = prefs.getString("last_sim_iccid", "") ?: ""
        set(value) = prefs.edit().putString("last_sim_iccid", value).apply()

    var currentMode: String
        get() = prefs.getString("current_mode", "pocket") ?: "pocket"
        set(value) = prefs.edit().putString("current_mode", value).apply()

    var isSimChangeDetectionEnabled: Boolean
        get() = prefs.getBoolean("sim_change_detection", true)
        set(value) = prefs.edit().putBoolean("sim_change_detection", value).apply()

    var isUsbAlertEnabled: Boolean
        get() = prefs.getBoolean("usb_connection_alert", true)
        set(value) = prefs.edit().putBoolean("usb_connection_alert", value).apply()

    var isChargerAlertEnabled: Boolean
        get() = prefs.getBoolean("charger_alert", true)
        set(value) = prefs.edit().putBoolean("charger_alert", value).apply()

    var isBluetoothGuardianEnabled: Boolean
        get() = prefs.getBoolean("bluetooth_guardian", false)
        set(value) = prefs.edit().putBoolean("bluetooth_guardian", value).apply()

    var bluetoothDeviceName: String
        get() = prefs.getString("bt_device_name", "") ?: ""
        set(value) = prefs.edit().putString("bt_device_name", value).apply()

    var bluetoothDeviceAddress: String
        get() = prefs.getString("bt_device_address", "") ?: ""
        set(value) = prefs.edit().putString("bt_device_address", value).apply()

    var bluetoothThresholdMinutes: Int
        get() = prefs.getInt("bt_threshold_minutes", 5)
        set(value) = prefs.edit().putInt("bt_threshold_minutes", value).apply()

    var heartbeatIntervalHours: Int
        get() = prefs.getInt("heartbeat_interval_hours", 6)
        set(value) = prefs.edit().putInt("heartbeat_interval_hours", value).apply()

    var lockScreenMessage: String
        get() = prefs.getString("lock_screen_msg", "این گوشی قفل شده است! در صورت یافتن لطفاً با شماره اضطراری تماس بگیرید.") ?: ""
        set(value) = prefs.edit().putString("lock_screen_msg", value).apply()

    var smsSentCount: Int
        get() = prefs.getInt("sms_sent_count", 0)
        set(value) = prefs.edit().putInt("sms_sent_count", value).apply()

    var daysProtected: Int
        get() = prefs.getInt("days_protected", 1)
        set(value) = prefs.edit().putInt("days_protected", value).apply()

    var lastLocationTime: Long
        get() = prefs.getLong("last_location_time", 0L)
        set(value) = prefs.edit().putLong("last_location_time", value).apply()

    var isAppLockerEnabled: Boolean
        get() = prefs.getBoolean("is_app_locker_enabled", true)
        set(value) = prefs.edit().putBoolean("is_app_locker_enabled", value).apply()

    var isDeviceLockEnabled: Boolean
        get() = prefs.getBoolean("is_device_lock_enabled", true)
        set(value) = prefs.edit().putBoolean("is_device_lock_enabled", value).apply()

    var isIntruderSelfieEnabled: Boolean
        get() = prefs.getBoolean("is_intruder_selfie_enabled", true)
        set(value) = prefs.edit().putBoolean("is_intruder_selfie_enabled", value).apply()

    var failedUnlockAttemptsCount: Int
        get() = prefs.getInt("failed_unlock_attempts_count", 0)
        set(value) = prefs.edit().putInt("failed_unlock_attempts_count", value).apply()

    var lastProcessedSmsKey: String
        get() = prefs.getString("last_processed_sms_key", "") ?: ""
        set(value) {
            prefs.edit().putString("last_processed_sms_key", value).commit()
        }

    fun incrementSmsSentCount() {
        smsSentCount += 1
    }

    fun verifySmsCode(input: String): Boolean {
        if (input.isEmpty() || secretSmsCode.isEmpty()) return false
        return SecurityUtils.hashSha256(input) == secretSmsCode
    }
}
