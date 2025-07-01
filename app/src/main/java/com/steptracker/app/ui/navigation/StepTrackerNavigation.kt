package com.steptracker.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.steptracker.app.R
import com.steptracker.app.ui.screens.GoalsScreen
import com.steptracker.app.ui.screens.HistoryScreen
import com.steptracker.app.ui.screens.HomeScreen
import com.steptracker.app.ui.screens.SettingsScreen
import com.steptracker.app.ui.screens.WalkScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepTrackerNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf(
                    Screen.Home to Icons.Filled.Home,
                    Screen.Walk to Icons.Filled.DirectionsWalk,
                    Screen.History to Icons.Filled.History,
                    Screen.Goals to Icons.Filled.Flag,
                    Screen.Settings to Icons.Filled.Settings
                )

                screens.forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = {
                            Text(
                                text = when (screen) {
                                    Screen.Home -> stringResource(R.string.nav_home)
                                    Screen.Walk -> stringResource(R.string.nav_walk)
                                    Screen.History -> stringResource(R.string.nav_history)
                                    Screen.Goals -> stringResource(R.string.nav_goals)
                                    Screen.Settings -> stringResource(R.string.nav_settings)
                                }
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Walk.route) {
                WalkScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Goals.route) {
                GoalsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
} 