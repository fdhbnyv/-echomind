package com.echomind.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.echomind.app.data.repository.SettingsRepository
import com.echomind.app.ui.navigation.EchoMindApp
import com.echomind.app.ui.screens.SplashScreen
import com.echomind.app.ui.screens.dataStore
import com.echomind.app.ui.theme.EchoMindTheme
import com.echomind.app.ui.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasAudioPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasAudioPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val settingsRepo = SettingsRepository(application.dataStore)
            val s = settingsRepo.settings.first()
            ThemeManager.setThemeById(s.selectedTheme)
            ThemeManager.updateDarkMode(s.isDarkMode)
        }

        hasAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            EchoMindTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isSplashFinished by rememberSaveable { mutableStateOf(false) }

                    AnimatedContent(
                        targetState = isSplashFinished,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "SplashTransition"
                    ) { finished ->
                        if (!finished) {
                            SplashScreen(
                                onSplashFinished = {
                                    isSplashFinished = true
                                    requestNotificationPermissionIfNeeded()
                                }
                            )
                        } else {
                            EchoMindApp()
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
