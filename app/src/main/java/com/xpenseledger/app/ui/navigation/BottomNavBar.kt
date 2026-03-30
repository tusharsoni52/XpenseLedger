package com.xpenseledger.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

/**
 * Three-tab bottom navigation bar: Home · Analytics · Profile.
 * "Add Expense" has been moved to a FAB — no longer lives here.
 *
 * Design:
 *  • Dark `#0D1117` surface matching the dashboard background
 *  • Active item: teal icon tint + animated scale-up + accent indicator
 *  • Thin teal→indigo gradient top-border accent line
 *  • Respects navigation bar window insets
 */
@Composable
fun AppBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color(0xFF0D1117),
        shadowElevation = 16.dp,
        tonalElevation  = 0.dp
    ) {
        Box {
            // ── Gradient top accent line ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(XpensePrimary, XpenseSecondary)))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .selectableGroup()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    RegularNavItem(
                        item     = item,
                        selected = currentRoute == item.screen.route,
                        onClick  = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Regular nav item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RowScope.RegularNavItem(
    item:     BottomNavItem,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue   = if (selected) XpensePrimary else Color(0xFF6B7280),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "iconColor_${item.label}"
    )
    val labelColor by animateColorAsState(
        targetValue   = if (selected) XpensePrimary else Color(0xFF6B7280),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "labelColor_${item.label}"
    )
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale_${item.label}"
    )

    NavigationBarItem(
        selected  = selected,
        onClick   = onClick,
        icon      = {
            Icon(
                imageVector        = if (selected) item.selectedIcon else item.defaultIcon,
                contentDescription = item.label,
                tint               = iconColor,
                modifier           = Modifier.size(24.dp).scale(scale)
            )
        },
        label     = {
            Text(
                text       = item.label,
                fontSize   = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = labelColor
            )
        },
        colors    = NavigationBarItemDefaults.colors(
            selectedIconColor   = XpensePrimary,
            unselectedIconColor = Color(0xFF6B7280),
            selectedTextColor   = XpensePrimary,
            unselectedTextColor = Color(0xFF6B7280),
            indicatorColor      = XpensePrimary.copy(alpha = 0.12f)
        ),
        modifier  = Modifier.weight(1f)
    )
}
