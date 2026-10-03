package com.echomind.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echomind.app.ui.screens.HistoryScreen
import com.echomind.app.ui.screens.HomeScreen
import com.echomind.app.ui.screens.SettingsScreen
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.TextDim

private enum class Tab(val label: String, val index: Int) {
    HOME("写", 0),
    HISTORY("记录", 1),
    SETTINGS("设置", 2),
}

@Composable
fun EchoMindApp() {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

    // 当在设置页面时，按系统返回键返回主页
    if (selectedTab == Tab.SETTINGS) {
        BackHandler {
            selectedTab = Tab.HOME
        }
    }

    Scaffold(
        bottomBar = {
            // 仅在主导航页面（写 / 记录）显示底部导航栏，设置页作为沉浸式页面
            if (selectedTab != Tab.SETTINGS) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = selectedTab == Tab.HOME,
                        onClick = { selectedTab = Tab.HOME },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.echomind.app.R.drawable.ic_tab_edit),
                                contentDescription = "记录",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("记录", style = MaterialTheme.typography.labelSmall) },
                        colors = navColors(selectedTab == Tab.HOME),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Tab.HISTORY,
                        onClick = { selectedTab = Tab.HISTORY },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.echomind.app.R.drawable.ic_tab_library),
                                contentDescription = "外脑库",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("外脑库", style = MaterialTheme.typography.labelSmall) },
                        colors = navColors(selectedTab == Tab.HISTORY),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val duration = 280
                val forward = targetState.index > initialState.index
                val slideOffset: (Int) -> Int = { fullWidth ->
                    if (forward) (fullWidth * 0.12f).toInt() else (-fullWidth * 0.12f).toInt()
                }
                val exitOffset: (Int) -> Int = { fullWidth ->
                    if (forward) (-fullWidth * 0.12f).toInt() else (fullWidth * 0.12f).toInt()
                }

                (slideInHorizontally(
                    animationSpec = tween(duration, easing = FastOutSlowInEasing),
                    initialOffsetX = slideOffset
                ) + fadeIn(
                    animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing),
                        targetOffsetX = exitOffset
                    ) + fadeOut(
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                )
            },
            label = "tab_transition",
            modifier = Modifier.padding(padding),
        ) { targetTab ->
            when (targetTab) {
                Tab.HOME -> HomeScreen(
                    onNavigateToSettings = { selectedTab = Tab.SETTINGS },
                    onNavigateToHistory = { selectedTab = Tab.HISTORY },
                )
                Tab.HISTORY -> HistoryScreen(
                    onNavigateToSettings = { selectedTab = Tab.SETTINGS },
                )
                Tab.SETTINGS -> SettingsScreen(
                    onBack = { selectedTab = Tab.HOME }
                )
            }
        }
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = Primary,
    selectedTextColor = Primary,
    unselectedIconColor = TextDim,
    unselectedTextColor = TextDim,
    indicatorColor = MaterialTheme.colorScheme.surface,
)
