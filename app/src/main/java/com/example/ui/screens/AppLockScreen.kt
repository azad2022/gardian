package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.GuardianPrefs
import com.example.utils.CameraHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { GuardianPrefs(context) }
    var enteredCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val correctCode = prefs.rawSecretCode
    val decoyCode = prefs.decoyPin

    // Shake animation offset on wrong passcode
    val shakeOffset = remember { Animatable(0f) }

    fun triggerVibration(duration: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    LaunchedEffect(enteredCode) {
        if (enteredCode.length == 4) {
            delay(150) // Brief visual pause so the user sees the 4th dot fill
            if (enteredCode == correctCode || (decoyCode.isNotEmpty() && enteredCode == decoyCode)) {
                triggerVibration(70)
                onUnlockSuccess()
            } else {
                // Wrong code
                isError = true
                triggerVibration(250)
                
                // Secretly capture intruder selfie
                if (prefs.isIntruderSelfieEnabled) {
                    try {
                        CameraHelper.captureIntruderPhoto(context)
                    } catch (e: Exception) {
                        android.util.Log.e("AppLockScreen", "Error triggering camera: ${e.message}")
                    }
                }
                
                // Shake animation sequence
                scope.launch {
                    val shakeSpec = keyframes<Float> {
                        durationMillis = 300
                        -15f at 50
                        15f at 100
                        -10f at 150
                        10f at 200
                        -5f at 250
                        0f at 300
                    }
                    shakeOffset.animateTo(0f, animationSpec = shakeSpec)
                }
                
                delay(400)
                enteredCode = ""
                isError = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0x154CAF50), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل برنامه",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "سیستم امنیتی گاردین",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "برای ورود رمز عبور ۴ رقمی خود را وارد کنید",
                fontSize = 14.sp,
                color = if (isError) Color(0xFFE57373) else Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
        }

        // Passcode DOTS Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(x = shakeOffset.value.dp)
                .padding(vertical = 24.dp)
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredCode.length
                val color = when {
                    isError -> Color(0xFFE57373)
                    isFilled -> Color(0xFF4CAF50)
                    else -> Color(0xFF333333)
                }
                
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color, shape = CircleShape)
                )
            }
        }

        // Custom Keypad Custom Grid (Balanced vertical layout)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 30.dp)
        ) {
            val keyList = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back")
            )

            keyList.forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    rowKeys.forEach { key ->
                        if (key.isEmpty()) {
                            // Empty space for layout symmetry
                            Box(modifier = Modifier.size(68.dp))
                        } else if (key == "back") {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1F222222))
                                    .clickable {
                                        if (enteredCode.isNotEmpty()) {
                                            triggerVibration(30)
                                            enteredCode = enteredCode.dropLast(1)
                                        }
                                    }
                                    .testTag("app_lock_backspace_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "پاک کردن",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2F222222))
                                    .clickable {
                                        if (enteredCode.length < 4) {
                                            triggerVibration(25)
                                            enteredCode += key
                                        }
                                    }
                                    .testTag("app_lock_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
