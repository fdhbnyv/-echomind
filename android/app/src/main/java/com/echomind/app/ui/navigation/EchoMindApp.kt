package com.echomind.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echomind.app.ui.screens.HistoryScreen
import com.echomind.app.ui.screens.HomeScreen
import com.echomind.app.ui.screens.SettingsScreen
import com.echomind.app.ui.theme.Primary
import com.echomind.app.ui.theme.TextDim

private enum class Tab(val label: String) {
    HOME("写"),
    HISTORY("记录"),
    SETTINGS("设置"),
}

@Composable
fun EchoMindApp() {
    var selectedTab by remember { mutableStateOf(Tab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == Tab.HOME,
                    onClick = { selectedTab = Tab.HOME },
                    icon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "写",
                        )
                    },
                    label = { Text("写", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors(selectedTab == Tab.HOME),
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.HISTORY,
                    onClick = { selectedTab = Tab.HISTORY },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "记录",
                        )
                    },
                    label = { Text("记录", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors(selectedTab == Tab.HISTORY),
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.SETTINGS,
                    onClick = { selectedTab = Tab.SETTINGS },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    },
                    label = { Text("设置", style = MaterialTheme.typography.labelSmall) },
                    colors = navColors(selectedTab == Tab.SETTINGS),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (selectedTab) {
            Tab.HOME -> HomeScreen(
                modifier = Modifier.padding(padding),
                onNavigateToSettings = { selectedTab = Tab.SETTINGS },
                onNavigateToHistory = { selectedTab = Tab.HISTORY },
            )
            Tab.HISTORY -> HistoryScreen(modifier = Modifier.padding(padding))
            Tab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(padding))
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
