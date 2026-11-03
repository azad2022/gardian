package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.utils.AudioManagerHelper
import com.example.utils.JalaliCalendarHelper
import com.example.utils.LocationTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GuardianService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var simCheckJob: Job? = null
    private lateinit var prefs: GuardianPrefs
    private lateinit var db: AppDatabase

    private var vibrator: Vibrator? = null
    private var isAlarmRunning = false
    private var flashHandler: Handler? = null
    private var isFlashOn = false

    // Multi-receiver coordination
    private var hardwareAlertReceiver: BroadcastReceiver? = null
    private var bluetoothDisconnectTime: Long? = null
    private var btStateReceiver: BroadcastReceiver? = null
    private var serviceSmsReceiver: com.example.receiver.SmsReceiver? = null
    private var subscriptionsChangedListener: android.telephony.SubscriptionManager.OnSubscriptionsChangedListener? = null

    companion object {
        const val CHANNEL_ID = "guardian_service_channel"
        const val NOTIFICATION_ID = 1001
        
        // Command to trigger overlays
        const val ACTION_SHOW_LOCK_OVERLAY = "com.example.guardian.SHOW_LOCK"
        const val ACTION_SHOW_TIMER_OVERLAY = "com.example.guardian.SHOW_TIMER"
        const val EXTRA_LOCK_TEXT = "lock_text"

        // Local state indicators to feed live feedback to the Compose UI
        var isSystemArmedLive = false
        var lastEventLive = "آماده به کار"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = GuardianPrefs(this)
        db = AppDatabase.getDatabase(this)
        isSystemArmedLive = prefs.isArmed
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        registerHardwareMonitoring()
        registerBluetoothMonitoring()
        registerSimMonitoring()
        scheduleHeartbeatTimer()

        // Register SMS receiver dynamically for maximum dependability on consumer OEM devices
        try {
            serviceSmsReceiver = com.example.receiver.SmsReceiver()
            val smsFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED").apply {
                priority = 1000 // Give dynamic listener top priority
            }
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                serviceSmsReceiver!!,
                smsFilter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e("GuardianService", "Failed to register dynamic SMS receiver", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isSystemArmedLive = prefs.isArmed
        if (intent != null) {
            val stopAlarm = intent.getBooleanExtra("STOP_ALARM", false)
            if (stopAlarm) {
                stopAntiTheftAlarm()
            }
            val startAlarm = intent.getBooleanExtra("START_ALARM", false)
            if (startAlarm) {
                startAntiTheftAlarm()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "محافظ ضد سرقت گاردین",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت محافظت سیستم امنیتی گاردین"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val statusText = if (prefs.isArmed) "محافظت در حال اجراست 🛡️" else "گاردین آماده فعال‌سازی است"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("سیستم امنیتی گاردین (سیستم سرویس)")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // SIM Card change monitoring (Boot & Dynamic Hot-Swap)
    private fun registerSimMonitoring() {
        if (!prefs.isSimChangeDetectionEnabled) return

        // 1. Initial check at startup
        checkSimChange("راه‌اندازی سیستم")

        // 2. Real-time Subscription listener for hot-swapping SIM cards
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val subManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                subscriptionsChangedListener = object : android.telephony.SubscriptionManager.OnSubscriptionsChangedListener() {
                    override fun onSubscriptionsChanged() {
                        super.onSubscriptionsChanged()
                        checkSimChange("تعویض بلادرنگ در حین کار")
                    }
                }
                subManager?.addOnSubscriptionsChangedListener(subscriptionsChangedListener)
            } catch (e: Exception) {
                Log.e("GuardianService", "Failed to addOnSubscriptionsChangedListener: ${e.message}")
            }
        }
    }

    private fun checkSimChange(source: String = "سیستم") {
        if (!prefs.isArmed || !prefs.isSimChangeDetectionEnabled) return

        simCheckJob?.cancel()
        simCheckJob = serviceScope.launch {
            try {
                // Short stabilization delay for radio network registration
                delay(3000)
                val simInfo = getCurrentSimFingerprint() ?: return@launch
                val (currentFingerprint, operatorName) = simInfo
                val originalFingerprint = prefs.lastSimIccid

                if (originalFingerprint.isNotEmpty() && originalFingerprint != currentFingerprint) {
                    // Alert of SIM card change!
                    triggerSimChangeAlert(currentFingerprint, operatorName, source)
                    // Update stored fingerprint to avoid recurring alerts for the new SIM
                    prefs.lastSimIccid = currentFingerprint
                } else if (originalFingerprint.isEmpty() && currentFingerprint.isNotEmpty()) {
                    // First time initialization: store current trusted SIM fingerprint
                    prefs.lastSimIccid = currentFingerprint
                }
            } catch (e: CancellationException) {
                // Expected when coroutine is cancelled for debouncing or service destruction
                Log.d("GuardianService", "SIM check job cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e("GuardianService", "Error during SIM check: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentSimFingerprint(): Pair<String, String>? {
        try {
            val hasPhonePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            // Modern Android: Use SubscriptionManager to get unique active SIM slots
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && hasPhonePermission) {
                val subManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                val activeList = subManager?.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    val fingerprint = StringBuilder()
                    val operatorNames = mutableListOf<String>()

                    for (info in activeList) {
                        val subId = info.subscriptionId
                        val slot = info.simSlotIndex
                        val carrier = info.carrierName?.toString()?.ifBlank { "اپراتور نامشخص" } ?: "اپراتور نامشخص"
                        operatorNames.add(carrier)

                        val cardId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            info.cardId.toString()
                        } else {
                            @Suppress("DEPRECATION")
                            info.iccId ?: "sub_$subId"
                        }
                        fingerprint.append("[$subId:$slot:$cardId:$carrier]")
                    }
                    return Pair(fingerprint.toString(), operatorNames.joinToString("، "))
                }
            }

            // Fallback for legacy devices or when SubscriptionManager list is unavailable
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null && telephonyManager.simState == TelephonyManager.SIM_STATE_READY) {
                val opName = telephonyManager.simOperatorName?.ifBlank { telephonyManager.networkOperatorName ?: "نامشخص" } ?: "نامشخص"
                val rawId = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && hasPhonePermission) {
                    @Suppress("DEPRECATION")
                    telephonyManager.simSerialNumber ?: opName
                } else {
                    "${opName}_${telephonyManager.simCountryIso}"
                }
                return Pair(rawId, opName)
            }
        } catch (e: Exception) {
            Log.e("GuardianService", "Failed to get SIM fingerprint: ${e.message}")
        }
        return null
    }

    private suspend fun triggerSimChangeAlert(newFingerprint: String, operatorName: String, triggerSource: String) {
        val timeFarsi = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
        val locData = LocationTracker.getOfflineLocationData(this@GuardianService)
        
        val locText = if (locData.isGPS) "${locData.lat},${locData.lng}" else "نامشخص (عدم اتصال GPS)"
        val alertMsg = "⚠️ هشدار گاردین: سیم‌کارت تعویض شد!\nاپراتور جدید: $operatorName\nزمان: $timeFarsi\nرویداد: $triggerSource\nموقعیت: $locText\nدکل: ${locData.cellTower}"

        db.logDao().insertLog(LogEntry(
            eventType = "SIM_CHANGE",
            description = "تعویض سیم‌کارت شناسایی شد: اپراتور $operatorName ($triggerSource)",
            location = locText,
            details = "مشخصه اثرانگشت سیم: $newFingerprint"
        ))

        lastEventLive = "تعویض سیم‌کارت گزارش شد"
        sendSecretSms(prefs.emergencyNumber, alertMsg)
    }

    // Dynamically register chargers, USB and shutdown hooks
    private fun registerHardwareMonitoring() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SHUTDOWN)
            addAction(Intent.ACTION_SCREEN_ON)
            @Suppress("DEPRECATION")
            addAction(Intent.ACTION_UMS_CONNECTED)
            addAction("android.hardware.usb.action.USB_STATE")
        }

        hardwareAlertReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!prefs.isArmed) return
                val action = intent.action ?: return

                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        if (prefs.isChargerAlertEnabled) {
                            handleChargerConnected()
                        }
                    }
                    Intent.ACTION_SHUTDOWN -> {
                        handleShutdownDetected()
                    }
                    "android.hardware.usb.action.USB_STATE" -> {
                        val connected = intent.extras?.getBoolean("connected") ?: false
                        if (connected && prefs.isUsbAlertEnabled) {
                            handleUsbAttached()
                        }
                    }
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            hardwareAlertReceiver!!,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun handleChargerConnected() {
        serviceScope.launch {
            val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val locData = LocationTracker.getOfflineLocationData(this@GuardianService)
            
            val locText = if (locData.isGPS) "N:${locData.lat}, E:${locData.lng}" else "نامشخص"
            val alertMsg = "گوشی به شارژر متصل شد\nموقعیت: $locText\nزمان: $farsiTime"

            db.logDao().insertLog(LogEntry(
                eventType = "POWER",
                description = "اتصال به شارژر متصل شد",
                location = locText
            ))

            lastEventLive = "اتصال به شارژر ثبت شد"
            sendSecretSms(prefs.emergencyNumber, alertMsg)
        }
    }

    private fun handleUsbAttached() {
        serviceScope.launch {
            if (isAlarmRunning) return@launch // Avoid rapid re-triggering

            val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val locData = LocationTracker.getOfflineLocationData(this@GuardianService)
            val locText = if (locData.isGPS) "N:${locData.lat}, E:${locData.lng}" else "نامشخص"
            
            val alertMsg = "اتصال USB برقرار شد\nهشدار انتقال داده\nموقعیت: $locText\nزمان: $farsiTime"

            db.logDao().insertLog(LogEntry(
                eventType = "USB",
                description = "اتصال کابل USB شناسایی شد - آغاز آلارم",
                location = locText
            ))

            lastEventLive = "اتصال USB ناشناس شناسایی شد"
            sendSecretSms(prefs.emergencyNumber, alertMsg)
            
            // Activate immediate warning sirens
            startAntiTheftAlarm()
        }
    }

    private fun handleShutdownDetected() {
        serviceScope.launch {
            val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0

            val alertMsg = "گوشی در حال خاموش شدن است\nزمان: $farsiTime\nباتری: $level درصد"
            
            try {
                db.logDao().insertLog(LogEntry(
                    eventType = "SHUTDOWN",
                    description = "تلاش برای خاموش کردن گوشی توسط سارق",
                    details = "شارژ باتری: %$level"
                ))
            } catch (e: Exception) {
                // Fail-safe
            }

            sendSecretSms(prefs.emergencyNumber, alertMsg)
        }
    }

    // Bluetooth watchguard disconnect listener
    private fun registerBluetoothMonitoring() {
        val filter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
        }

        btStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!prefs.isArmed || !prefs.isBluetoothGuardianEnabled) return
                val action = intent.action ?: return
                
                val deviceName = prefs.bluetoothDeviceName
                val deviceAddress = prefs.bluetoothDeviceAddress
                
                val extraDeviceName = intent.getStringExtra("android.bluetooth.device.extra.DEVICE") ?: ""
                val address = intent.getParcelableExtra<android.bluetooth.BluetoothDevice>("android.bluetooth.device.extra.DEVICE")?.address ?: ""

                if (action == "android.bluetooth.device.action.ACL_DISCONNECTED") {
                    if (address == deviceAddress || extraDeviceName.contains(deviceName, ignoreCase = true)) {
                        // Start watch countdown
                        bluetoothDisconnectTime = System.currentTimeMillis()
                        startBluetoothRangeCountdown()
                    }
                } else if (action == "android.bluetooth.device.action.ACL_CONNECTED") {
                    if (address == deviceAddress || extraDeviceName.contains(deviceName, ignoreCase = true)) {
                        // Cancel watch countdown
                        bluetoothDisconnectTime = null
                    }
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            btStateReceiver!!,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun startBluetoothRangeCountdown() {
        serviceScope.launch {
            val limitMillis = prefs.bluetoothThresholdMinutes * 60 * 1000
            delay(1000) // Small safety sleep
            
            while (bluetoothDisconnectTime != null) {
                val elapsed = System.currentTimeMillis() - (bluetoothDisconnectTime ?: break)
                if (elapsed >= limitMillis) {
                    // Timeout hit! Play alarm and warn emergency
                    triggerBluetoothDisconnectAlert()
                    break
                }
                delay(10000) // check every 10 seconds
            }
        }
    }

    private suspend fun triggerBluetoothDisconnectAlert() {
        if (isAlarmRunning) return
        
        val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
        val locData = LocationTracker.getOfflineLocationData(this@GuardianService)
        val locText = if (locData.isGPS) "N:${locData.lat}, E:${locData.lng}" else "نامشخص"

        val alertMsg = "قطع اتصال بلوتوث محافظ گاردین\nنام دستگاه: ${prefs.bluetoothDeviceName}\nزمان قطع: $farsiTime\nموقعیت: $locText"

        db.logDao().insertLog(LogEntry(
            eventType = "BT_GUARDIAN",
            description = "قطع ارتباط با بلوتوث گاردین (${prefs.bluetoothDeviceName}) بیش از حد مجاز",
            location = locText
        ))

        lastEventLive = "قطع اتصال بلوتوث محافظ"
        sendSecretSms(prefs.emergencyNumber, alertMsg)
        
        // play alert immediately
        startAntiTheftAlarm()
    }

    // Heartbeat Timer generator
    private fun scheduleHeartbeatTimer() {
        serviceScope.launch {
            while (true) {
                val hours = prefs.heartbeatIntervalHours
                // Wait for the requested interval in hours
                delay(hours * 3600 * 1000L)
                
                if (prefs.isArmed) {
                    sendHeartbeatSms()
                }
            }
        }
    }

    private suspend fun sendHeartbeatSms() {
        val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
        val locData = LocationTracker.getOfflineLocationData(this)
        val locText = if (locData.isGPS) "N:${locData.lat}, E:${locData.lng}" else "نامشخص"
        
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0

        val alertMsg = "گوشی آنلاین است (سیگنال حیات)\nزمان: $farsiTime\nباتری: $level درصد\nموقعیت: $locText"

        db.logDao().insertLog(LogEntry(
            eventType = "HEARTBEAT",
            description = "ارسال سیگنال حیات دوره‌ای (Heartbeat)",
            location = locText
        ))

        sendSecretSms(prefs.emergencyNumber, alertMsg)
    }

    // Start anti-theft siren with blinking flash, high-pattern vibration and max volume sound
    fun startAntiTheftAlarm() {
        if (isAlarmRunning) return
        isAlarmRunning = true

        // Play high alarm sound (Stream Alarm keeps running under DND)
        AudioManagerHelper.playSystemAlarm(this)

        // Trigger massive physical vibration pattern
        serviceScope.launch {
            while (isAlarmRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 800, 300, 800)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 800, 300, 800), -1)
                }
                delay(2000)
            }
        }

        // Trigger flash blinking camera manager torch
        startBlinkingFlash()
    }

    fun stopAntiTheftAlarm() {
        isAlarmRunning = false
        AudioManagerHelper.stopPlayback()
        vibrator?.cancel()
        stopBlinkingFlash()
    }

    private fun startBlinkingFlash() {
        flashHandler = Handler(Looper.getMainLooper())
        val blinkRunnable = object : Runnable {
            override fun run() {
                if (!isAlarmRunning) {
                    setFlashlight(false)
                    return
                }
                isFlashOn = !isFlashOn
                setFlashlight(isFlashOn)
                flashHandler?.postDelayed(this, 300)
            }
        }
        flashHandler?.post(blinkRunnable)
    }

    private fun stopBlinkingFlash() {
        flashHandler?.removeCallbacksAndMessages(null)
        flashHandler = null
        setFlashlight(false)
    }

    private fun setFlashlight(state: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, state)
            }
        } catch (e: Exception) {
            // Ignore if camera permission lacks or flash unavailable
        }
    }

    // Simple robust SmsManager helper
    fun sendSecretSms(to: String, text: String) {
        if (to.isEmpty()) {
            serviceScope.launch {
                db.logDao().insertLog(LogEntry(
                    eventType = "ERROR",
                    description = "خطا: شماره اضطراری مالک ثبت نشده است! ⚠️",
                    details = "سیستم مایل به ارسال پیامک هشدار بود ولی امکان ارسال به دلیل خالی بودن شماره اضطراری مقدور نشد."
                ))
            }
            return
        }
        if (text.isEmpty()) return
        try {
            // Standardize phone number to English digits only
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
                            this.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
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
                    this.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            if (smsManager == null) {
                throw IllegalStateException("موتور ارسال پیامک (SmsManager) در دسترس نیست.")
            }
            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(sanitizedTo, null, text, null, null)
            }
            prefs.incrementSmsSentCount()
        } catch (e: Exception) {
            Log.e("GuardianService", "SMS failed: $e")
            serviceScope.launch {
                db.logDao().insertLog(LogEntry(
                    eventType = "ERROR",
                    description = "خطا در ارسال فیزیکی پیامک امنیتی به $to ❌",
                    details = "علت خطا: ${e.localizedMessage ?: "محدودیت سخت‌افزاری یا مجوز پیامک"}\nپیام تلاش شده: $text"
                ))
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stopAntiTheftAlarm()

        // Unregister runtime state listeners
        try {
            hardwareAlertReceiver?.let { unregisterReceiver(it) }
            btStateReceiver?.let { unregisterReceiver(it) }
            serviceSmsReceiver?.let { unregisterReceiver(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                subscriptionsChangedListener?.let { subManager?.removeOnSubscriptionsChangedListener(it) }
            }
        } catch (e: Exception) {
            // Safe
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
