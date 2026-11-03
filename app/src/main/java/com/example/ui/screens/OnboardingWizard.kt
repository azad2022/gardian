package com.example.ui.screens

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.receiver.GuardianDeviceAdminReceiver
import com.example.ui.theme.GuardianAccent
import com.example.ui.theme.GuardianSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.JalaliCalendarHelper

@Composable
fun OnboardingWizard(viewModel: MainViewModel) {
    val currentStep by viewModel.onboardingStep.collectAsState()

    // Force RTL Layout to support Persian natively
    CompositionLocalProvider(value = LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F0F))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Wizard Header
                OnboardingHeader(currentStep = currentStep)

                // Step Content (7 distinct steps)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentStep) {
                        1 -> WelcomeStepView(viewModel)
                        2 -> EmergencyContactStepView(viewModel)
                        3 -> SecretSmsCodeStepView(viewModel)
                        4 -> DecoyPinStepView(viewModel)
                        5 -> DeviceAdminStepView(viewModel)
                        6 -> PermissionsStepView(viewModel)
                        7 -> ReadyStepView(viewModel)
                    }
                }

                // Footer (Dynamic Navigation dots for 7 steps)
                OnboardingFooter(currentStep = currentStep)
            }
        }
    }
}

@Composable
fun OnboardingHeader(currentStep: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "راه‌اندازی سامانه ضد سرقت گاردین 🛡️",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("onboarding_header_title")
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal step indicators (7 steps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..7) {
                val isActive = i == currentStep
                val isCompleted = i < currentStep

                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isActive) 24.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> GuardianAccent
                                isCompleted -> GuardianSecondary
                                else -> Color.DarkGray
                            }
                        )
                )
                if (i < 7) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingFooter(currentStep: Int) {
    Text(
        text = "گام $currentStep از ۷",
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// STEP 1: WELCOME & PRIVACY POLICY
@Composable
fun WelcomeStepView(viewModel: MainViewModel) {
    val scroll = rememberScrollState()
    var isPrivacyAccepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "به گاردین خوش آمدید",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "سامانه مدرن، هوشمند و کاملاً آفلاین ضدسرقت گوشی هوشمند. این برنامه بدون نیاز به اینترنت، گزارشات موقعیت‌یاب و قفل‌های امنیتی را از طریق پیامک برای شما ارسال می‌کند.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                BulletFeatureItem("کنترل ۱۰۰ درصد آفلاین با پیامک", "کنترل گوشی از راه دور بدون نیاز به شبکه اینترنتی")
                BulletFeatureItem("اعلام هوشمند تغییر سیم‌کارت", "پیامک اتوماتیک حاوی مختصات GPS در صورت تعویض سیم کارت")
                BulletFeatureItem("قفل امنیتی و آژیر ضد سکوت", "گوشی با حداکثر توان آژیر کشیده و فلاش دوربین چشمک‌زن می‌شود")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PRIVACY POLICY SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛡️ سیاست حفظ حریم خصوصی (Privacy Policy)",
                    color = GuardianAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "برنامه ضدسرقت گاردین متعهد به حفظ کامل حریم خصوصی و صیانت از داده‌های شماست. این برنامه کاملا آفلاین بوده و جهت کاربری امنیتی خود داده‌های زیر را به صورت کاملاً لوکال دریافت و پردازش می‌کند:\n" +
                            "۱. شماره همراه اضطراری: صرفاً جهت ارسال پیامک آلارم و هشدار سرقت به مالک ثانویه انتخابی شما.\n" +
                            "۲. موقعیت مکانی دقیق و تقریبی: جهت مکان‌یابی تلفن هدر رفته یا مسروقه و ارسال مستقیم پیامک لوکیشن.\n" +
                            "۳. دسترسی دوربین جلو: جهت گرفتن سلفی محرمانه از چهره فرد خرابکار در صورت تلاش ناموفق برای باز کردن پین قفل.\n" +
                            "۴. خواندن و بررسی پیامک‌های ورودی: جهت دریافت دستورات کنترل از راه دور پیامکی ارسال شده از جانب شماره مالک ثانویه.\n\n" +
                            "تعهد ما: هیچ داده‌ای به هیچ سرور خارجی، شخص ثالث، توسعه‌دهنده یا سازمان خارجی ارسال نخواهد شد و کلیه کارهای پردازش و نگهداری اطلاعات در امنیت مطلق روی حافظه داخلی تلفن همراه شما نگهداری می‌شود.",
                    color = Color.LightGray.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPrivacyAccepted = !isPrivacyAccepted }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPrivacyAccepted,
                        onCheckedChange = { isPrivacyAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GuardianAccent,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سیاست حفظ حریم خصوصی گاردین را با دقت مطالعه کردم و با کلیه موارد موافقم.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.setOnboardingStep(2) },
            enabled = isPrivacyAccepted,
            colors = ButtonDefaults.buttonColors(
                containerColor = GuardianAccent,
                contentColor = Color.Black,
                disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_start_settings_button")
        ) {
            Text("موافقت و شروع تنظیمات گاردین", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// STEP 2: EMERGENCY PHONE
@Composable
fun EmergencyContactStepView(viewModel: MainViewModel) {
    var rawPhone by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "تعیین شماره اضطراری",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "این شماره مالک ثانویه (همسر، پدر یا دوست صمیمی) است که تمام پیامک‌های اضطراری، جزییات موقعیت مکانی و آلارم‌ها را دریافت می‌کند.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = rawPhone,
            onValueChange = {
                if (it.all { char -> char.isDigit() || char == '+' }) {
                    rawPhone = it
                    errorMsg = ""
                }
            },
            label = { Text("مثال: 09123456789", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GuardianAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GuardianAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("emergency_phone_input_onboarding")
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setOnboardingStep(1) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("قبلی")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (rawPhone.trim().length >= 10) {
                        viewModel.submitEmergencyNumberOnSetup(rawPhone.trim())
                        viewModel.setOnboardingStep(3)
                    } else {
                        errorMsg = "لطفا یک شماره تلفن معتبر (حداقل ۱۰ رقم) وارد کنید ❌"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardianAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("submit_emergency_phone_onboarding")
            ) {
                Text("تأیید و ادامه", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// STEP 3: SMS SECRET CODE
@Composable
fun SecretSmsCodeStepView(viewModel: MainViewModel) {
    var code by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "تعیین کد کنترل پیامکی",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "یک رمز ۴ رقمی تعیین کنید. شما تمام دستورات مانند قفل کردن یا اخذ موقعیت مکانی را با این رمز هدایت می‌کنید.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "مثال: برای ردیابی گوشی کافیست پیامک #LOCکدمخفی# را بفرستید.",
                color = GuardianAccent,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = code,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    code = it
                    errorMsg = ""
                }
            },
            label = { Text("رمز ۴ رقمی جدید", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GuardianAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GuardianAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sms_code_setup_onboarding")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmCode,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    confirmCode = it
                    errorMsg = ""
                }
            },
            label = { Text("تکرار رمز ۴ رقمی جدید", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GuardianAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GuardianAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sms_code_confirm_onboarding")
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setOnboardingStep(2) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("قبلی")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (code.length == 4 && code == confirmCode) {
                        viewModel.submitSecretSmsCodeOnSetup(code)
                        viewModel.setOnboardingStep(4) // Proceed to Decoy PIN step!
                    } else if (code != confirmCode) {
                        errorMsg = "رمز‌های وارد شده یکسان نیستند ❌"
                    } else {
                        errorMsg = "رمز عبور باید ۴ رقم باشد ❌"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardianAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("submit_sms_code_onboarding")
            ) {
                Text("تأیید و ادامه", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// STEP 4: DECOY PIN (PIN جعلی / فریب‌دهنده سارق)
@Composable
fun DecoyPinStepView(viewModel: MainViewModel) {
    var decoy by remember { mutableStateOf("") }
    var confirmDecoy by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val secretCode by viewModel.secretSmsCode.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "تعیین PIN جعلی (فریب‌دهنده)",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "یک رمز دوم ۴ رقمی (متفاوت از کد پیامکی) تعیین کنید. اگر سارق شما را مجبور به باز کردن گوشی کرد، با وارد کردن این رمز گوشی ظاهراً باز می‌شود، اما در پس‌زمینه بدون ایجاد کوچک‌ترین سر و صدا، موقعیت GPS و هشدار خطر به شماره اضطراری مخابره می‌گردد.",
            color = Color.LightGray,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = decoy,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    decoy = it
                    errorMsg = ""
                }
            },
            label = { Text("رمز ۴ رقمی جعلی (فریبنده)", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GuardianAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GuardianAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("decoy_pin_input_onboarding")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmDecoy,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    confirmDecoy = it
                    errorMsg = ""
                }
            },
            label = { Text("تکرار رمز ۴ رقمی جعلی", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GuardianAccent,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GuardianAccent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("decoy_pin_confirm_onboarding")
        )

        if (errorMsg.isNotEmpty()) {
            Text(
                text = errorMsg,
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setOnboardingStep(3) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("قبلی")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (decoy.length != 4) {
                        errorMsg = "PIN جعلی باید ۴ رقمی باشد ❌"
                    } else if (decoy != confirmDecoy) {
                        errorMsg = "تکرار رمز با رمز وارد شده مطابقت ندارد ❌"
                    } else if (decoy == secretCode) {
                        errorMsg = "PIN جعلی باید با کد کنترل پیامکی تفاوت داشته باشد! ❌"
                    } else {
                        viewModel.submitDecoyPinOnSetup(decoy)
                        viewModel.setOnboardingStep(5) // Proceed to Device Admin
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GuardianAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("submit_decoy_pin_onboarding")
            ) {
                Text("تأیید و ادامه", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// STEP 5: DEVICE ADMINISTRATOR (با سه وضعیت دقیق و بررسی هنگام بازگشت از تنظیمات)
@Composable
fun DeviceAdminStepView(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val adminReceiver = remember { ComponentName(context, GuardianDeviceAdminReceiver::class.java) }
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminReceiver)) }
    var hasAttemptedLaunch by remember { mutableStateOf(false) }
    var showIncompleteDialog by remember { mutableStateOf(false) }

    // Real-time resume listener to accurately detect if user actually accepted Device Admin in Android settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = dpm.isAdminActive(adminReceiver)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launchAdminSetup = {
        hasAttemptedLaunch = true
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "سامانه گاردین برای جلوگیری از لغو نصب برنامه توسط سارق به مجوز مدیر دستگاه نیاز دارد.")
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "فعالسازی مدیریت دستگاه (Device Admin)",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "جهت جلوگیری از حذف غیرمجاز اپلیکیشن توسط سارقین در تنظیمات اندروید و بازار، مجوز مدیریت دستگاه الزامی است.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Three distinct status states:
        when {
            isAdminActive -> {
                // State 1: Verified Active
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "مدیر دستگاه فعال و تأیید شد 🛡️", color = Color(0xFF81C784), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "سیستم ضدحذف با موفقیت فعال شد. برنامه در برابر حذف محافظت می‌شود.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
            hasAttemptedLaunch -> {
                // State 2: Returned without activation (Denied / Incomplete)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFC62828).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "وضعیت: مدیر دستگاه فعال نشد ⚠️", color = Color(0xFFEF5350), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "پنجره تنظیمات بسته شد اما مجوز اعطا نگردید. برای فعال‌سازی کامل حفاظت ضدحذف، لطفاً مجدداً دکمه زیر را لمس کرده و در پنجره سیستم گزینه فعال‌سازی را انتخاب فرمایید.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { launchAdminSetup() },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardianAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("تلاش مجدد برای فعال‌سازی مدیر دستگاه", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                // State 3: Waiting for initial launch
                Button(
                    onClick = { launchAdminSetup() },
                    colors = ButtonDefaults.buttonColors(containerColor = GuardianAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("activate_device_admin_button")
                ) {
                    Text("فعال‌سازی سرور مدیریت گاردین", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setOnboardingStep(4) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("قبلی")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (isAdminActive) {
                        viewModel.setOnboardingStep(6) // Proceed to Permissions
                    } else {
                        showIncompleteDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdminActive) GuardianAccent else Color.DarkGray,
                    contentColor = if (isAdminActive) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("next_device_admin_button")
            ) {
                Text(
                    text = if (isAdminActive) "تأیید و ادامه" else "ادامه بدون مدیر دستگاه",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteDialog = false },
            title = {
                Text(
                    text = "هشدار عدم فعال‌سازی مدیر دستگاه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "بدون فعال‌سازی مجوز مدیریت دستگاه، سارق می‌تواند با باز کردن بخش تنظیمات گوشی یا بازار، برنامه گاردین را به آسانی لغو نصب کند. آیا مایلید بدون حفاظت ضدحذف ادامه دهید؟",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncompleteDialog = false
                        viewModel.setOnboardingStep(6)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White)
                ) {
                    Text("ادامه با ریسک حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showIncompleteDialog = false
                    launchAdminSetup()
                }) {
                    Text("بازگشت و فعال‌سازی", color = GuardianAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// STEP 6: PERMISSIONS REQUESTER (اصلاح رفتار دکمه ادامه برای جلوگیری از گمراهی)
@Composable
fun PermissionsStepView(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionsState by remember {
        mutableStateOf(
            mapOf(
                Manifest.permission.SEND_SMS to isGranted(context, Manifest.permission.SEND_SMS),
                Manifest.permission.RECEIVE_SMS to isGranted(context, Manifest.permission.RECEIVE_SMS),
                Manifest.permission.READ_SMS to isGranted(context, Manifest.permission.READ_SMS),
                Manifest.permission.ACCESS_FINE_LOCATION to isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION),
                Manifest.permission.RECORD_AUDIO to isGranted(context, Manifest.permission.RECORD_AUDIO)
            )
        )
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var showIncompletePermsDialog by remember { mutableStateOf(false) }

    // Re-check permissions and overlay on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsState = mapOf(
                    Manifest.permission.SEND_SMS to isGranted(context, Manifest.permission.SEND_SMS),
                    Manifest.permission.RECEIVE_SMS to isGranted(context, Manifest.permission.RECEIVE_SMS),
                    Manifest.permission.READ_SMS to isGranted(context, Manifest.permission.READ_SMS),
                    Manifest.permission.ACCESS_FINE_LOCATION to isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION),
                    Manifest.permission.RECORD_AUDIO to isGranted(context, Manifest.permission.RECORD_AUDIO)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hasOverlayPermission = Settings.canDrawOverlays(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsState = results.mapValues { it.value }
    }

    val allGranted = permissionsState.values.all { it } && hasOverlayPermission

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "تأیید مجوزهای امنیتی",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "برای کارکرد کاملاً خودکار و برقراری امنیت از راه دور، واگذاری این مجوزها ضروری است.",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Listing required items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PermissionExplanationRow(
                title = "پیامک (SMS)",
                description = "برای دریافت دستورات ردیابی راه دور و فرستادن اطلاعات موقعیت سارق",
                isGranted = permissionsState[Manifest.permission.SEND_SMS] == true &&
                        permissionsState[Manifest.permission.RECEIVE_SMS] == true &&
                        permissionsState[Manifest.permission.READ_SMS] == true,
                icon = Icons.Filled.Email
            )

            PermissionExplanationRow(
                title = "مکان‌یاب جی‌پی‌اس (GPS)",
                description = "جهت استخراج آفلاین دقیق‌ترین مختصات جغرافیایی دستگاه مسروقه",
                isGranted = permissionsState[Manifest.permission.ACCESS_FINE_LOCATION] == true,
                icon = Icons.Filled.LocationOn
            )

            PermissionExplanationRow(
                title = "ضبط صدا (اسلات‌های هشدار صوتی)",
                description = "جهت ضبط پیام صوتی آژیرگیر به دلخواه مالک و پخش با حداکثر توان",
                isGranted = permissionsState[Manifest.permission.RECORD_AUDIO] == true,
                icon = Icons.Filled.Call
            )

            PermissionExplanationRow(
                title = "ترسیم بر روی سایر برنامه‌ها (Overlay)",
                description = "جهت نمایش پیامک مسدودساز صفحه بر روی قفل گوشی از راه دور",
                isGranted = hasOverlayPermission,
                icon = Icons.Filled.Settings
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!allGranted) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val hasCorePerms = permissionsState.values.all { it }
                if (!hasCorePerms) {
                    Button(
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardianSecondary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(bottom = 8.dp)
                            .testTag("request_permissions_button_onboarding")
                    ) {
                        Text("اعطای دسترسی‌های امنیتی لازم")
                    }
                }

                if (!hasOverlayPermission) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("فعالسازی قابلیت مدیریت ترسیم روی برنامه‌ها")
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                    .padding(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "تمامی دسترسی‌های اصلی با موفقیت اعطا شد.", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.setOnboardingStep(5) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("قبلی")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (allGranted) {
                        viewModel.setOnboardingStep(7) // Go to Ready
                    } else {
                        showIncompletePermsDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allGranted) GuardianAccent else Color.White.copy(alpha = 0.08f),
                    contentColor = if (allGranted) Color.Black else Color.White
                ),
                border = if (allGranted) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("next_onboarding_perm_button")
            ) {
                Text(
                    text = if (allGranted) "تأیید و ادامه" else "ادامه با مجوزهای ناقص ⚠️",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showIncompletePermsDialog) {
        AlertDialog(
            onDismissRequest = { showIncompletePermsDialog = false },
            title = {
                Text(
                    text = "ادامه با دسترسی‌های ناقص",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "برخی مجوزهای کلیدی (مانند پیامک، موقعیت مکانی جی‌پی‌اس یا ترسیم روی صفحه) هنوز فعال نشده‌اند. بدون این دسترسی‌ها، قابلیت ردیابی آفلاین و قفل فوری گوشی غیرفعال خواهد بود. آیا مایلید با محدودیت ادامه دهید؟",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncompletePermsDialog = false
                        viewModel.setOnboardingStep(7)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White)
                ) {
                    Text("ادامه به هر حال", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompletePermsDialog = false }) {
                    Text("بازگشت و اعطای مجوزها", color = GuardianAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PermissionExplanationRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(1.dp, if (isGranted) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color.DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGranted) "فعال" else "غیرفعال",
                color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF5252),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// STEP 7: READY & CONFIG SUMMARY (با محافظت از نمایش رمز پیامکی و پین جعلی)
@Composable
fun ReadyStepView(viewModel: MainViewModel) {
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()
    val secretCode by viewModel.secretSmsCode.collectAsState()
    val decoyPin by viewModel.decoyPin.collectAsState()

    var isSecretCodeVisible by remember { mutableStateOf(false) }
    var isDecoyPinVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "آماده محافظت از گوشی شما هستیم!",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "پیکربندی با موفقیت انجام شد. خلاصه تنظیمات امنیتی شما به شرح زیر است:",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryConfigRow("تلفن اضطراری گیرنده هشدار:", JalaliCalendarHelper.toPersianNumber(emergencyNumber))

                // MASKED SECRET CODE WITH TOGGLE (CRITICAL SECURITY FIX)
                SummarySecretRow(
                    label = "کد پیامکی کنترل از راه دور:",
                    value = secretCode,
                    isVisible = isSecretCodeVisible,
                    onToggle = { isSecretCodeVisible = !isSecretCodeVisible }
                )

                // MASKED DECOY PIN WITH TOGGLE
                SummarySecretRow(
                    label = "PIN جعلی (فریب‌دهنده):",
                    value = if (decoyPin.isNotEmpty()) decoyPin else "تنظیم‌نشده",
                    isVisible = isDecoyPinVisible,
                    onToggle = { isDecoyPinVisible = !isDecoyPinVisible }
                )

                SummaryConfigRow("سیستم ضد حذف (Device Admin):", "فعال و مجهز")
                SummaryConfigRow("اعلام تعویض سیم کارت:", "روشن")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.completeOnboardingAndActivate() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_and_activate_button")
        ) {
            Text("فعالسازی و ورود به صفحه اصلی گاردین", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SummarySecretRow(
    label: String,
    value: String,
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isVisible) JalaliCalendarHelper.toPersianNumber(value) else "••••",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (isVisible) "مخفی کردن" else "نمایش رمز",
                    tint = GuardianAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun BulletFeatureItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = GuardianAccent,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun SummaryConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

fun isGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
