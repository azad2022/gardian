package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.service.GuardianService
import com.example.utils.AudioManagerHelper
import com.example.utils.JalaliCalendarHelper
import com.example.utils.LocationTracker
import com.example.utils.CameraHelper
import com.example.utils.RemoteWipeHelper
import com.example.ui.overlay.GuardianLockActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = GuardianPrefs(context)
        if (!prefs.isArmed) return // only respond if armed

        val rawCode = prefs.rawSecretCode
        if (rawCode.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val emergencyNum = prefs.emergencyNumber
        if (emergencyNum.isEmpty()) return

        val cleanEmergency = sanitizePhoneNumberForFiltering(emergencyNum)

        // Queue commands to process
        val commandsToExecute = mutableListOf<CommandItem>()

        for (sms in messages) {
            val body = sms.displayMessageBody?.trim() ?: continue
            val sender = sms.displayOriginatingAddress ?: ""

            // --- STRICT CAFE BAZAAR SECURITY AND PRIVACY FILTER ---
            // Only accept command messages sent directly from the owner's emergency contact number.
            val cleanSender = sanitizePhoneNumberForFiltering(sender)
            val match = cleanSender == cleanEmergency || 
                        (cleanSender.length >= 10 && cleanEmergency.length >= 10 && 
                         cleanSender.takeLast(10) == cleanEmergency.takeLast(10))
            
            if (!match) {
                Log.d("SmsReceiver", "Ignored SMS from $sender: Not authorized (Emergency is $emergencyNum)")
                continue
            }

            // Prevent processing duplicate broadcasts
            val smsUniqueKey = "${sms.originatingAddress}_${sms.timestampMillis}"
            if (prefs.lastProcessedSmsKey == smsUniqueKey) {
                continue
            }

            // Standard commands matching pattern: #[COMMAND][CODE]#
            val locCmd = "#LOC$rawCode#"
            val lockCmd = "#LOCK$rawCode#"
            val unlockCmd = "#UNLOCK$rawCode#"
            val wipeCmd = "#WIPE$rawCode#"
            val alarmCmd = "#ALARM$rawCode#"
            val msgCmdPrefix = "#MSG$rawCode"
            val statusCmd = "#STATUS$rawCode#"
            val timerCmd = "#TIMER$rawCode#"
            val sound1Cmd = "#SOUND1_$rawCode#"
            val sound2Cmd = "#SOUND2_$rawCode#"
            val sound3Cmd = "#SOUND3_$rawCode#"
            val sound4Cmd = "#SOUND4_$rawCode#"

            val isCommand = body.contains(locCmd, ignoreCase = true) ||
                    body.contains(lockCmd, ignoreCase = true) ||
                    body.contains(unlockCmd, ignoreCase = true) ||
                    body.contains(wipeCmd, ignoreCase = true) ||
                    body.contains(alarmCmd, ignoreCase = true) ||
                    body.uppercase().contains(msgCmdPrefix.uppercase()) ||
                    body.contains(statusCmd, ignoreCase = true) ||
                    body.contains(timerCmd, ignoreCase = true) ||
                    body.contains(sound1Cmd, ignoreCase = true) ||
                    body.contains(sound2Cmd, ignoreCase = true) ||
                    body.contains(sound3Cmd, ignoreCase = true) ||
                    body.contains(sound4Cmd, ignoreCase = true)

            if (isCommand) {
                // Abort broadcast synchronously on main thread
                try { abortBroadcast() } catch (e: Exception) {}
                prefs.lastProcessedSmsKey = smsUniqueKey
                commandsToExecute.add(CommandItem(sender, body))
            }
        }

        if (commandsToExecute.isEmpty()) return

        // Use goAsync to keep broadcast alive during asynchronous coroutine execution
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (item in commandsToExecute) {
                    processIncomingCommand(context, prefs, rawCode, item.sender, item.body)
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error during SMS command execution", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private data class CommandItem(val sender: String, val body: String)

    private suspend fun processIncomingCommand(
        context: Context,
        prefs: GuardianPrefs,
        rawCode: String,
        sender: String,
        body: String
    ) {
        val locCmd = "#LOC$rawCode#"
        val lockCmd = "#LOCK$rawCode#"
        val unlockCmd = "#UNLOCK$rawCode#"
        val wipeCmd = "#WIPE$rawCode#"
        val alarmCmd = "#ALARM$rawCode#"
        val msgCmdPrefix = "#MSG$rawCode"
        val statusCmd = "#STATUS$rawCode#"
        val timerCmd = "#TIMER$rawCode#"
        val photoCmd = "#PHOTO$rawCode#"
        val sound1Cmd = "#SOUND1_$rawCode#"
        val sound2Cmd = "#SOUND2_$rawCode#"
        val sound3Cmd = "#SOUND3_$rawCode#"
        val sound4Cmd = "#SOUND4_$rawCode#"

        when {
            body.contains(locCmd, ignoreCase = true) -> {
                handleLocationCommand(context, prefs, sender)
            }
            body.contains(lockCmd, ignoreCase = true) -> {
                handleLockCommand(context, prefs, sender, prefs.lockScreenMessage)
            }
            body.contains(unlockCmd, ignoreCase = true) -> {
                handleUnlockCommand(context, prefs, sender)
            }
            body.contains(wipeCmd, ignoreCase = true) -> {
                handleWipeCommand(context, prefs, sender)
            }
            body.contains(photoCmd, ignoreCase = true) -> {
                handlePhotoCommand(context, prefs, sender)
            }
            body.contains(alarmCmd, ignoreCase = true) -> {
                handleAlarmCommand(context, prefs, sender)
            }
            body.uppercase().contains(msgCmdPrefix.uppercase()) -> {
                val startIndex = body.uppercase().indexOf(msgCmdPrefix.uppercase()) + msgCmdPrefix.length
                var customMsg = body.substring(startIndex).trim()
                if (customMsg.startsWith("#")) customMsg = customMsg.substring(1).trim()
                if (customMsg.endsWith("#")) customMsg = customMsg.substring(0, customMsg.length - 1).trim()
                if (customMsg.startsWith("[")) customMsg = customMsg.substring(1).trim()
                if (customMsg.endsWith("]")) customMsg = customMsg.substring(0, customMsg.length - 1).trim()
                handleLockCommand(context, prefs, sender, customMsg, isCustomMsg = true)
            }
            body.contains(statusCmd, ignoreCase = true) -> {
                handleStatusCommand(context, prefs, sender)
            }
            body.contains(timerCmd, ignoreCase = true) -> {
                handleTimerCommand(context, prefs, sender)
            }
            body.contains(sound1Cmd, ignoreCase = true) -> {
                handleSoundCommand(context, prefs, sender, 1)
            }
            body.contains(sound2Cmd, ignoreCase = true) -> {
                handleSoundCommand(context, prefs, sender, 2)
            }
            body.contains(sound3Cmd, ignoreCase = true) -> {
                handleSoundCommand(context, prefs, sender, 3)
            }
            body.contains(sound4Cmd, ignoreCase = true) -> {
                handleSoundCommand(context, prefs, sender, 4)
            }
        }
    }

    private suspend fun handleLocationCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            val timeFarsi = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val locResult = LocationTracker.getOfflineLocationData(context)
            
            val locResponse = "گزارش مکان | ساعت: $timeFarsi\n" + locResult.toSmsString()
            
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "LOC",
                    description = "دستور موقعیت مکانی از راه دور اجرا شد",
                    location = if (locResult.isGPS) "${locResult.lat},${locResult.lng}" else null,
                    details = "ارسال شده به: $sender"
                ))
            } catch (dbEx: Exception) {
                Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            sendDirectSms(context, sender, locResponse, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleLocationCommand", e)
        }
    }

    private suspend fun handleLockCommand(
        context: Context,
        prefs: GuardianPrefs,
        sender: String,
        message: String,
        isCustomMsg: Boolean = false
    ) {
        try {
            val db = AppDatabase.getDatabase(context)
            
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "SMS_CMD",
                    description = if (isCustomMsg) "دستور نمایش پیام به سارق اجرا شد" else "دستور قفل امنیتی از راه دور اجرا شد",
                    details = "ارسال شده از: $sender | پیام: $message"
                ))
            } catch (dbEx: Exception) {
                Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            // If device admin is active, immediately lock the hardware screen
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            val adminComponent = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.lockNow()
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to lock device physically: ${e.message}")
                }
            }

            // Launch lock overlay Activity via FullScreenIntent & direct launch
            displayLockScreenOverlay(context, if (isCustomMsg) "message" else "lock", message)

            // Start alarm sirens automatically on lock trigger
            val serviceIntent = Intent(context, GuardianService::class.java)
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (ex: Exception) {
                context.startService(serviceIntent)
            }
            
            val response = "دستگاه با موفقیت قفل شد."
            sendDirectSms(context, sender, response, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleLockCommand", e)
        }
    }

    private suspend fun handleUnlockCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.logDao().insertLog(LogEntry(
                eventType = "SMS_CMD",
                description = "دستور بازگشایی از راه دور اجرا شد 🔓",
                details = "ارسال شده از: $sender"
            ))

            // Broadcast intent to unlock the GuardianLockActivity overlay
            val unlockIntent = Intent("com.example.action.REMOTE_UNLOCK")
            context.sendBroadcast(unlockIntent)

            // Stop alarms
            val serviceIntent = Intent(context, GuardianService::class.java).apply {
                putExtra("STOP_ALARM", true)
            }
            context.startService(serviceIntent)

            val response = "قفل امنیتی دستگاه با موفقیت از راه دور بازگشایی شد."
            sendDirectSms(context, sender, response, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleUnlockCommand", e)
        }
    }

    private suspend fun handleWipeCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "WIPE",
                    description = "دستور حذف کامل اطلاعات و بازنشانی کارخانه صادر شد ⚠️",
                    details = "درخواست کننده: $sender"
                ))
            } catch (dbEx: Exception) {
                Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            // Send notification SMS first before reboot/wipe begins
            val response = "فرمان پاکسازی امنیتی و بازنشانی کارخانه (Factory Reset) دریافت شد و در حال اجرا می‌باشد..."
            sendDirectSms(context, sender, response, prefs)

            // Small delay to ensure SMS radio dispatches the packet
            kotlinx.coroutines.delay(1000)

            // Execute deep erase & factory reset
            RemoteWipeHelper.wipeAllData(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleWipeCommand", e)
        }
    }

    private suspend fun handlePhotoCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "PHOTO",
                    description = "دستور عکاسی مخفیانه از سارق اجرا شد 📸",
                    details = "درخواست‌کننده: $sender"
                ))
            } catch (dbEx: Exception) {
                Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            CameraHelper.captureIntruderPhoto(context)

            val response = "فرمان عکاسی مخفیانه صادر شد. تصویر سارق با دوربین جلو ثبت و در حافظه امن ذخیره گردید."
            sendDirectSms(context, sender, response, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handlePhotoCommand", e)
        }
    }

    private suspend fun handleAlarmCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "ALARM",
                    description = "دستور فعالسازی آژیر امنیتی متوقف‌نشدنی",
                    details = "شماره ارسال‌کننده: $sender"
                ))
            } catch (dbEx: Exception) {
                 Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            // Start foreground system alarm immediately
            val serviceIntent = Intent(context, GuardianService::class.java)
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (ex: Exception) {
                context.startService(serviceIntent)
            }
            
            try {
                val triggerIntent = Intent(context, GuardianService::class.java).apply {
                    putExtra("START_ALARM", true)
                }
                ContextCompat.startForegroundService(context, triggerIntent)
            } catch (e: Exception) {
                // Fail-safe
            }

            val response = "سیستم هشدار فعال شد."
            sendDirectSms(context, sender, response, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleAlarmCommand", e)
        }
    }

    private suspend fun handleStatusCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            val timeFarsi = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val locResult = LocationTracker.getOfflineLocationData(context)
            
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
            
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            var operator = tm?.networkOperatorName ?: "نامشخص"
            if (operator.equals("IRANCELL", ignoreCase = true) || operator.equals("MTNIrancell", ignoreCase = true)) {
                operator = "ایرانسل"
            } else if (operator.equals("MCI", ignoreCase = true) || operator.equals("TCI", ignoreCase = true) || operator.contains("Hamrah", ignoreCase = true)) {
                operator = "همراه اول"
            } else if (operator.equals("Rightel", ignoreCase = true)) {
                operator = "رایتل"
            }

            val flatLat = if (locResult.isGPS) String.format("%.5f", locResult.lat) else ""
            val flatLng = if (locResult.isGPS) String.format("%.5f", locResult.lng) else ""
            val locText = if (locResult.isGPS) "$flatLat , $flatLng" else "خاموش"

            val statusText = "وضعیت سیستم:\n" +
                    "باتری: $batteryLevel%\n" +
                    "سیم‌کارت: $operator\n" +
                    "مکان: $locText\n" +
                    "زمان: $timeFarsi\n" +
                    "شارژر: ${if (bm?.isCharging == true) "متصل" else "قطع"}"

            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "LOC",
                    description = "دستور وضعیت سیستم دریافت شد",
                    details = "پاسخ به: $sender"
                ))
            } catch (dbEx: Exception) {
                 Log.e("SmsReceiver", "Database log failed", dbEx)
            }

            sendDirectSms(context, sender, statusText, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleStatusCommand", e)
        }
    }

    private suspend fun handleTimerCommand(context: Context, prefs: GuardianPrefs, sender: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.logDao().insertLog(LogEntry(
                eventType = "SMS_CMD",
                description = "دستور شمارش معکوس پلیس فعال شد",
                details = "درخواست از: $sender"
            ))

            // Launch countdown activity overlay via FullScreenIntent & direct launch
            displayLockScreenOverlay(context, "timer", "")

            val response = "شمارش معکوس پلیس روی گوشی فعال شد."
            sendDirectSms(context, sender, response, prefs)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleTimerCommand", e)
        }
    }

    private suspend fun handleSoundCommand(context: Context, prefs: GuardianPrefs, sender: String, slot: Int) {
        try {
            val db = AppDatabase.getDatabase(context)
            
            db.logDao().insertLog(LogEntry(
                eventType = "ALARM",
                description = "دستور پخش صدای ضبط‌شده اسلات $slot اجرا شد",
                details = "شماره ارسال‌کننده: $sender"
            ))

            if (AudioManagerHelper.isSlotRecorded(context, slot)) {
                AudioManagerHelper.startPlayback(context, slot)
                val response = "صدای ضبط شده شماره $slot پخش شد."
                sendDirectSms(context, sender, response, prefs)
            } else {
                val response = "خطا: فایل صدای فرستاده ضبط نشده است."
                sendDirectSms(context, sender, response, prefs)
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error in handleSoundCommand", e)
        }
    }

    /**
     * Reliable lock overlay display bypassing Android 10+ background activity limits.
     * Uses High-Priority FullScreenIntent Notification combined with direct startActivity.
     */
    private fun displayLockScreenOverlay(
        context: Context,
        overlayType: String,
        message: String
    ) {
        try {
            val lockIntent = Intent(context, GuardianLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(GuardianLockActivity.EXTRA_OVERLAY_TYPE, overlayType)
                putExtra(GuardianLockActivity.EXTRA_MESSAGE, message)
            }

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                1001,
                lockIntent,
                pendingFlags
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channelId = "guardian_lock_alert_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "هشدارهای تمام‌صفحه و قفل امنیتی گاردین",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "نمایش تمام‌صفحه صفحه قفل و اخطار پلیس هنگام دریافت پیامک امنیتی"
                        enableLights(true)
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("هشدار امنیتی گاردین 🛡️")
                    .setContentText(if (overlayType == "timer") "شمارش معکوس هشدار پلیس فعال شد" else "دستگاه از راه دور قفل شد")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .build()

                notificationManager.notify(9001, notification)
            }

            // Also attempt direct activity start in case overlay permissions are active
            context.startActivity(lockIntent)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error displaying lock screen overlay: ${e.message}", e)
        }
    }

    private fun sanitizePhoneNumberForFiltering(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], (i + 48).toChar())
            result = result.replace(arabicDigits[i], (i + 48).toChar())
        }
        return result.filter { it.isDigit() }.trim()
    }

    private fun sendDirectSms(context: Context, to: String, text: String, prefs: GuardianPrefs) {
        try {
            val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            var sanitizedTo = to
            for (i in 0..9) {
                sanitizedTo = sanitizedTo.replace(persianDigits[i], (i + 48).toChar())
                sanitizedTo = sanitizedTo.replace(arabicDigits[i], (i + 48).toChar())
            }
            sanitizedTo = sanitizedTo.filter { it.isDigit() || it == '+' }.trim()

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

            if (smsManager == null) {
                throw IllegalStateException("موتور ارسال پیامک SmsManager در دسترس نیست.")
            }
            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(sanitizedTo, null, text, null, null)
            }
            prefs.incrementSmsSentCount()
        } catch (e: Exception) {
            Log.e("SmsReceiver", "SMS fails: $e")
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.logDao().insertLog(LogEntry(
                        eventType = "ERROR",
                        description = "خطا در ارسال پاسخ پیامکی به $to ❌",
                        details = "متن پیام: $text\nعلت خطا: ${e.localizedMessage ?: "ناشناس"}"
                    ))
                } catch (dbEx: Exception) {
                     // Database write fail-safe
                }
            }
        }
    }
}
