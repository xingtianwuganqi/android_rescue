package com.rescue.flutter_720yun.demo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomTab("home", "Home", Icons.Default.Home)
    object Profile : BottomTab("profile", "Profile", Icons.Default.Person)
    object Settings : BottomTab("settings", "Settings", Icons.Default.Settings)
}