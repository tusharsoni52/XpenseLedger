package com.xpenseledger.app.notification.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.xpenseledger.app.notification.data.PendingTransactionRepositoryImpl
import com.xpenseledger.app.notification.model.PendingTransaction
import com.xpenseledger.app.notification.prefs.NotificationListenerPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * System entry point for the Notification Listener feature.
 *
 * Lifecycle:
 *  - Bound by Android system when the user grants "Notification access" permission.
 *  - Runs as a bound service (not a started service) — negligible battery impact.
 *  - [onNotificationPosted] is called for every new notification on the device.
 *
 * Pipeline:
 *  NotificationFilter → TransactionParser → CategoryMatcher →
 *  DuplicateDetector → PendingTransactionRepository →
 *  ConfirmationNotificationManager
 */
@AndroidEntryPoint
class XpenseNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var prefs: NotificationListenerPrefs
    @Inject lateinit var pendingRepo: PendingTransactionRepositoryImpl
    @Inject lateinit var confirmationManager: ConfirmationNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // Guard: feature disabled by user
        if (!prefs.isEnabled) return

        val pkg   = sbn.packageName ?: return
        // Guard: this package is disabled by user
        if (!prefs.isAppEnabled(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title  = extras.getCharSequence("android.title")?.toString()
        val text   = extras.getCharSequence("android.text")?.toString()

        // Stage 1: whitelist + OTP/failure filter
        if (!NotificationFilter.shouldParse(pkg, title, text)) return

        // Stage 2–4: parse amount, type, merchant
        val parsed = TransactionParser.parse(pkg, title, text) ?: return

        val now     = System.currentTimeMillis()
        val hash    = DuplicateDetector.computeHash(parsed.amount, now, pkg)

        // Check dismissed hashes
        if (prefs.isDismissed(hash)) return

        scope.launch {
            // Check DB for recent duplicate
            if (pendingRepo.isDuplicate(hash)) return@launch

            // Stage 5: category assignment
            val catMatch = if (prefs.autoAssignCategories)
                CategoryMatcher.match(parsed.merchant)
            else
                CategoryMatcher.CategoryMatch("Other", "Miscellaneous", 8L, 82L)

            val appInfo = MonitoredApps.find(pkg)

            val tx = PendingTransaction(
                sourcePackage   = pkg,
                sourceLabel     = appInfo?.label ?: pkg,
                amount          = parsed.amount,
                merchant        = parsed.merchant,
                category        = catMatch.category,
                subCategory     = catMatch.subCategory,
                categoryId      = catMatch.categoryId,
                subCategoryId   = catMatch.subCategoryId,
                transactionType = parsed.transactionType,
                rawText         = "${title.orEmpty()} ${text.orEmpty()}".trim(),
                detectedAt      = now,
                dedupeHash      = hash
            )

            val insertedId = pendingRepo.insert(tx)
            if (insertedId < 0) return@launch   // IGNORE conflict — already exists

            val saved = tx.copy(id = insertedId)

            if (prefs.showConfirmationNotification) {
                confirmationManager.showConfirmation(saved)
            }

            // Housekeeping: remove old non-pending rows weekly
            pendingRepo.cleanupOld()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not used — we only listen for posts
    }
}

