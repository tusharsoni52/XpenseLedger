package com.xpenseledger.app.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.entity.ExpenseEntity
import com.xpenseledger.app.notification.data.PendingTransactionRepositoryImpl
import com.xpenseledger.app.notification.model.PendingTransaction
import com.xpenseledger.app.notification.prefs.NotificationListenerPrefs
import com.xpenseledger.app.notification.service.MonitoredApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationListenerViewModel @Inject constructor(
    private val pendingRepo: PendingTransactionRepositoryImpl,
    private val expenseDao: ExpenseDao,
    val prefs: NotificationListenerPrefs
) : ViewModel() {

    val pendingTransactions: StateFlow<List<PendingTransaction>> =
        pendingRepo.getPending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> =
        pendingRepo.getPendingCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Settings state (observed by Settings screen) ──────────────────────────
    private val _isEnabled             = MutableStateFlow(prefs.isEnabled)
    private val _showConfirmation      = MutableStateFlow(prefs.showConfirmationNotification)
    private val _autoCategory          = MutableStateFlow(prefs.autoAssignCategories)

    val isEnabled:         StateFlow<Boolean> = _isEnabled
    val showConfirmation:  StateFlow<Boolean> = _showConfirmation
    val autoCategory:      StateFlow<Boolean> = _autoCategory

    fun setEnabled(v: Boolean)            { prefs.isEnabled = v;                        _isEnabled.value = v }
    fun setShowConfirmation(v: Boolean)   { prefs.showConfirmationNotification = v;     _showConfirmation.value = v }
    fun setAutoCategory(v: Boolean)       { prefs.autoAssignCategories = v;             _autoCategory.value = v }
    fun setAppEnabled(pkg: String, v: Boolean) = prefs.setAppEnabled(pkg, v)
    fun isAppEnabled(pkg: String): Boolean = prefs.isAppEnabled(pkg)

    // ── Actions ───────────────────────────────────────────────────────────────
    fun addTransaction(tx: PendingTransaction) {
        viewModelScope.launch {
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
            pendingRepo.markAdded(tx.id)
        }
    }

    fun dismissTransaction(tx: PendingTransaction) {
        viewModelScope.launch {
            prefs.addDismissedHash(tx.dedupeHash)
            pendingRepo.markDismissed(tx.id)
        }
    }

    val monitoredApps = MonitoredApps.ALL
}

