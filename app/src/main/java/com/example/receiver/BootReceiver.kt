package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.service.GuardianService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver to automatically re-launch Guardian anti-theft foreground service
 * and restore full device protection immediately upon phone startup/reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Boot completed broadcast caught: $action")
            val prefs = GuardianPrefs(context)
            if (prefs.isOnboarded && prefs.isArmed) {
                try {
                    val serviceIntent = Intent(context, GuardianService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.i("BootReceiver", "GuardianService restarted successfully after device reboot.")

                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            db.logDao().insertLog(
                                LogEntry(
                                    eventType = "POWER",
                                    description = "سرویس ضدسرقت گاردین پس از روشن شدن مجدد گوشی فعال شد 🛡️",
                                    details = "اکشن دریافتی سیستم: $action"
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("BootReceiver", "Failed to log reboot event", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to start GuardianService on boot", e)
                }
            }
        }
    }
}
