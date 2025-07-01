package com.steptracker.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Walk : Screen("walk")
    object History : Screen("history")
    object Goals : Screen("goals")
    object Settings : Screen("settings")
} 