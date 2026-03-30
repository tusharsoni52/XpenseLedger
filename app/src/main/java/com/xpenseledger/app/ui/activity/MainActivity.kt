package com.xpenseledger.app.ui.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
        // Prevent screenshots and screen recording — applied before super so
        // the system never has a chance to capture an unprotected frame.
        applySecureFlag()

        super.onCreate(savedInstanceState)

        setContent {
            XpenseLedgerTheme {
                AppRoot()
            }
        }
    }

    /** Re-apply on every resume so the flag survives task-switching / PiP. */
    override fun onResume() {
        super.onResume()
        applySecureFlag()
    }

    /**
     * Lock the app whenever it is moved to the background (home button, task switch,
     * power button, incoming call, etc.).
     * onStop fires after onPause and before the activity is no longer visible.
     */
    override fun onStop() {
        super.onStop()
        // Obtain SessionViewModel from the ViewModelStore — no Compose needed here
        val sessionVm = androidx.lifecycle.ViewModelProvider(this)
            .get(SessionViewModel::class.java)
        sessionVm.lock()
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
    val sessionVm:  SessionViewModel    = hiltViewModel()
    val expenseVm:  ExpenseViewModel    = hiltViewModel()
    val categoryVm: CategoryViewModel   = hiltViewModel()
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

    if (isLocked) {
        LoginScreen(
            biometricAuthManager = biometricAuthManager,
            canUseBiometrics     = canUseBiometrics,
            onAuthenticated      = { sessionVm.unlock() }
        )
    } else {
        AppNavGraph(
            expenseVm      = expenseVm,
            categoryVm     = categoryVm,
            profileVm      = profileVm,
            onUserActivity = { sessionVm.onUserInteraction() },
            onLogout       = { sessionVm.lock() }
        )
    }
}