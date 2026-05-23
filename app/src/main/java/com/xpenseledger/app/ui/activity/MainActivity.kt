package com.xpenseledger.app.ui.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.xpenseledger.app.security.biometrics.BiometricAuthManager
import com.xpenseledger.app.ui.navigation.AppNavGraph
import com.xpenseledger.app.ui.screens.auth.LoginScreen
import com.xpenseledger.app.ui.theme.XpenseLedgerTheme
import com.xpenseledger.app.ui.viewmodel.CategoryViewModel
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import com.xpenseledger.app.ui.viewmodel.SessionViewModel
import com.xpenseledger.app.ui.viewmodel.UserProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applySecureFlag()
        super.onCreate(savedInstanceState)
        setContent {
            XpenseLedgerTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySecureFlag()
    }

    override fun onStop() {
        super.onStop()

        // ── Force-hide IME at Window level ────────────────────────────────────
        // LocalSoftwareKeyboardController is unreliable when the keyboard is
        // owned by a composable in a different subtree (e.g. AddExpenseScreen
        // while the lock overlay is in a sibling Box). Using WindowInsetsController
        // directly on the Window always works regardless of focus owner.
        currentFocus?.clearFocus()   // remove focus so IME doesn't reappear on resume
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())

        // ── Trigger session lock ──────────────────────────────────────────────
        val sessionVm = androidx.lifecycle.ViewModelProvider(this)
            .get(SessionViewModel::class.java)
        sessionVm.lockWithGracePeriod()
    }

    private fun applySecureFlag() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

@Composable
private fun AppRoot() {
    val sessionVm:  SessionViewModel     = hiltViewModel()
    val expenseVm:  ExpenseViewModel     = hiltViewModel()
    val categoryVm: CategoryViewModel    = hiltViewModel()
    val profileVm:  UserProfileViewModel = hiltViewModel()
    val context = LocalContext.current

    val biometricManager  = BiometricManager.from(context)
    val canUseBiometrics  = biometricManager.canAuthenticate(
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    val activity             = context as FragmentActivity
    val biometricAuthManager = if (canUseBiometrics) BiometricAuthManager(activity) else null

    val isLocked by sessionVm.isLocked.collectAsState()
    sessionVm.startInactivityTimer()

    // ── AppNavGraph is ALWAYS in the tree ──────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        AppNavGraph(
            expenseVm      = expenseVm,
            categoryVm     = categoryVm,
            profileVm      = profileVm,
            onUserActivity = { sessionVm.onUserInteraction() },
            onLogout       = { sessionVm.lock() }
        )

        // Lock-screen overlay
        AnimatedVisibility(
            visible = isLocked,
            enter   = fadeIn(tween(180)),
            exit    = fadeOut(tween(220))
        ) {
            LoginScreen(
                biometricAuthManager = biometricAuthManager,
                canUseBiometrics     = canUseBiometrics,
                onAuthenticated      = { sessionVm.unlock() }
            )
        }
    }
}