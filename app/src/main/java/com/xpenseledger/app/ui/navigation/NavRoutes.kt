package com.xpenseledger.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining every navigation destination in the app.
 *
 * [bottomNavItems] lists the three items shown in the BottomNavBar in order.
 */
sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object AddExpense : Screen("add_expense")   // kept for navigation — not shown in bottom bar
    object Analytics  : Screen("analytics")
    object Profile    : Screen("profile")
}

data class BottomNavItem(
    val screen:       Screen,
    val label:        String,
    val selectedIcon: ImageVector,
    val defaultIcon:  ImageVector
)

/** Three-item bottom nav — Add Expense is now a FAB, not a tab. */
val bottomNavItems = listOf(
    BottomNavItem(
        screen       = Screen.Home,
        label        = "Home",
        selectedIcon = Icons.Filled.Home,
        defaultIcon  = Icons.Outlined.Home
    ),
    BottomNavItem(
        screen       = Screen.Analytics,
        label        = "Analytics",
        selectedIcon = Icons.Filled.BarChart,
        defaultIcon  = Icons.Outlined.BarChart
    ),
    BottomNavItem(
        screen       = Screen.Profile,
        label        = "Profile",
        selectedIcon = Icons.Filled.Person,
        defaultIcon  = Icons.Outlined.Person
    )
)
