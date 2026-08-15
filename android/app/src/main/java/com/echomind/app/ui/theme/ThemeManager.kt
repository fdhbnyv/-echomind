package com.echomind.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * EchoMind 主题管理器。
 *
 * 使用 Compose [mutableStateOf] 保证主题切换后 UI 即时响应。
 */
object ThemeManager {

    /** 当前主题（Compose 状态，修改后自动触发重组） */
    var currentTheme: AppTheme by mutableStateOf(AppTheme.LIQUID_GLASS)
        private set

    /** 当前主题的亮色配色 */
    fun lightColors(): ThemeColors = currentTheme.light

    /** 当前主题的暗色配色 */
    fun darkColors(): ThemeColors = currentTheme.dark

    /** 切换主题 */
    fun setTheme(theme: AppTheme) {
        currentTheme = theme
    }

    /** 通过 id 切换主题 */
    fun setThemeById(id: String) {
        currentTheme = AppTheme.entries.find { it.id == id } ?: AppTheme.LIQUID_GLASS
    }
}
