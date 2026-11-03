package com.example.ui.overlay

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.telephony.SmsManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.service.GuardianService
import com.example.utils.CameraHelper
import com.example.utils.JalaliCalendarHelper
import com.example.utils.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GuardianLockActivity : ComponentActivity() {

    private val unlockReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.action.REMOTE_UNLOCK") {
                val serviceIntent = Intent(this@GuardianLockActivity, GuardianService::class.java).apply {
                    putExtra("STOP_ALARM", true)
                }
                startService(serviceIntent)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_OVERLAY_TYPE = "overlay_type"
        const val EXTRA_MESSAGE = "msg_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register custom broadcast receiver for remote unlocking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                unlockReceiver,
                android.content.IntentFilter("com.example.action.REMOTE_UNLOCK"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                unlockReceiver,
                android.content.IntentFilter("com.example.action.REMOTE_UNLOCK")
            )
        }

        // Force layout over locked screen, secure overlay setup
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Prevent taking screenshots or showing in recents (security)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val overlayType = intent.getStringExtra(EXTRA_OVERLAY_TYPE) ?: "lock"
        val customMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: ""

        setContent {
            LockOverlayContent(
                type = overlayType,
                customMessage = customMessage,
                onUnlocked = {
                    // Turn off active alarms
                    val serviceIntent = Intent(this, GuardianService::class.java).apply {
                        putExtra("STOP_ALARM", true)
                    }
                    startService(serviceIntent)
                    finish()
                }
            )
        }
    }

    @Deprecated("Force lock mechanism, ignore back press")
    override fun onBackPressed() {
        // Blocks back navigation to keep device strictly locked
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}

