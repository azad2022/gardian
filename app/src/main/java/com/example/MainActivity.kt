package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingWizard
import com.example.ui.theme.GuardianTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GuardianTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F0F) // Set dark base background
                ) {
                    val isOnboarded by viewModel.isOnboarded.collectAsState()
                    var isUnlocked by remember { mutableStateOf(false) }

                    if (isOnboarded) {
                        if (!isUnlocked) {
                            AppLockScreen(onUnlockSuccess = { isUnlocked = true })
                        } else {
                            DashboardScreen(viewModel = viewModel)
                        }
                    } else {
                        OnboardingWizard(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

