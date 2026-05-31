package com.xpenseledger.app.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.dao.PendingTransactionDao
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import com.xpenseledger.app.notification.model.PendingTransaction
import com.xpenseledger.app.notification.prefs.NotificationListenerPrefs
import com.xpenseledger.app.notification.service.ConfirmationNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives button-tap intents from the confirmation notification.
 * Handles ✓ Add, ✏ Edit, ✗ Dismiss actions.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var pendingDao: PendingTransactionDao
    @Inject lateinit var expenseDao: ExpenseDao
    @Inject lateinit var prefs: NotificationListenerPrefs
    @Inject lateinit var notifManager: ConfirmationNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingId = intent.getLongExtra(ConfirmationNotificationManager.EXTRA_PENDING_ID, -1L)
        val notifId   = intent.getIntExtra(ConfirmationNotificationManager.EXTRA_NOTIF_ID, -1)
        if (pendingId < 0) return

        when (intent.action) {
            ConfirmationNotificationManager.ACTION_ADD -> {
                scope.launch {
                    val entities = pendingDao.getPendingSnapshot()
                    val tx = entities.firstOrNull { it.id == pendingId } ?: return@launch
                    // Insert into main expenses table
                    expenseDao.insert(
                        ExpenseEntity(
                            title         = tx.merchant.ifBlank { tx.sourceLabel },
                            amount        = tx.amount,
                            category      = tx.category,
                            subCategory   = tx.subCategory,
                            categoryId    = tx.categoryId,
                            subCategoryId = tx.subCategoryId,
                            timestamp     = tx.detectedAt,
                            type          = tx.transactionType
                        )
                    )
                    pendingDao.updateStatus(pendingId, PendingTransaction.STATUS_ADDED)
                    if (notifId >= 0) notifManager.cancel(notifId)
                }
            }

            ConfirmationNotificationManager.ACTION_EDIT -> {
                // Launch app to AddExpenseScreen — handled by deep-link intent
                val launchIntent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_pending_id", pendingId)
                    }
                if (launchIntent != null) context.startActivity(launchIntent)
                if (notifId >= 0) notifManager.cancel(notifId)
            }

            ConfirmationNotificationManager.ACTION_DISMISS -> {
                scope.launch {
                    // Retrieve hash before marking dismissed
                    val entities = pendingDao.getPendingSnapshot()
                    val tx = entities.firstOrNull { it.id == pendingId }
                    if (tx != null) prefs.addDismissedHash(tx.dedupeHash)
                    pendingDao.updateStatus(pendingId, PendingTransaction.STATUS_DISMISSED)
                    if (notifId >= 0) notifManager.cancel(notifId)
                }
            }
        }
    }
}

