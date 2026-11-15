package com.learnlock.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.learnlock.app.LearnLockApp
import com.learnlock.app.ui.screens.HomeScreen
import com.learnlock.app.ui.screens.OnboardingScreen
import com.learnlock.app.ui.screens.ParentPinSetupScreen
import com.learnlock.app.ui.screens.PermissionsScreen
import com.learnlock.app.ui.screens.SettingsScreen
import com.learnlock.app.ui.screens.StatsScreen
import com.learnlock.app.ui.theme.LearnLockTheme
import com.learnlock.app.ui.viewmodel.MainViewModel
import com.learnlock.app.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LearnLockApp
        setContent {
            LearnLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LearnLockNav(app)
                }
            }
        }
    }
}

@Composable
private fun LearnLockNav(app: LearnLockApp) {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel(
        factory = MainViewModelFactory(app, app.container.repository)
    )

    LaunchedEffect(Unit) {
        vm.bootstrap()
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                state = vm.uiState,
                onOpenOnboarding = { navController.navigate("onboarding") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenStats = { navController.navigate("stats") },
                onOpenPermissions = { navController.navigate("permissions") },
                onToggleLock = { enabled -> vm.setLockEnabled(enabled) },
                onPreviewLock = { vm.previewLock() },
                onEndSession = { vm.endSession() }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                hasPin = vm.uiState.hasParentPin,
                onContinueToPin = { navController.navigate("pin_setup") },
                onContinueToPermissions = { navController.navigate("permissions") },
                onDone = { navController.popBackStack() }
            )
        }
        composable("pin_setup") {
            ParentPinSetupScreen(
                onSave = { pin ->
                    vm.setPin(pin)
                    navController.navigate("permissions") {
                        popUpTo("onboarding")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("permissions") {
            PermissionsScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                profile = vm.uiState.profile,
                onSave = { profile ->
                    vm.saveProfile(profile)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("stats") {
            StatsScreen(
                attempts = vm.uiState.attempts,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
