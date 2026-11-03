package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.components.VoiceSlotCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.utils.CameraHelper
import java.io.File
import com.example.MainActivity
import com.example.data.database.LogEntry
import com.example.data.prefs.GuardianPrefs
import com.example.service.GuardianService
import com.example.utils.JalaliCalendarHelper
import com.example.ui.overlay.GuardianLockActivity
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val context = LocalContext.current

    // Force layout direction to RTL for native Persian support
    CompositionLocalProvider(value = LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1A237E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛡️", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "گاردین",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F0F0F)
                    ),
                    actions = {
                        IconButton(
                            onClick = { /* notification action placeholder */ },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Text("🔔", fontSize = 16.sp)
                        }
                    },
                    modifier = Modifier.testTag("app_bar_title")
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF151515),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == "home",
                        onClick = { viewModel.updateActiveTab("home") },
                        label = { Text("خانه", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "خانه") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFB300),
                            selectedTextColor = Color(0xFFFFB300),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("home_tab_button")
                    )

                    NavigationBarItem(
                        selected = activeTab == "settings",
                        onClick = { viewModel.updateActiveTab("settings") },
                        label = { Text("تنظیمات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = "تنظیمات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFB300),
                            selectedTextColor = Color(0xFFFFB300),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("settings_tab_button")
                    )

                    NavigationBarItem(
                        selected = activeTab == "logs",
                        onClick = { viewModel.updateActiveTab("logs") },
                        label = { Text("گزارشات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Filled.List, contentDescription = "گزارشات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFB300),
                            selectedTextColor = Color(0xFFFFB300),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("logs_tab_button")
                    )

                    NavigationBarItem(
                        selected = activeTab == "guide",
                        onClick = { viewModel.updateActiveTab("guide") },
                        label = { Text("راهنما", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "راهنما") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFB300),
                            selectedTextColor = Color(0xFFFFB300),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("guide_tab_button")
                    )
                }
            },
            containerColor = Color(0xFF0F0F0F)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "home" -> HomeTabScreen(viewModel)
                    "settings" -> SettingsTabScreen(viewModel)
                    "logs" -> LogsTabScreen(viewModel)
                    "guide" -> GuideTabScreen(viewModel)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 1. HOME TAB SCREEN (خانه)
