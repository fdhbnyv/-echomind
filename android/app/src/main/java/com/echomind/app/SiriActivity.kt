package com.echomind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.echomind.app.ui.components.SiriOverlay
import com.echomind.app.ui.theme.EchoMindTheme

/**
 * Siri 式语音交互 Activity — 透明全屏覆盖层。
 *
 * 从桌面 Widget 点击启动，提供 Siri 风格的语音录入体验。
 * 使用透明主题 + EdgeToEdge 实现"悬浮于桌面"的效果。
 */
class SiriActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark = isSystemInDarkTheme()
            EchoMindTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SiriOverlay(
                        isDark = isDark,
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }
}