@Composable
fun LockOverlayContent(
    type: String,
    customMessage: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { GuardianPrefs(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isPhoneDecoyUnlocked by remember { mutableStateOf(false) }

    // Countdown state for Police Timer
    var remainingSeconds by remember { mutableStateOf(600) } // 10 minutes (600 seconds)

    LaunchedEffect(type) {
        if (type == "timer") {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
        }
    }

    // Sound slots looping warning
    LaunchedEffect(true) {
        // Trigger high system alert loop periodically
        val serviceIntent = Intent(context, GuardianService::class.java)
        context.startService(serviceIntent)
    }

    // Helper for Persian numbers
    fun parseTimerText(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        val rawTime = String.format("%02d:%02d", mins, secs)
        return JalaliCalendarHelper.toPersianNumber(rawTime)
    }

    // Unlock handler
    val handleUnlockAttempt = {
        val code = enteredPin.trim()
        val rawSecret = prefs.rawSecretCode
        val rawDecoy = prefs.decoyPin

        when {
            code == rawSecret -> {
                // Legitimate unlock! Log event and close
                scope.launch {
                    db.logDao().insertLog(LogEntry(
                        eventType = "ALARM",
                        description = "امنیتی: قفل محافظت با موفقیت وارد شد",
                        details = "قفل گوشی با رمز صحیح باز شد"
                    ))
                }
                onUnlocked()
            }
            code == rawDecoy && rawDecoy.isNotEmpty() -> {
                // Decoy Unlock entered!
                // 1. Show fake empty launcher screen
                isPhoneDecoyUnlocked = true
                
                // 2. Secretly log coordinates and trigger silent alert SMS
                scope.launch {
                    val locResult = LocationTracker.getOfflineLocationData(context)
                    val pos = if (locResult.isGPS) "${locResult.lat},${locResult.lng}" else "نامشخص"
                    
                    db.logDao().insertLog(LogEntry(
                        eventType = "ALARM",
                        description = "سیستم هشدار: سارق رمز جعلی (PIN جعلی) را وارد کرد",
                        location = pos,
                        details = "گوشی وارد صفحه فریبنده شد"
                    ))

                    val timeFarsi = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
                    val alertSms = "سارق رمز جعلی وارد کرد (ورود جعلی)\nزمان: $timeFarsi\n" + locResult.toSmsString()
                    
                    try {
                        val rawNum = prefs.emergencyNumber
                        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
                        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
                        var sanitizedTo = rawNum
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

                        if (smsManager != null) {
                            val parts = smsManager.divideMessage(alertSms)
                            if (parts != null && parts.size > 1) {
                                smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
                            } else {
                                smsManager.sendTextMessage(sanitizedTo, null, alertSms, null, null)
                            }
                            prefs.incrementSmsSentCount()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            else -> {
                errorMessage = "کد وارد شده صحیح نیست ❌"
                enteredPin = ""
                
                // Secretly capture intruder selfie
                if (prefs.isIntruderSelfieEnabled) {
                    try {
                        CameraHelper.captureIntruderPhoto(context)
                    } catch (e: Exception) {
                        Log.e("GuardianLock", "Error taking photo: ${e.message}")
                    }
                }
                
                // Log failed attempt
                scope.launch {
                    val locResult = LocationTracker.getOfflineLocationData(context)
                    val pos = if (locResult.isGPS) "${locResult.lat},${locResult.lng}" else "نامشخص"
                    
                    db.logDao().insertLog(LogEntry(
                        eventType = "ALARM",
                        description = "رمز عبور بازگشایی اشتباه وارد شد",
                        location = pos,
                        details = "کد وارد شده: $code"
                    ))
                    
                    val timeFarsi = JalaliCalendarHelper.getJalaliDateString(System.currentTimeMillis())
                    val alertSms = "تلاش ناموفق برای باز کردن قفل گوشی\nزمان: $timeFarsi\nموقعیت: $pos"
                    
                    try {
                        val rawNum = prefs.emergencyNumber
                        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
                        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
                        var sanitizedTo = rawNum
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

                        if (smsManager != null) {
                            val parts = smsManager.divideMessage(alertSms)
                            if (parts != null && parts.size > 1) {
                                smsManager.sendMultipartTextMessage(sanitizedTo, null, parts, null, null)
                            } else {
                                smsManager.sendTextMessage(sanitizedTo, null, alertSms, null, null)
                            }
                            prefs.incrementSmsSentCount()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F0F) // Deep warning dark background
    ) {
        AnimatedVisibility(
            visible = isPhoneDecoyUnlocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Fake Mock Empty System Home Screen (Decoy unlock)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "سیستم در حال راه‌اندازی است...\nلطفاً شکیبا باشید.",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "اطلاعات شبکه و تماس سیم‌کارت در حال همگام‌سازی است.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = !isPhoneDecoyUnlocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP WARNING DISPLAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    when (type) {
                        "timer" -> {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300), // Amber accent
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "⚠️ این گوشی سرقتی است",
                                color = Color(0xFFFF5252),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "اطلاع به پلیس در:",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = parseTimerText(remainingSeconds),
                                color = Color(0xFFFF5252),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "بازگشت گوشی = جایزه نقدی محفوظ",
                                color = Color(0xFFFFB300),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "message" -> {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "🔒 پیام اضطراری سیستم",
                                color = Color(0xFFFFB300),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = customMessage.ifEmpty { "سیستم امنیت فعال است!" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp),
                                    lineHeight = 28.sp
                                )
                            }
                        }
                        else -> { // DEFAULT SECURITY SMART LOCK
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color(0xFF1A237E),
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF1A237E).copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                    .padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "🔒 این گوشی قفل شده است",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = customMessage.ifEmpty { prefs.lockScreenMessage },
                                color = Color.LightGray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Emergency Hotline Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "در صورت پیدا کردن با این شماره تماس بگیرید:",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = JalaliCalendarHelper.toPersianNumber(prefs.emergencyNumber),
                                            color = Color(0xFFFFB300),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("emergency_phone_label")
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            try {
                                                // Trigger fast calling prompt (standard security exception proof dial)
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = android.net.Uri.parse("tel:${prefs.emergencyNumber}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        },
                                        modifier = Modifier
                                            .background(Color(0xFFFFB300), RoundedCornerShape(12.dp))
                                            .size(48.dp)
                                            .testTag("call_emergency_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Call,
                                            contentDescription = "تماس اضطراری",
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "🎁 مژدگانی ارزنده‌ای برای یابنده محفوظ است",
                                color = Color(0xFFFFB300),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // BOTTOM UNLOCK CONTROLS
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { 
                            if (it.length <= 10) {
                                enteredPin = it 
                                errorMessage = ""
                            }
                        },
                        label = { Text("ورود رمز مخفی یا PIN جعلی...", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = Color(0xFFFFB300)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_code_input_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleUnlockAttempt() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_unlock_button")
                    ) {
                        Text(
                            text = "تأیید و بازگشایی قفل",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