// ═══════════════════════════════════════
@Composable
fun HomeTabScreen(viewModel: MainViewModel) {
    val isArmed by viewModel.isArmed.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val lastEvent by viewModel.lastEventMessage.collectAsState()
    val smsCount by viewModel.smsSentCount.collectAsState()
    val lastLocTime by viewModel.lastLocationTimeStr.collectAsState()
    
    val recentLogs by viewModel.eventLogs.collectAsState()
    val listToShow = recentLogs.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High visibility guard active state layout card in Clean Minimalism theme styles
        Card(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isArmed) Color(0xFF1A237E) else Color(0xFF151515)
            ),
            border = if (isArmed) null else BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular status indicator
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 4.dp,
                                color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                    // Inner dashed simulated ring
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                color = Color(0xFFFFB300).copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                    )
                    // Center white circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArmed) "🔒" else "🔓",
                            fontSize = 30.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isArmed) "سیستم فعال است" else "سیستم غیرفعال است",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("armed_status_text")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isArmed) "تمام سنسورها در حال مانیتورینگ هستند" else "آخرین فعالیت: $lastEvent",
                    color = if (isArmed) Color(0xFF90CAF9) else Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Armed state activator button
                Button(
                    onClick = { viewModel.toggleGuardianArmed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF1A237E)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("arm_toggle_button")
                ) {
                    Text(
                        text = if (isArmed) "توقف محافظت" else "مسلح‌سازی و فعال‌سازی فوری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Device Guard mode sector
        Text(
            text = "حالت محافظت فعال دستگاه",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val modes = listOf(
                "pocket" to "جیبی",
                "cafe" to "کافه",
                "charging" to "شارژر",
                "bag" to "کیف"
            )
            for ((key, title) in modes) {
                val isSelected = currentMode == key
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clickable { viewModel.updateCurrentMode(key) }
                        .testTag("mode_$key"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f)
                    ),
                    border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color(0xFF1A237E) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatInfoCard(
                modifier = Modifier.weight(1.2f),
                title = "پیامک‌های ارسالی",
                value = JalaliCalendarHelper.toPersianNumber(smsCount.toString()),
                sub = "کنترل و گزارش موقعیت",
                tag = "sms_sent_stat_card"
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatInfoCard(
                modifier = Modifier.weight(1f),
                title = "امنیت تداوم",
                value = JalaliCalendarHelper.toPersianNumber("۱"),
                sub = "روزه محافظت",
                tag = "days_protected_card"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent alerts list (last 5 logs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "آخرین اقدامات امنیتی رخ‌داده",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "مشاهده همه",
                color = Color(0xFFFFB300),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { viewModel.updateActiveTab("logs") }
                    .testTag("view_all_logs_text")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (listToShow.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "هیچ گزارش امنیتی ثبت نشده است (سیستم پاک است) ✅",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (log in listToShow) {
                    LogTimelineItem(log = log)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    tag: String
) {
    Card(
        modifier = modifier.testTag(tag),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color(0xFFFFB300),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (sub.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// 2. SETTINGS TAB SCREEN (تنظیمات)
// ═══════════════════════════════════════
@Composable
fun SettingsTabScreen(viewModel: MainViewModel) {
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()
    val secretCode by viewModel.secretSmsCode.collectAsState()
    
    val simDetect by viewModel.simDetectEnabled.collectAsState()
    val usbAlert by viewModel.usbAlertEnabled.collectAsState()
    val chargerAlert by viewModel.chargerAlertEnabled.collectAsState()
    val btGuardian by viewModel.btGuardianEnabled.collectAsState()
    val btDeviceName by viewModel.btDeviceName.collectAsState()
    val btThreshold by viewModel.btThreshold.collectAsState()
    val heartbeatInterval by viewModel.heartbeatInterval.collectAsState()
    val lockScreenMsg by viewModel.lockScreenMessage.collectAsState()

    val isAppLockerEnabled by viewModel.isAppLockerEnabled.collectAsState()
    val isDeviceLockEnabled by viewModel.isDeviceLockEnabled.collectAsState()
    val isIntruderSelfieEnabled by viewModel.isIntruderSelfieEnabled.collectAsState()

    val recordedSlots by viewModel.recordedSlots.collectAsState()
    val activeRecording by viewModel.activeRecordingSlot.collectAsState()
    val activePlaying by viewModel.activePlaybackSlot.collectAsState()

    var editingNumber by remember { mutableStateOf(emergencyNumber) }
    var editingCode by remember { mutableStateOf(secretCode) }
    var editingMessage by remember { mutableStateOf(lockScreenMsg) }

    val context = LocalContext.current

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var isAdminActive by remember { mutableStateOf(viewModel.isDeviceAdminActive()) }
    var showDeactivateConfirmDialog by remember { mutableStateOf(false) }

    var pendingRecordSlot by remember { mutableStateOf<Int?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingRecordSlot?.let { slot ->
                viewModel.startRecordingSound(slot)
                pendingRecordSlot = null
            }
        }
    }

    val requestRecordSlot: (Int) -> Unit = { slot ->
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.startRecordingSound(slot)
        } else {
            pendingRecordSlot = slot
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Automatically check overlay and admin status on launch
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        }
        isAdminActive = viewModel.isDeviceAdminActive()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!hasOverlayPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نیاز به مجوز ترسیم روی سایر برنامه‌ها ⚠️",
                            color = Color(0xFFEF5350),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای کارکرد و اجرای صحیح دستور قفل صفحه نمایش و مسدودسازی تصویری سارق، اعطای مجوز ترسیم حیاتی است. لطفاً برای فعال‌سازی دکمه زیر را فشار دهید.",
                        color = Color.LightGray,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text("فعال‌سازی دسترسی ترسیم سیستم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: EMERGENCY CREDENTIALS
        Text(
            text = "اطلاعات اضطراری و حساب",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Emergency Phone number setup
                OutlinedTextField(
                    value = editingNumber,
                    onValueChange = { 
                        editingNumber = it
                        viewModel.setEmergencyNumber(it)
                    },
                    label = { Text("شماره تلفن اضطراری مالک", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFFFB300),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_phone_input_settings"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Secret command code input4 digits
                OutlinedTextField(
                    value = editingCode,
                    onValueChange = { 
                        if (it.length <= 4) {
                            editingCode = it
                            viewModel.setSecretSmsCode(it)
                        }
                    },
                    label = { Text("کد رمز پیامکی کنترل (۴ رقم)", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFFFB300),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_code_input_settings"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test SMS and apply buttons
                Button(
                    onClick = { viewModel.fireTestSms() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF1A237E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_sms_button")
                ) {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ارسال پیامک تستی به شماره اضطراری", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 2: SMART TRIGGERS
        Text(
            text = "سنسورها و آلارم‌های هوشمند",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Toggle SIM detect
                SettingToggleRow(
                    title = "سیستم تشخیص تعویض سیم‌کارت",
                    desc = "ارسال لوکیشن به محض خارج شدن یا اتصال سیم جدید",
                    checked = simDetect,
                    onCheckedChange = { viewModel.setSimDetectEnabled(it) },
                    tag = "sim_check_toggle"
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle USB alarm
                SettingToggleRow(
                    title = "هشدار اتصال کابل USB سارق",
                    desc = "آژیر بلند و بلاک فوری داده وقتی به کامپیوتر متصل شد",
                    checked = usbAlert,
                    onCheckedChange = { viewModel.setUsbAlertEnabled(it) },
                    tag = "usb_check_toggle"
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle Charger alerted
                SettingToggleRow(
                    title = "آلارم خروج گوشی از منبع شارژ",
                    desc = "ارسال لوکیشن به شاکی و بوق خطر در زمان قطع شارژر",
                    checked = chargerAlert,
                    onCheckedChange = { viewModel.setChargerAlertEnabled(it) },
                    tag = "charger_check_toggle"
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle Bluetooth paired device protector
                SettingToggleRow(
                    title = "نگهبان هوشمند بلوتوث (Bluetooth Guardian)",
                    desc = "اتصال به ساعت یا ایربادز. دوری و قطع اتصال آن زنگ خطر را فعال می‌کند",
                    checked = btGuardian,
                    onCheckedChange = { viewModel.setBtGuardianEnabled(it) },
                    tag = "bt_guard_check_toggle"
                )

                if (btGuardian) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "نام دستگاه بلوتوث متصل: ${if (btDeviceName.isEmpty()) "نگهبانی در انتظار جفت‌سازی" else btDeviceName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "فاصله خروج تا هشدار: $btThreshold دقیقه", color = Color.Gray, fontSize = 12.sp)
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Heartbeat frequency adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فرکانس سیگنال حیات دوره‌ای (Heartbeat)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("ارسال هرازگاه سلامت فیزیکی و باتری به مالک", color = Color.Gray, fontSize = 11.sp)
                    }
                    val options = listOf(3, 6, 12)
                    Row {
                        for (opt in options) {
                            val active = heartbeatInterval == opt
                            Button(
                                onClick = { viewModel.setHeartbeatInterval(opt) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f),
                                    contentColor = if (active) Color(0xFF1A237E) else Color.White
                                ),
                                border = if (active) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(width = 44.dp, height = 36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("${opt}ساعت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle App Locker
                SettingToggleRow(
                    title = "سیستم قفل سنگین برنامه‌ها (App Locker)",
                    desc = "درخواست گذرواژه برای ورود مجدد به کلیه برنامه‌های جانبی",
                    checked = isAppLockerEnabled,
                    onCheckedChange = { viewModel.setAppLockerEnabled(it) },
                    tag = "app_locker_check_toggle"
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle Device Lock
                SettingToggleRow(
                    title = "قفل فیزیکی سخت‌افزاری دستگاه (Device Lock)",
                    desc = "اجازه اعمال قفل و تغییر PIN اصلی صفحه از طریق پیامک",
                    checked = isDeviceLockEnabled,
                    onCheckedChange = { viewModel.setDeviceLockEnabled(it) },
                    tag = "device_lock_check_toggle"
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                // Toggle Intruder Selfie
                SettingToggleRow(
                    title = "تصویربردار مخفی محیط آفلاین (Intruder Selfie)",
                    desc = "ثبت تصویر سارق صورت در گالری حین ثبت پین یا رمز نامعتبر",
                    checked = isIntruderSelfieEnabled,
                    onCheckedChange = { viewModel.setIntruderSelfieEnabled(it) },
                    tag = "intruder_selfie_toggle"
                )
            }
        }

        // Section 3: SPECIAL RECORDER SLOTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎙️",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اسلات‌های ضبط هشدار صوتی ضد سکوت",
                    color = Color(0xFFFFB300),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val readyCount = recordedSlots.values.count { it }
            Surface(
                color = if (readyCount > 0) Color(0xFF81C784).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (readyCount > 0) Color(0xFF81C784).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    text = "$readyCount از ۴ فعال",
                    color = if (readyCount > 0) Color(0xFFA5D6A7) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Text(
            text = "شما می‌توانید برای هر یک از ۴ اسلات زیر، صدای آژیر، پیام اضطراری یا فریاد هشدار ضبط کنید. هنگام ارسال پیامک، بلندگوی دستگاه حتی در وضعیت بی‌صدا (Mute) با حداکثر توان این صدا را پخش خواهد کرد.",
            color = Color.LightGray.copy(alpha = 0.75f),
            fontSize = 11.5.sp,
            lineHeight = 17.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (slot in 1..4) {
                val isRecorded = recordedSlots[slot] == true
                val recordingActive = activeRecording == slot
                val playingActive = activePlaying == slot

                VoiceSlotCard(
                    slot = slot,
                    isRecorded = isRecorded,
                    recordingActive = recordingActive,
                    playingActive = playingActive,
                    onStartRecord = { requestRecordSlot(slot) },
                    onStopRecord = { viewModel.stopRecordingSound() },
                    onStartPlay = { viewModel.startPlaybackSound(slot) },
                    onStopPlay = { viewModel.stopPlaybackSound() },
                    onDelete = { viewModel.deleteSoundSlot(slot) }
                )
            }
        }

        // Section 4: MESSAGES SCREEN PREVIEW
        Text(
            text = "متن پیام قفل‌کننده صفحه نمایش",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = editingMessage,
                    onValueChange = { 
                        editingMessage = it
                        viewModel.setLockScreenMessage(it)
                    },
                    label = { Text("متن هشدار صفحه قفل", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFFFFB300),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_screen_msg_input_settings"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Dispatch lock screen test immediately
                        val testIntent = Intent(context, GuardianLockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            putExtra(GuardianLockActivity.EXTRA_OVERLAY_TYPE, "lock")
                            putExtra(GuardianLockActivity.EXTRA_MESSAGE, editingMessage)
                        }
                        context.startActivity(testIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("preview_lock_screen_button")
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نمایش پیش‌نمایش صفحه قفل گاردین", fontSize = 13.sp)
                }
            }
        }

        // Section 5: DEVICE ADMIN & APP UNINSTALL
        Text(
            text = "مدیریت دستگاه و لغو دسترسی (جهت حذف برنامه)",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdminActive) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f)
            ),
            border = BorderStroke(
                1.dp,
                if (isAdminActive) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAdminActive) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdminActive) Icons.Filled.CheckCircle else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (isAdminActive) Color(0xFF81C784) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAdminActive) "وضعیت: مدیر دستگاه فعال است" else "وضعیت: مدیر دستگاه غیرفعال است",
                            color = if (isAdminActive) Color(0xFF81C784) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAdminActive) "حفاظت ضدسرقت و ضدحذف فعال" else "برنامه آماده لغو نصب یا فعال‌سازی مجدد",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isAdminActive) {
                        "برای جلوگیری از حذف برنامه توسط سارق، گاردین به عنوان مدیر دستگاه فعال است. چنانچه قصد دارید برنامه را حذف کنید، ابتدا باید با لمس دکمه زیر دسترسی مدیر دستگاه را لغو نمایید؛ در غیر این‌صورت دکمه «لغو نصب» در تنظیمات گوشی و کافه بازار کار نخواهد کرد."
                    } else {
                        "دسترسی مدیر دستگاه غیرفعال است. اکنون می‌توانید بدون هیچ مانعی برنامه را مستقیماً لغو نصب نمایید، یا در صورت تمایل برای ارتقای امنیت آن را مجدداً فعال سازید."
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isAdminActive) {
                    Button(
                        onClick = { showDeactivateConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("deactivate_admin_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("غیرفعال‌سازی مدیر دستگاه (جهت حذف برنامه)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val adminComponent = ComponentName(context, com.example.receiver.GuardianDeviceAdminReceiver::class.java)
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "فعال‌سازی مدیر دستگاه جهت جلوگیری از حذف برنامه توسط سارق")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFFFFB300)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reactivate_admin_button")
                        ) {
                            Text("فعال‌سازی مجدد", color = Color(0xFFFFB300), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(uninstallIntent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC62828),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("uninstall_app_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لغو نصب برنامه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showDeactivateConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeactivateConfirmDialog = false },
                title = {
                    Text(
                        text = "لغو دسترسی مدیر دستگاه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "با غیرفعال‌سازی این گزینه، حفاظت ضدحذف برنامه لغو می‌شود و امکان لغو نصب برنامه از تنظیمات اندروید و کافه بازار فراهم می‌گردد. آیا مایل به ادامه هستید؟",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeactivateConfirmDialog = false
                            val removed = viewModel.removeDeviceAdmin()
                            isAdminActive = viewModel.isDeviceAdminActive()
                            android.widget.Toast.makeText(
                                context,
                                if (removed || !isAdminActive) "مدیر دستگاه غیرفعال شد. اکنون می‌توانید برنامه را حذف کنید."
                                else "خطا در لغو مدیر دستگاه",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        )
                    ) {
                        Text("غیرفعال کن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateConfirmDialog = false }) {
                        Text("انصراف", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFFB300)
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

// ═══════════════════════════════════════
// 3. LOGS TAB SCREEN (گزارشات)
// ═══════════════════════════════════════
@Composable
fun LogsTabScreen(viewModel: MainViewModel) {
    val logs by viewModel.eventLogs.collectAsState()
    val activeFilter by viewModel.activeLogFilter.collectAsState()
    val context = LocalContext.current
    
    var activeSubTab by remember { mutableStateOf("events") } // "events" or "photos"
    var showDialogClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Mode Switcher: Events vs Intruder Photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeSubTab = "events" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "events") Color(0xFF1A237E) else Color.White.copy(alpha = 0.05f),
                    contentColor = if (activeSubTab == "events") Color(0xFFFFB300) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (activeSubTab == "events") BorderStroke(1.dp, Color(0xFFFFB300)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("📋 گزارشات وقایع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeSubTab = "photos" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSubTab == "photos") Color(0xFF1A237E) else Color.White.copy(alpha = 0.05f),
                    contentColor = if (activeSubTab == "photos") Color(0xFFFFB300) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (activeSubTab == "photos") BorderStroke(1.dp, Color(0xFFFFB300)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("📸 تصاویر سارقین", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (activeSubTab == "photos") {
            IntruderPhotosGalleryView(context = context)
        } else {
            // Horizontal filter chips in Clean Minimalism style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val chips = listOf(
                    "ALL" to "همه",
                    "ALARM" to "آژیرها",
                    "LOC" to "ردیابی",
                    "SIM_CHANGE" to "سیم‌کارت",
                    "POWER" to "برق/شارژ"
                )

                for ((filter, title) in chips) {
                    val isSelected = activeFilter == filter
                    Button(
                        onClick = { viewModel.setLogFilter(filter) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f),
                            contentColor = if (isSelected) Color(0xFF1A237E) else Color.White
                        ),
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logs listing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "هیچ گزارش امنیتی یافت نشد.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        items(logs) { log ->
                            LogTimelineItem(log = log)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clear logs button
            Button(
                onClick = { showDialogClearConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = ColorxFFFF5252, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_logs_button")
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("پاک کردن کلیه گزارشات", fontWeight = FontWeight.Bold)
            }

            // CONFIRMATION DIALOG
            if (showDialogClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showDialogClearConfirm = false },
                    title = { Text("حذف گزارشات گاردین", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = { Text("آیا واقعاً می‌خواهید سوابق کلیه هشدارها و لاگ‌های امنیتی را برای همیشه حذف کنید؟", color = Color.LightGray) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllLogs()
                                showDialogClearConfirm = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ColorxFFFF5252)
                        ) {
                            Text("بله، حذف کن", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("confirm_clear_logs_button"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialogClearConfirm = false }) {
                            Text("انصراف", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF151515),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

@Composable
fun IntruderPhotosGalleryView(context: Context) {
    var photos by remember { mutableStateOf(CameraHelper.getIntruderPhotos(context)) }
    var selectedPhoto by remember { mutableStateOf<File?>(null) }
    var photoToDelete by remember { mutableStateOf<File?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تصاویر ثبت‌شده سارقین (${photos.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            IconButton(
                onClick = { photos = CameraHelper.getIntruderPhotos(context) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "بروزرسانی",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📸", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "هیچ تصویری از سارق ثبت نشده است",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنگامی که رمز گوشی یا PIN اشتباه وارد شود، یا پیامک #PHOTO[کد]# دریافت شود، تصاویر سارق به‌صورت مخفیانه با دوربین جلو گرفته شده و در این گالری امن نمایش داده می‌شوند.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(photos, key = { it.absolutePath }) { file ->
                        val bitmap = remember(file.absolutePath) {
                            try {
                                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                                BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPhoto = file },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "تصویر سارق",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("📸", fontSize = 20.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تصویر سارق شناسایی‌شده",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "زمان: ${JalaliCalendarHelper.getJalaliDateString(file.lastModified())}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFB300)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "حجم: ${file.length() / 1024} کیلوبایت",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                IconButton(
                                    onClick = { photoToDelete = file },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "حذف عکس",
                                        tint = ColorxFFFF5252,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Photo Preview Dialog
    if (selectedPhoto != null) {
        val fullBitmap = remember(selectedPhoto?.absolutePath) {
            try {
                selectedPhoto?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            title = {
                Text(
                    text = "مشاهده تصویر سارق 📸",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap,
                            contentDescription = "تصویر بزرگ سارق",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("عدم امکان بارگذاری تصویر", color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ثبت شده در: ${selectedPhoto?.let { JalaliCalendarHelper.getJalaliDateString(it.lastModified()) } ?: ""}",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhoto = null }) {
                    Text("بستن", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF151515),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete Confirmation Dialog
    if (photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("حذف تصویر سارق", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("آیا از حذف این تصویر از حافظه امن برنامه مطمئن هستید؟", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToDelete?.let { CameraHelper.deleteIntruderPhoto(it) }
                        photos = CameraHelper.getIntruderPhotos(context)
                        photoToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorxFFFF5252)
                ) {
                    Text("بله، حذف کن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("انصراف", color = Color.White)
                }
            },
            containerColor = Color(0xFF151515),
            shape = RoundedCornerShape(24.dp)
        )
    }
}


@Composable
fun LogTimelineItem(log: LogEntry) {
    val farsiTime = JalaliCalendarHelper.getJalaliDateString(log.timestamp)
    val sysIcon = when (log.eventType) {
        "SIM_CHANGE" -> Icons.Filled.Warning
        "SMS_CMD" -> Icons.Filled.Email
        "ALARM" -> Icons.Filled.Warning
        "LOC" -> Icons.Filled.LocationOn
        "USB" -> Icons.Filled.Build
        "POWER" -> Icons.Filled.Warning
        else -> Icons.Filled.Lock
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = sysIcon,
                contentDescription = null,
                tint = if (log.eventType == "ALARM" || log.eventType == "SIM_CHANGE") ColorxFFFF5252 else Color(0xFFFFB300),
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!log.details.isNullOrEmpty()) {
                    Text(
                        text = log.details,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (!log.location.isNullOrEmpty()) {
                    Text(
                        text = "مختصات: ${log.location}",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = farsiTime,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════
// 4. GUIDE TAB SCREEN (راهنمای استفاده)
// ═══════════════════════════════════════
@Composable
fun GuideTabScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Welcoming Hero Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "راهنمای جامع هوشمند گاردین 🛡️",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "محافظت کاملاً آفلاین و متمرکز بر حریم خصوصی فاقد هرگونه ابزار اینترنتی",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section 1: Step by Step Setup Wizard Flow
        Text(
            text = "۱. راهنمای فعال‌سازی گام‌به‌گام 🚀",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StepRow("۱", "ثبت شماره تلفن اضطراری (بسیار مهم)", "ابتدا به بخش 'تنظیمات' بروید و شماره شخصی امین (همسر، پدر، دوست نزدیک) را با اعداد انگلیسی و فرمت استاندارد (مثال: 09123456789) ثبت کنید. تمام پیامک‌های هشدار ردیابی، شارژ باتری و موقعیت جغرافیایی به این شماره ارسال خواهد شد.")
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 12.dp))
                StepRow("۲", "واگذاری دسترسی‌های امنیتی کامل", "در زمان اجرای اول، برنامه از شما مجوزهای سیستمی (مانند ارسال و خواندن پیامک، مکان‌یابی دقیق GPS و دسترسی ضبط صدا برای پیامگیر) را می‌خواهد. تمام مجوزها را برای کارکرد صحیح در پس‌زمینه تأیید کنید.")
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 12.dp))
                StepRow("۳", "تعیین کدمخفی و رمز عبور از راه دور", "یک کدمخفی ۴ رقمی ایجاد کنید (تنظیمات پیش‌فرض: 1234). تمام دستوراتی که با ارسال پیامک به گوشی کار می‌کنند، برای تأیید اعتبار حتماً باید شامل این کدمخفی باشند.")
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 12.dp))
                StepRow("۴", "مسلح‌سازی (Active/Armed)", "در صفحه 'پیشخوان'، دکمه 'روشن/خاموش' را بزنید تا برنامه وارد وضعیت محافظت مسلحانه شود. در این وضعیت، تمام سنسورهای حرکتی، سوکت شارژر، سیم‌کارت و دکل‌ها فعال و گوش‌به‌زنگ می‌شوند.")
            }
        }

        // Section 2: Troubleshooting and Diagnostics (CRITICAL REUSE FOR USER SATISFACTION)
        Text(
            text = "۲. عیب‌یابی و پاسخ به مشکلات متداول 🛠️",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TroubleItem(
                    title = "چرا ارسال 'پیامک تست' من بی‌نتیجه است و کار نمی‌کند؟",
                    desc = "در صورتی که پیامک تستی به مقصد نمی‌رسد، علت آن را در این ۳ مورد بررسی کنید:\n\n" +
                           "۱. **عدم ثبت شماره گیرنده:** از تنظیم بودن صحیح شماره اضطراری ملّی مطمئن شوید. بدون ست کردن شماره در تنظیمات، ارسال لغو می‌گردد.\n" +
                           "۲. **محدودیت شبیه‌سازها (Emulator):** اگر سیستم گاردین را در شبیه‌ساز کامپیوتر یا مرورگر وب تست می‌کنید، شبیه‌سازها به شبکه مخابرات فیزیکی مجهز نیستند و نمی‌توانند پیامک فیزیکی ارسال کنند. با این وجود، برنامه گاردین کاملاً هوشمند است و جزئیات دقیق خطا یا تلاش برای ارسال را به همراه متن خطا بلافاصله در زبانه 'گزارشات' ثبت می‌کند تا به شما ثابت کند فرآیند کاملاً بیدار و در جریان است!\n" +
                           "۳. **مجوزهای لغو شده اندروید:** اگر مجوزهای پیامک (SEND_SMS یا READ_SMS) توسط شما یا سیستم لغو شده باشد، ارسال پیامک متوقف می‌شود. برای بررسی دقیق‌تر، حتماً زبانه 'گزارشات' را چک کنید تا جزئیات کدهای خطای سیستمی را ببینید."
                )
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 12.dp))
                TroubleItem(
                    title = "چرا برخی سنسورها مثل شارژر یا USB آلارم نمی‌دهند؟",
                    desc = "باید توجه داشته باشید که سنسورهای قطع شارژر، اتصال غیرمجاز کابل USB، یا جابجایی فیزیکی سطوح، تنها در صورتی فعال می‌شوند که سیستم گاردین **'مسلح' (Armed)** باشد. اگر دکمه فعال‌سازی بزرگ صفحه پیشخوان غیرفعال باشد، این سنسورها جهت جلوگیری از مزاحمت صوتی در شرایط عادی، زنگ نمی‌زنند."
                )
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 12.dp))
                TroubleItem(
                    title = "سیستم 'سایلنت شکست' آژیر گاردین چیست؟",
                    desc = "یکی از بزرگترین قابلیتهای گاردین این است که حتی اگر گوشی شما فیزیکاً در حالت 'بی‌صدا' (Silent)، با صدای پایین (Mute) یا حاوی بازه در آرامش (Do Not Disturb) باشد، به صورت خودکار ولوم خروجی را بر روی بالاترین حد مجاز بلندگو (Stream Alarm Booster) تنظیم و قفل کرده و فریاد آژیر را ممتد و توقف‌ناپذیر پخش خواهد کرد!"
                )
            }
        }

        // Section 3: Sensor Behaviors Detailed Descriptions
        Text(
            text = "۳. نحوه عملکرد نگهبانان ضد سرقت فیزیکی 👁️",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SensorBehaviorRow(
                Icons.Filled.Lock,
                "محافظ سیم‌کارت (SIM Watcher)",
                "گاردین در پس‌زمینه سریال فنی سیم‌کارت فعال شما را ردیابی می‌کند. در صورتی که سارق سیم‌کارت شما را خارج کند و سیم‌باند غریبه‌ای قرار دهد، به محض بالا آمدن و اولین بوت مخفیانه، مشخصات اپراتور جدید، سریال فنی سیمکلاس و موقعیت دکل رادیویی آن، بی سروصدا به شماره اضطراری مالک اول ارسال خواهد شد."
            )
            SensorBehaviorRow(
                Icons.Filled.Warning,
                "سنسور کابل داده USB و نفوذ اطلاعات",
                "با اتصال هرگونه کابل USB به دستگاه در حالت مسلح، گاردین آن را به عنوان اقدام سارق برای فلش کردن، قفل‌شکنی یا انتقال تصاویر شخصی تلقی کرده، بلافاصله صفحه گوشی را با پیام هشداری قرمز قفل سنگین کرده و شروع به پخش بلندترین آژیر خطای متوالی و ممتد فیزیکی می‌نماید."
            )
            SensorBehaviorRow(
                Icons.Filled.LocationOn,
                "ردیاب گام‌به‌گام موقعیت آفلاین (GPS-Triangulation)",
                "بدون نیاز به شبکه اینترنتی یا دیتا، گاردین با به‌کارگیری سخت‌افزار GPS آفلاین گوشی و دریافت شناسه سلولهای دکل رادیویی فعال، موقعیت جغرافیایی را استخراج ساخته و در قالب لینک‌های جهانی نقشه گوگل (Google Maps) پیام فرست می‌کند."
            )
            SensorBehaviorRow(
                Icons.Filled.Phone,
                "سیستم بلوتوث محافظ هوشمند (Bluetooth Guardian)",
                "شما می‌توانید گوشی خود را به ساعت هوشمند ورزشی، دستبند یا هندزفری بلوتوث نزدیک خود جفت کنید. در صورت بروز هرگونه سرقت و جابجا شدن ناگهانی گوشی از محدوده امن شما، به محض قطع ارتباط رادیویی بلوتوث بیش از تایم تعریف‌شده شما، زنگ پیش‌مقدماتی و پیامک اضطراری خطر به‌کار می‌افتد."
            )
        }

        // Section 4: Comprehensive SMS Commands reference table
        Text(
            text = "۴. راهنمای کدهای کنترلی پیامکی از راه دور ✉️",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "با شماره گیرنده اضطراری خود، کدهای دستوری زیر را با قالب‌بندی هشتگ (#) به گوشی هدف پیامک کنید. توجه کنید تمامی کاراکترها باید عینا شبیه الگو باشند و هیچ فاصله (اسپیس) اضافی در پیامک وجود نداشته باشد:",
                    color = Color.LightGray.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SmsCommandGuideRow(
                    title = "یافتن مکان و ردیابی لحظه‌ای",
                    cmd = "#LOC[کد]#",
                    example = "#LOC1234#"
                )

                SmsCommandGuideRow(
                    title = "قفل فوری گوشی و نمایش صفحه مسدودی",
                    cmd = "#LOCK[کد]#",
                    example = "#LOCK1234#"
                )

                SmsCommandGuideRow(
                    title = "به صدا درآوردن آژیر ممتد امنیتی",
                    cmd = "#ALARM[کد]#",
                    example = "#ALARM1234#"
                )

                SmsCommandGuideRow(
                    title = "ارسال پیام متنی دلخواه برای نمایش به سارق",
                    cmd = "#MSG[کد][متن]#",
                    example = "#MSG1234گوشی من دزدیده شده است#"
                )

                SmsCommandGuideRow(
                    title = "دریافت وضعیت کامل (باتری، سیم‌کارت، شارژر)",
                    cmd = "#STATUS[کد]#",
                    example = "#STATUS1234#"
                )

                SmsCommandGuideRow(
                    title = "فعال‌سازی آلارم زمان‌شمار معکوس پلیس",
                    cmd = "#TIMER[کد]#",
                    example = "#TIMER1234#"
                )

                SmsCommandGuideRow(
                    title = "پخش صدای ضبط شده اضطراری شما",
                    cmd = "#SOUND[اسلات]_[کد]#",
                    example = "#SOUND1_1234#"
                )

                SmsCommandGuideRow(
                    title = "بازگشای قفل گاردین از راه دور",
                    cmd = "#UNLOCK[کد]#",
                    example = "#UNLOCK1234#"
                )

                SmsCommandGuideRow(
                    title = "فرمان بازنشانی کارخانه و پاکسازی کامل دستگاه (Factory Reset)",
                    cmd = "#WIPE[کد]#",
                    example = "#WIPE1234#"
                )

                SmsCommandGuideRow(
                    title = "عکاسی مخفیانه از سارق با دوربین جلو و ثبت در حافظه امن",
                    cmd = "#PHOTO[کد]#",
                    example = "#PHOTO1234#"
                )
            }
        }

        // Section 5: Comprehensive Detailed Feature Explanations
        Text(
            text = "۵. تشریح کاربرد و مکانیزم قابلیت‌های گاردین 🛡️",
            color = Color(0xFFFFB300),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 قفل دوقلو هوشمند (ورود اولیه و کلیه برنامه‌ها):",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "سیستم گاردین مجهز به یک دیوار امنیتی نفوذناپذیر است که می‌تواند روی ورود اولیه دستگاه و همچنین تمامی برنامه‌های نصب‌شده گوشی قفل محکم اعمال کند. در صورت نیاز به کنترل مصلحت‌آمیز، می‌توانید حتی از فرسنگ‌ها دورتر با ارسال پیامک رمزدار (مانند کدهای دستوری UNLOCK یا LOCK) از طریق خط اضطراری خود، وضعیت قفل گوشی را به صورت زنده و آنی باز یا مجدداً مسدود کنید.",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "📸 سلفی مخفیانه و بی‌صدا از سارق (Intruder Selfie):",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "به محض فعال شدن این سنسور، اگر سارق یا فرد کنجکاو تلاش کند پین ورود یا رمز برنامه‌های قفل شده شما را به طور نادرست وارد نماید، دوربین جلوی گوشی کاملاً هوشمند، بی‌صدا و بدون هیچ شاتر، لرزش یا نشانه‌ای از چهره وی عکس مخفیانه می‌گیرد و فوراً در بخش عکس‌های محرمانه گالری ذخیره می‌کند تا سند تصویری جرم ثبت گردد.",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "🗑️ پاکسازی و وایپ اضطراری حریم خصوصی (Remote Wipe):",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "بزرگترین سلاح صیانت از آبرو و اسرار شخصی شما! چنانچه گوشی به طور دائم به سرقت رفته و امیدی به استرداد آن نباشد، تنها با فرستادن یک دستور متنی هشتگ‌دار خصوصی، برنامه تمام توان خود را به کار بسته و فوراً کلیه پیامک‌ها، لیست مخاطبین، و تمامی فایل‌های صوتی و تصویری گالری حافظه موانع را به کل نابود و فورمت می‌کند تا هیچ‌گونه سوءاستفاده‌ای از اطلاعات شخصی شما صورت نگیرد.",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "📢 آژیر ضد سکوت و اسلات‌های طلایی آوا:",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "طراح خلاق برنامه ۴ اسلات صوتی اختصاصی برای شما فراهم آورده است. شما می‌توانید صدای ضبط شده هشدار خود، صدای داد و فریاد، آژیر فیزیکی یا راهنمایی پلیس را پیش‌ضبط کنید و با ارسال دستور صوتی پیامکی، آژیر ضد سکوت را منفجر ساخته تا حتی در حالت بی‌صدا (Mute)، صدای از پیش ضبط شده شما با حداکثر توان بلندگوی دستگاه پخش شده و سارق را در ملاء عام رسوا نماید.",
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Creator blinking heart signature
        CreatorSignatureRow()
    }
}

@Composable
fun CreatorSignatureRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "heart_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "طراحی و توسعه",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Love",
                tint = Color(0xFFE53935),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "آزاد آذرخش",
                color = Color(0xFFFFB300),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StepRow(stepNum: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFFFB300), CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                color = Color(0xFF1E1E1E),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = Color.LightGray,
                fontSize = 11.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TroubleItem(title: String, desc: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(24.dp)
            )
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                color = Color.LightGray,
                fontSize = 11.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun SensorBehaviorRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SmsCommandGuideRow(
    title: String,
    cmd: String,
    example: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Line 1: Title & Purpose description
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Line 2: The Command pattern
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کد دستوری:",
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Right
            )
            Text(
                text = cmd,
                color = Color(0xFFFFB300),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                ),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Line 3: Example code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مثال واقعی ارسال:",
                color = Color.LightGray.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Right
            )
            Text(
                text = example,
                color = Color(0xFF81C784),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(
                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                ),
                modifier = Modifier
                    .background(Color(0x1F81C784), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FaqItem(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = q, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = a, color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

// Global Color representation fix
val ColorxFFFF5252 = Color(0xFFFF5252)


