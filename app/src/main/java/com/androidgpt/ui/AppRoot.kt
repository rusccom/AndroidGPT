package com.androidgpt.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.androidgpt.features.assistant.ui.AssistantScreen
import com.androidgpt.features.expert.ui.ExpertScreen
import com.androidgpt.features.home.HomeScreen
import com.androidgpt.features.local_llm.ui.ModelsScreen
import com.androidgpt.features.settings.SettingsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Outlined.Home),
    Tab("assistant", "Ассистент", Icons.Outlined.Mic),
    Tab("models", "Модели", Icons.Outlined.Storage),
    Tab("settings", "Настройки", Icons.Outlined.Settings),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentRoute?.let { r ->
                        entry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    } ?: false
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") { HomeScreen(onOpenAssistant = { nav.navigate("assistant") }) }
            composable("assistant") { AssistantScreen(onOpenExpert = { nav.navigate("expert") }) }
            composable("models") { ModelsScreen() }
            composable("settings") { SettingsScreen() }
            composable("expert") { ExpertScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
