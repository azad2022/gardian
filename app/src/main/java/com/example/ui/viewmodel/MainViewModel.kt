package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.data.repository.LogRepository
import com.example.service.GuardianService
import com.example.utils.AudioManagerHelper
import com.example.utils.JalaliCalendarHelper
import com.example.utils.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = GuardianPrefs(context)
    private val db = AppDatabase.getDatabase(context)
    private val repository = LogRepository(db.logDao())

    // Onboarding process state: Steps 1 to 7
    private val _onboardingStep = MutableStateFlow(1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    // Screen Tabs navigation state: "home", "settings", "logs", "guide"
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Filters for logs: "ALL", "SIM_CHANGE", "SMS_CMD", "ALARM", "LOC", "USB", "SHUTDOWN", "POWER", "BT_GUARDIAN"
    private val _activeLogFilter = MutableStateFlow("ALL")
    val activeLogFilter: StateFlow<String> = _activeLogFilter.asStateFlow()

    // Observe logs reactively based on filter selection
    val eventLogs: StateFlow<List<LogEntry>> = _activeLogFilter.flatMapLatest { filter ->
        if (filter == "ALL") {
            repository.allLogs
        } else {
            repository.getLogsByType(filter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // App live parameters
    private val _isOnboarded = MutableStateFlow(prefs.isOnboarded)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _isArmed = MutableStateFlow(prefs.isArmed)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _lastEventMessage = MutableStateFlow(GuardianService.lastEventLive)
    val lastEventMessage: StateFlow<String> = _lastEventMessage.asStateFlow()

    // Config parameters mapped from prefs
    private val _emergencyNumber = MutableStateFlow(prefs.emergencyNumber)
    val emergencyNumber: StateFlow<String> = _emergencyNumber.asStateFlow()

    private val _secretSmsCode = MutableStateFlow(prefs.rawSecretCode)
    val secretSmsCode: StateFlow<String> = _secretSmsCode.asStateFlow()

    private val _decoyPin = MutableStateFlow(prefs.decoyPin)
    val decoyPin: StateFlow<String> = _decoyPin.asStateFlow()

    private val _currentMode = MutableStateFlow(prefs.currentMode)
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    private val _simDetectEnabled = MutableStateFlow(prefs.isSimChangeDetectionEnabled)
    val simDetectEnabled: StateFlow<Boolean> = _simDetectEnabled.asStateFlow()

    private val _usbAlertEnabled = MutableStateFlow(prefs.isUsbAlertEnabled)
    val usbAlertEnabled: StateFlow<Boolean> = _usbAlertEnabled.asStateFlow()

    private val _chargerAlertEnabled = MutableStateFlow(prefs.isChargerAlertEnabled)
    val chargerAlertEnabled: StateFlow<Boolean> = _chargerAlertEnabled.asStateFlow()

    private val _btGuardianEnabled = MutableStateFlow(prefs.isBluetoothGuardianEnabled)
    val btGuardianEnabled: StateFlow<Boolean> = _btGuardianEnabled.asStateFlow()

    private val _isAppLockerEnabled = MutableStateFlow(prefs.isAppLockerEnabled)
    val isAppLockerEnabled: StateFlow<Boolean> = _isAppLockerEnabled.asStateFlow()

    private val _isDeviceLockEnabled = MutableStateFlow(prefs.isDeviceLockEnabled)
    val isDeviceLockEnabled: StateFlow<Boolean> = _isDeviceLockEnabled.asStateFlow()

    private val _isIntruderSelfieEnabled = MutableStateFlow(prefs.isIntruderSelfieEnabled)
    val isIntruderSelfieEnabled: StateFlow<Boolean> = _isIntruderSelfieEnabled.asStateFlow()

    private val _btDeviceName = MutableStateFlow(prefs.bluetoothDeviceName)
    val btDeviceName: StateFlow<String> = _btDeviceName.asStateFlow()

    private val _btThreshold = MutableStateFlow(prefs.bluetoothThresholdMinutes)
    val btThreshold: StateFlow<Int> = _btThreshold.asStateFlow()

    private val _heartbeatInterval = MutableStateFlow(prefs.heartbeatIntervalHours)
    val heartbeatInterval: StateFlow<Int> = _heartbeatInterval.asStateFlow()

    private val _lockScreenMessage = MutableStateFlow(prefs.lockScreenMessage)
    val lockScreenMessage: StateFlow<String> = _lockScreenMessage.asStateFlow()

    private val _smsSentCount = MutableStateFlow(prefs.smsSentCount)
    val smsSentCount: StateFlow<Int> = _smsSentCount.asStateFlow()

    private val _daysProtected = MutableStateFlow(prefs.daysProtected)
    val daysProtected: StateFlow<Int> = _daysProtected.asStateFlow()

    private val _lastLocationTimeStr = MutableStateFlow(
        if (prefs.lastLocationTime == 0L) "تاکنون ثبت نشده" 
        else JalaliCalendarHelper.getJalaliDateString(prefs.lastLocationTime)
    )
    val lastLocationTimeStr: StateFlow<String> = _lastLocationTimeStr.asStateFlow()

    // Tracking for recorded sound slots in Settings
    private val _recordedSlots = MutableStateFlow(getRecordedSlotsMap())
    val recordedSlots: StateFlow<Map<Int, Boolean>> = _recordedSlots.asStateFlow()

    // Active playing/recording feedback
    var activeRecordingSlot = MutableStateFlow<Int?>(null)
    var activePlaybackSlot = MutableStateFlow<Int?>(null)

    init {
        if (prefs.firstInstallTime == 0L) {
            prefs.firstInstallTime = System.currentTimeMillis()
        }
        _daysProtected.value = prefs.daysProtected
        // Log first installation stats if newly completed
        viewModelScope.launch {
            _lastEventMessage.value = GuardianService.lastEventLive
        }
    }

    fun updateActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun setLogFilter(filter: String) {
        _activeLogFilter.value = filter
    }

    // ONBOARDING HANDLERS
    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step
    }

    fun submitEmergencyNumberOnSetup(number: String): Boolean {
        if (number.trim().length >= 8) {
            prefs.emergencyNumber = number.trim()
            _emergencyNumber.value = number.trim()
            return true
        }
        return false
    }

    fun submitSecretSmsCodeOnSetup(code: String): Boolean {
        if (code.trim().length == 4) {
            prefs.secretSmsCode = code.trim() //SHA-256 in setter
            prefs.rawSecretCode = code.trim() // store clear prefix for SMS matching
            _secretSmsCode.value = code.trim()
            return true
        }
        return false
    }

    fun submitDecoyPinOnSetup(pin: String): Boolean {
        if (pin.trim().isNotEmpty() && pin.trim() != prefs.rawSecretCode) {
            prefs.decoyPin = pin.trim()
            _decoyPin.value = pin.trim()
            return true
        }
        return false
    }

    fun completeOnboardingAndActivate() {
        if (prefs.firstInstallTime == 0L) {
            prefs.firstInstallTime = System.currentTimeMillis()
        }
        _daysProtected.value = prefs.daysProtected
        prefs.isOnboarded = true
        prefs.isArmed = true
        _isOnboarded.value = true
        _isArmed.value = true
        
        // Save initial Room log
        viewModelScope.launch {
            repository.insertLog(LogEntry(
                eventType = "ALARM",
                description = "کاربر از مرحله راه‌اندازی وارد برنامه شد",
                details = "وضعیت مجوزها و قابلیت‌های امنیتی باید جداگانه در داشبورد بررسی شود."
            ))
        }

        // Start background watcher service
        try {
            val serviceIntent = Intent(context, GuardianService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to start service: ${e.message}")
        }
    }

    // ARMED TOGGLE
    fun toggleGuardianArmed() {
        val newState = !prefs.isArmed
        prefs.isArmed = newState
        _isArmed.value = newState
        GuardianService.isSystemArmedLive = newState

        viewModelScope.launch {
            repository.insertLog(LogEntry(
                eventType = "ALARM",
                description = if (newState) "سامانه ضد سرقت گاردین مسلح شد 🛡️" else "سامانه غیرفعال شد ⚠️",
                details = if (newState) {
                    "مسلح‌سازی فعال شد؛ وضعیت مجوزها و قابلیت‌های پیکربندی‌شده را جداگانه بررسی کنید."
                } else {
                    "مسلح‌سازی غیرفعال شد."
                }
            ))
            _lastEventMessage.value = if (newState) "گاردین فعال است" else "غیرفعال"
        }

        val serviceIntent = Intent(context, GuardianService::class.java)
        if (newState) {
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to start service: ${e.message}")
            }
        } else {
            // Stop service or stop active sounds
            try {
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateCurrentMode(mode: String) {
        prefs.currentMode = mode
        _currentMode.value = mode
        viewModelScope.launch {
            repository.insertLog(LogEntry(
                eventType = "ALARM",
                description = "حالت محافظت به ${getLocalizedModeName(mode)} تغییر یافت"
            ))
        }
    }

    private fun getLocalizedModeName(mode: String): String {
        return when (mode) {
            "cafe" -> "کافه"
            "pocket" -> "جیبی"
            "charging" -> "شارژر"
            else -> "کیف"
        }
    }

    // SETTINGS SAVED CONFIG ACTIONS
    fun setEmergencyNumber(num: String) {
        prefs.emergencyNumber = num.trim()
        _emergencyNumber.value = num.trim()
    }

    fun setSecretSmsCode(code: String) {
        if (code.trim().length == 4) {
            prefs.secretSmsCode = code.trim()
            prefs.rawSecretCode = code.trim()
            _secretSmsCode.value = code.trim()
        }
    }

    fun setDecoyPin(pin: String) {
        prefs.decoyPin = pin.trim()
        _decoyPin.value = pin.trim()
    }

    fun setSimDetectEnabled(enabled: Boolean) {
        prefs.isSimChangeDetectionEnabled = enabled
        _simDetectEnabled.value = enabled
    }

    fun setUsbAlertEnabled(enabled: Boolean) {
        prefs.isUsbAlertEnabled = enabled
        _usbAlertEnabled.value = enabled
    }

    fun setChargerAlertEnabled(enabled: Boolean) {
        prefs.isChargerAlertEnabled = enabled
        _chargerAlertEnabled.value = enabled
    }

    fun setAppLockerEnabled(enabled: Boolean) {
        prefs.isAppLockerEnabled = enabled
        _isAppLockerEnabled.value = enabled
    }

    fun setDeviceLockEnabled(enabled: Boolean) {
        prefs.isDeviceLockEnabled = enabled
        _isDeviceLockEnabled.value = enabled
    }

    fun setIntruderSelfieEnabled(enabled: Boolean) {
        prefs.isIntruderSelfieEnabled = enabled
        _isIntruderSelfieEnabled.value = enabled
    }

    fun setBtGuardianEnabled(enabled: Boolean) {
        prefs.isBluetoothGuardianEnabled = enabled
        _btGuardianEnabled.value = enabled
    }

    fun setBtDevice(name: String, address: String) {
        prefs.bluetoothDeviceName = name
        prefs.bluetoothDeviceAddress = address
        _btDeviceName.value = name
    }

    fun setBtThreshold(mins: Int) {
        prefs.bluetoothThresholdMinutes = mins
        _btThreshold.value = mins
    }

    fun setHeartbeatInterval(hours: Int) {
        prefs.heartbeatIntervalHours = hours
        _heartbeatInterval.value = hours
    }

    fun setLockScreenMessage(msg: String) {
        prefs.lockScreenMessage = msg
        _lockScreenMessage.value = msg
    }

    // AUDIO SLOT RECORD CONTROLLERS
    private fun getRecordedSlotsMap(): Map<Int, Boolean> {
        return mapOf(
            1 to AudioManagerHelper.isSlotRecorded(context, 1),
            2 to AudioManagerHelper.isSlotRecorded(context, 2),
            3 to AudioManagerHelper.isSlotRecorded(context, 3),
            4 to AudioManagerHelper.isSlotRecorded(context, 4)
        )
    }

    fun startRecordingSound(slot: Int) {
        viewModelScope.launch {
            try {
                AudioManagerHelper.startRecording(context, slot)
                activeRecordingSlot.value = slot
            } catch (e: Exception) {
                // Handle missing mic permissions gracefully
            }
        }
    }

    fun stopRecordingSound() {
        AudioManagerHelper.stopRecording()
        activeRecordingSlot.value = null
        _recordedSlots.value = getRecordedSlotsMap()
    }

    fun startPlaybackSound(slot: Int) {
        activePlaybackSlot.value = slot
        AudioManagerHelper.startPlayback(context, slot) {
            activePlaybackSlot.value = null
        }
    }

    fun stopPlaybackSound() {
        AudioManagerHelper.stopPlayback()
        activePlaybackSlot.value = null
    }

    fun deleteSoundSlot(slot: Int) {
        AudioManagerHelper.deleteSlot(context, slot)
        _recordedSlots.value = getRecordedSlotsMap()
    }

    // TEST SMS GENERATOR
    fun fireTestSms() {
        val number = prefs.emergencyNumber
        if (number.isEmpty()) {
            viewModelScope.launch {
                repository.insertLog(LogEntry(
                    eventType = "SMS_CMD",
                    description = "خطا: پیامک تست ارسال نشد ⚠️",
                    details = "دلیل: شماره اضطراری مالک هنوز تعریف نشده است! لطفاً ابتدا به زبانه 'تنظیمات' بروید و شماره تماس گیرنده را ذخیره کنید."
                ))
            }
            return
        }
        
        viewModelScope.launch {
            val farsiTime = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
            val alertTestMsg = "پیامک تست سیستم امنیتی گاردین\nسیستم شما با موفقیت پیکربندی شده و آماده کار است.\nزمان ارسال: $farsiTime"
            
            try {
                // Standardize phone number to English digits only
                val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
                val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
                var sanitizedTo = number
                for (i in 0..9) {
                    sanitizedTo = sanitizedTo.replace(persianDigits[i], (i + 48).toChar())
                    sanitizedTo = sanitizedTo.replace(arabicDigits[i], (i + 48).toChar())
                }
                sanitizedTo = sanitizedTo.filter { it.isDigit() || it == '+' }.trim()

                val smsManager: SmsManager = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                        val subId = android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                        if (subId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            @Suppress("DEPRECATION")
                            SmsManager.getSmsManagerForSubscriptionId(subId)
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                }

                if (smsManager == null) {
                    throw IllegalStateException("موتور ارسال پیامک (SmsManager) در این دستگاه در دسترس نیست.")
                }
                val parts = smsManager.divideMessage(alertTestMsg)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(sanitizedTo, null, alertTestMsg, null, null)
                }

                prefs.incrementSmsSentCount()
                _smsSentCount.value = prefs.smsSentCount
                
                repository.insertLog(LogEntry(
                    eventType = "SMS_CMD",
                    description = "ارسال موفق پیامک تستی به شماره اضطراری ✅",
                    details = "شماره: $sanitizedTo | زمان: $farsiTime"
                ))
            } catch (e: Exception) {
                Log.e("MainViewModel", "Test SMS failed", e)
                repository.insertLog(LogEntry(
                    eventType = "SMS_CMD",
                    description = "خطا در ارسال فیزیکی پیامک تستی ❌",
                    details = "شماره تلاش شده: $number\nعلت خطا: ${e.localizedMessage ?: "محدودیت دسترسی یا سخت‌افزاری"}\nنکته: در شبیه‌سازها یا بدون سیم‌کارت فیزیکی معتبر با شارژ کافی، ارسال پیامک غیرممکن است اما منطق عملکردی برنامه کاملاً سالم است."
                ))
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun isDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.example.receiver.GuardianDeviceAdminReceiver::class.java)
        return dpm?.isAdminActive(adminComponent) == true
    }

    fun removeDeviceAdmin(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            val adminComponent = android.content.ComponentName(context, com.example.receiver.GuardianDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.removeActiveAdmin(adminComponent)
                viewModelScope.launch {
                    repository.insertLog(
                        LogEntry(
                            eventType = "POWER",
                            description = "دسترسی مدیر دستگاه توسط کاربر لغو شد (آماده حذف برنامه)",
                            details = "دستگاه از حالت حفاظت ضدحذف خارج شد"
                        )
                    )
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to remove device admin", e)
            false
        }
    }
}
