package com.cs407.soundscape.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.soundscape.ui.screens.AnalyticsScreen
import com.cs407.soundscape.ui.screens.HistoryScreen
import com.cs407.soundscape.ui.screens.HomeScreen
import com.cs407.soundscape.ui.screens.LoginScreen
import com.cs407.soundscape.ui.screens.MapScreen
import com.cs407.soundscape.ui.screens.ScanScreen
import com.cs407.soundscape.ui.screens.SettingsScreen

@Composable
fun SoundScapeNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Map,
        Screen.Scan,
        Screen.History,
        Screen.Analytics
    )

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != Screen.Login.route) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                // Special handling for Home - always navigate to it
                                if (screen is Screen.Home) {
                                    // If already on Home, just pop any screens above it
                                    if (currentDestination?.route == Screen.Home.route) {
                                        // Already on Home, pop everything else
                                        navController.popBackStack(Screen.Home.route, inclusive = false)
                                    } else {
                                        // Navigate to Home and pop everything else
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = false
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    // For other screens, navigate normally
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Scan.route) {
                ScanScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

// Extension property for icons (using Material Icons)
private val Screen.icon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        is Screen.Home -> Icons.Default.Home
        is Screen.Map -> Icons.Default.Map
        is Screen.Scan -> Icons.Default.QrCodeScanner
        is Screen.History -> Icons.Default.History
        is Screen.Analytics -> Icons.Default.Analytics
        is Screen.Settings -> Icons.Default.Settings
        is Screen.Login -> Icons.Default.Person
    }
