package com.xpenseledger.app.notification.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences wrapper for all Notification Listener settings.
 * All keys are prefixed with "nls_" to avoid collision.
 */
@Singleton
class NotificationListenerPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xpense_nls_prefs", Context.MODE_PRIVATE)

    // ── Master switch ───────────────────────────────────────────────────────────
    var isEnabled: Boolean
        get()      = prefs.getBoolean("nls_enabled", true)
        set(value) = prefs.edit().putBoolean("nls_enabled", value).apply()

    var showConfirmationNotification: Boolean
        get()      = prefs.getBoolean("nls_show_confirmation", true)
        set(value) = prefs.edit().putBoolean("nls_show_confirmation", value).apply()

    var autoAssignCategories: Boolean
        get()      = prefs.getBoolean("nls_auto_category", true)
        set(value) = prefs.edit().putBoolean("nls_auto_category", value).apply()

    // ── Onboarding ──────────────────────────────────────────────────────────────
    var onboardingDismissed: Boolean
        get()      = prefs.getBoolean("nls_onboarding_dismissed", false)
        set(value) = prefs.edit().putBoolean("nls_onboarding_dismissed", value).apply()

    // ── Per-app whitelist ───────────────────────────────────────────────────────
    fun isAppEnabled(packageName: String): Boolean =
        prefs.getBoolean("nls_app_$packageName", true)

    fun setAppEnabled(packageName: String, enabled: Boolean) =
        prefs.edit().putBoolean("nls_app_$packageName", enabled).apply()

    // ── Dismissed hashes (never ask again) ─────────────────────────────────────
    fun addDismissedHash(hash: String) {
        val set = getDismissedHashes().toMutableSet()
        set.add(hash)
        // Keep only last 500 hashes to avoid unbounded growth
        val trimmed = if (set.size > 500) set.drop(set.size - 500).toMutableSet() else set
        prefs.edit().putStringSet("nls_dismissed_hashes", trimmed).apply()
    }

    fun isDismissed(hash: String): Boolean = getDismissedHashes().contains(hash)

    private fun getDismissedHashes(): Set<String> =
        prefs.getStringSet("nls_dismissed_hashes", emptySet()) ?: emptySet()
}

