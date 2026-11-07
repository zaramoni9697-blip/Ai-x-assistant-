package com.jarvis.ai

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.ai.core.voice.WakeWordService
import com.jarvis.ai.feature.admin.AdminScreen
import com.jarvis.ai.feature.auth.ForgotScreen
import com.jarvis.ai.feature.auth.LoginScreen
import com.jarvis.ai.feature.auth.SignupScreen
import com.jarvis.ai.feature.chat.ChatScreen
import com.jarvis.ai.feature.coding.CodingScreen
import com.jarvis.ai.feature.dashboard.DashboardScreen
import com.jarvis.ai.feature.device.DeviceScreen
import com.jarvis.ai.feature.memory.MemoryScreen
import com.jarvis.ai.feature.profile.ProfileScreen
import com.jarvis.ai.feature.settings.SettingsScreen
import com.jarvis.ai.feature.splash.SplashScreen
import com.jarvis.ai.feature.voice.VoiceScreen
import com.jarvis.ai.ui.navigation.Routes
import com.jarvis.ai.ui.theme.JarvisBlack
import com.jarvis.ai.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBaselinePermissions()
        setContent {
            JarvisTheme {
                Surface(Modifier.fillMaxSize(), color = JarvisBlack) {
                    val nav = rememberNavController()
                    var wakeQuery by remember { mutableStateOf(intent?.getStringExtra(WakeWordService.EXTRA_QUERY)) }
                    LaunchedEffect(intent) {
                        if (intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE, false) == true) {
                            wakeQuery = intent.getStringExtra(WakeWordService.EXTRA_QUERY)
                            nav.navigate(Routes.Voice)
                        }
                    }
                    NavHost(navController = nav, startDestination = Routes.Splash) {
                        composable(Routes.Splash) {
                            SplashScreen { logged ->
                                nav.navigate(if (logged) Routes.Dashboard else Routes.Login) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            }
                        }
                        composable(Routes.Login) {
                            LoginScreen(
                                onLoggedIn = {
                                    nav.navigate(Routes.Dashboard) {
                                        popUpTo(Routes.Login) { inclusive = true }
                                    }
                                },
                                onSignup = { nav.navigate(Routes.Signup) },
                                onForgot = { nav.navigate(Routes.Forgot) }
                            )
                        }
                        composable(Routes.Signup) {
                            SignupScreen(
                                onLoggedIn = {
                                    nav.navigate(Routes.Dashboard) {
                                        popUpTo(Routes.Login) { inclusive = true }
                                    }
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable(Routes.Forgot) {
                            ForgotScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.Dashboard) {
                            DashboardScreen { route -> nav.navigate(route) }
                        }
                        composable(Routes.Chat) {
                            ChatScreen(onBack = { nav.popBackStack() }, onVoice = { nav.navigate(Routes.Voice) })
                        }
                        composable(Routes.Voice) {
                            VoiceScreen(onBack = { nav.popBackStack() }, initialQuery = wakeQuery)
                        }
                        composable(Routes.Coding) { CodingScreen(onBack = { nav.popBackStack() }) }
                        composable(Routes.Memory) { MemoryScreen(onBack = { nav.popBackStack() }) }
                        composable(Routes.Device) { DeviceScreen(onBack = { nav.popBackStack() }) }
                        composable(Routes.Profile) {
                            ProfileScreen(
                                onBack = { nav.popBackStack() },
                                onLoggedOut = {
                                    nav.navigate(Routes.Login) {
                                        popUpTo(Routes.Dashboard) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.Settings) { SettingsScreen(onBack = { nav.popBackStack() }) }
                        composable(Routes.Admin) { AdminScreen(onBack = { nav.popBackStack() }) }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestBaselinePermissions() {
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(needed.toTypedArray())
    }
}
