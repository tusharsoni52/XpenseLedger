package com.xpenseledger.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.xpenseledger.app.ui.activity.MainActivity
import org.junit.Rule
import org.junit.Test

class LoginScreenComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginScreen_showsLockIcon_and_handlesBiometricClick() {
        // Set content is not needed because MainActivity hosts the NavGraph; ensure the login route is reachable in integration tests
        // Look for the lock icon by content description that the LoginScreen should provide.
        val node = composeRule.onNodeWithContentDescription("login_lock_icon")
        node.assertExists()
        node.performClick()
        // No further assertions — this is a skeleton to show how to interact with the UI.
    }
}

