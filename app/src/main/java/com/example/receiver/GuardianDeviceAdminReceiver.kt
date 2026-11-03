package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.utils.CameraHelper
import com.example.utils.JalaliCalendarHelper
import com.example.utils.LocationTracker
import com.example.utils.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
        Toast.makeText(context, "مدیر دستگاه فعال شد: $farsiTime", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Warning dialog to detour uninstallation attempt of user's anti-theft app
        return "⚠️ اخطار: غیرفعال‌سازی سیستم مدیریت گاردین ممکن است امنیت گوشی شما را به خطر بیندازد و اجازه حذف برنامه را به سارقان بدهد!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "مدیر دستگاه غیرفعال شد", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val prefs = GuardianPrefs(context)
        val count = prefs.failedUnlockAttemptsCount + 1
        prefs.failedUnlockAttemptsCount = count

        Log.w("GuardianAdmin", "System lock password failed (attempt $count)")

        // 1. Secretly capture intruder photo if armed and enabled
        if (prefs.isArmed && prefs.isIntruderSelfieEnabled) {
            try {
                CameraHelper.captureIntruderPhoto(context)
            } catch (e: Exception) {
                Log.e("GuardianAdmin", "Failed to capture intruder photo: ${e.message}")
            }
        }

        // 2. Asynchronously log the security breach and alert owner if threshold reached
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val locResult = LocationTracker.getOfflineLocationData(context)
                val pos = if (locResult.isGPS) "${locResult.lat},${locResult.lng}" else "نامشخص"
                val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())

                db.logDao().insertLog(LogEntry(
                    eventType = "ALARM",
                    description = "تلاش ناموفق برای باز کردن قفل اصلی گوشی (تلاش $count)",
                    location = pos,
                    details = "شناسایی توسط مدیر دستگاه در $farsiTime"
                ))

                // If consecutive failures reach 3 or more and emergency number is set, dispatch silent SMS
                if (count >= 3 && prefs.isArmed && prefs.emergencyNumber.isNotEmpty()) {
                    val alertSms = "⚠️ هشدار گاردین: $count بار تلاش ناموفق برای باز کردن قفل گوشی شناسایی شد!\nزمان: $farsiTime\nموقعیت: $pos"
                    SecurityUtils.sendDirectSms(context, prefs.emergencyNumber, alertSms, prefs)
                }
            } catch (e: Exception) {
                Log.e("GuardianAdmin", "Error handling onPasswordFailed log: ${e.message}")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val prefs = GuardianPrefs(context)
        prefs.failedUnlockAttemptsCount = 0
    }
}

