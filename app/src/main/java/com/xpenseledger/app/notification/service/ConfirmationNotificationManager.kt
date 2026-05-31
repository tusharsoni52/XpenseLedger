package com.xpenseledger.app.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xpenseledger.app.R
import com.xpenseledger.app.notification.model.PendingTransaction
import com.xpenseledger.app.notification.receiver.NotificationActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmationNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID   = "xpense_auto_detect"
        const val CHANNEL_NAME = "Auto-Detected Expenses"

        const val ACTION_ADD     = "com.xpenseledger.ACTION_ADD_PENDING"
        const val ACTION_EDIT    = "com.xpenseledger.ACTION_EDIT_PENDING"
        const val ACTION_DISMISS = "com.xpenseledger.ACTION_DISMISS_PENDING"

        const val EXTRA_PENDING_ID  = "pending_id"
        const val EXTRA_NOTIF_ID    = "notif_id"
    }

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init { createChannel() }

    fun showConfirmation(tx: PendingTransaction) {
        val notifId = tx.id.toInt().coerceAtLeast(1)
        val amountStr = "₹%.2f".format(tx.amount)
        val title   = "💳 $amountStr · ${tx.merchant.ifBlank { tx.sourceLabel }}"
        val subtext = "${tx.category} · ${if (tx.transactionType == "INCOME") "Income" else "Expense"}"

        fun pendingIntent(action: String) = PendingIntent.getBroadcast(
            context,
            notifId * 10 + action.hashCode() % 10,
            Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_PENDING_ID, tx.id)
                putExtra(EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtext)
            .setSubText("Tap to review · ${tx.sourceLabel}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "✓ Add",      pendingIntent(ACTION_ADD))
            .addAction(0, "✏ Edit",     pendingIntent(ACTION_EDIT))
            .addAction(0, "✗ Dismiss",  pendingIntent(ACTION_DISMISS))
            .build()

        nm.notify(notifId, notification)
    }

    fun cancel(notifId: Int) = nm.cancel(notifId)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for auto-detected payment transactions"
            }
            nm.createNotificationChannel(ch)
        }
    }
}

