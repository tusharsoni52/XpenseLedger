package com.xpenseledger.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xpenseledger.app.ui.components.AddExpenseFab
import com.xpenseledger.app.ui.screens.add.AddExpenseScreen
import com.xpenseledger.app.ui.screens.comparison.ComparisonScreen
import com.xpenseledger.app.ui.screens.home.HomeScreen
import com.xpenseledger.app.ui.screens.profile.ProfileScreen
import com.xpenseledger.app.ui.viewmodel.CategoryViewModel
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import com.xpenseledger.app.ui.viewmodel.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  App navigation graph
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root composable that owns the [NavHostController] and the [Scaffold] with
 * the [AppBottomNavBar].
 *
 * Back-stack behaviour:
 *  • Home / Analytics are treated as top-level destinations (saveState=true).
 *  • Add Expense navigates to a full-screen form; pressing ✕ or submitting
 *    pops back to the previous destination (no back-stack accumulation).
 *  • The bottom bar is hidden on the Add Expense screen so the form is
 *    completely unobstructed.
 *
 * FAB visibility:
 *  • Shown on Home and Analytics
 *  • Hidden on AddExpense (full-screen form) and Profile
 */
@Composable
fun AppNavGraph(
    expenseVm:      ExpenseViewModel,
    categoryVm:     CategoryViewModel,
    profileVm:      UserProfileViewModel,
    onUserActivity: () -> Unit = {},
    onLogout:       () -> Unit = {},
    navController:  NavHostController = rememberNavController()
) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route
    val editingExpense by expenseVm.editingExpense.collectAsState()

    // Routes where the bottom bar should be visible
    val showBottomBar = currentRoute != Screen.AddExpense.route

    // Routes where the FAB should be visible (Home + Analytics only)
    val showFab = currentRoute in setOf(Screen.Home.route, Screen.Analytics.route)

    Scaffold(
        containerColor      = Color.Transparent,
        bottomBar           = { if (showBottomBar) AppBottomNavBar(navController) },
        floatingActionButton = {
            if (showFab) {
                AddExpenseFab(
                    onClick = {
                        navController.navigate(Screen.AddExpense.route) {
                            // Don't add AddExpense to the back-stack on top of itself
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController       = navController,
            startDestination    = Screen.Home.route,
            modifier            = Modifier
                .padding(innerPadding)
                // Reset inactivity timer on every touch anywhere in the nav host
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            onUserActivity()
                        }
                    }
                },
            enterTransition     = { fadeIn(tween(220)) },
            exitTransition      = { fadeOut(tween(180)) },
            popEnterTransition  = { fadeIn(tween(220)) },
            popExitTransition   = { fadeOut(tween(180)) }
        ) {
            // ── Home ──────────────────────────────────────────────────────────
            composable(route = Screen.Home.route) {
                HomeScreen(vm = expenseVm, categoryVm = categoryVm)
            }

            // ── Add Expense (full-screen, no bottom bar) ──────────────────────
            composable(
                route             = Screen.AddExpense.route,
                enterTransition   = { slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) },
                exitTransition    = { slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240)) },
                popExitTransition = { slideOutHorizontally(tween(240)) { it } + fadeOut(tween(240)) }
            ) {
                // Capture editing expense in local variable (non-delegated) to enable smart cast
                val currentEditingExpense = editingExpense
                
                AddExpenseScreen(
                    editExpense  = currentEditingExpense,  // Now passes the editing expense (null if adding)
                    categoryVm   = categoryVm,
                    onDismiss    = { 
                        expenseVm.clearEditingExpense()
                        navController.popBackStack() 
                    },
                    onConfirm    = { title, amount, category, subCategory,
                                     categoryId, subCategoryId, timestamp, type ->
                        if (currentEditingExpense != null) {
                            // Update existing expense — preserve type
                            expenseVm.updateExpense(currentEditingExpense.copy(
                                title         = title,
                                amount        = amount,
                                category      = category,
                                subCategory   = subCategory,
                                categoryId    = categoryId,
                                subCategoryId = subCategoryId,
                                timestamp     = timestamp,
                                type          = type
                            ))
                        } else {
                            // Add new expense — pass type through
                            expenseVm.addExpense(
                                title         = title,
                                amount        = amount,
                                category      = category,
                                subCategory   = subCategory,
                                categoryId    = categoryId,
                                subCategoryId = subCategoryId,
                                timestamp     = timestamp,
                                type          = type
                            )
                        }
                        expenseVm.selectMonth(
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))
                        )
                        expenseVm.clearEditingExpense()
                        navController.popBackStack()
                    }
                )
            }

            // ── Analytics ─────────────────────────────────────────────────────
            composable(route = Screen.Analytics.route) {
                ComparisonScreen(vm = expenseVm)
            }

            // ── Profile ───────────────────────────────────────────────────────
            composable(route = Screen.Profile.route) {
                ProfileScreen(
                    profileVm = profileVm,
                    onLogout  = onLogout
                )
            }
        }
    }
}
