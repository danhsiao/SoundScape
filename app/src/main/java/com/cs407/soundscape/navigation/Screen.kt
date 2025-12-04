package com.cs407.soundscape.navigation

sealed class Screen(val route: String, val title: String) {
    object SignIn : Screen("signIn", "Sign In")
    object SignUp : Screen("signUp", "Sign Up")
    object Login : Screen("login", "Login")
    object Home : Screen("home", "Home")
    object Map : Screen("map", "Map")
    object Scan : Screen("scan", "Scan")
    object History : Screen("history", "History")
    object Analytics : Screen("analytics", "Analytics")
    object Settings : Screen("settings", "Settings")
    object Test : Screen("roomTest", "Test")
}

