package com.xpenseledger.app.ui.screens.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.xpenseledger.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Persists Add/Edit Expense form state across:
 *   • Session locks (app goes to background → lock screen overlay → return)
 *   • Configuration changes (rotation)
 *   • Process death (system kills app under memory pressure)
 *
 * All fields are stored in [SavedStateHandle] so they survive process death
 * and are automatically restored when the ViewModel is recreated.
 *
 * The composable reads/writes these flows; the ViewModel never validates or
 * submits — that stays in the composable via [AddExpenseFormState].
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val savedState: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_TITLE     = "form_title"
        private const val KEY_AMOUNT    = "form_amount"
        private const val KEY_TIMESTAMP = "form_timestamp"
        private const val KEY_TYPE      = "form_type"
        private const val KEY_CAT_ID    = "form_cat_id"
        private const val KEY_SUBCAT_ID = "form_subcat_id"
    }

    // ── Exposed state (backed by SavedStateHandle) ────────────────────────────

    val title: StateFlow<String> =
        savedState.getStateFlow(KEY_TITLE, "")

    val amount: StateFlow<String> =
        savedState.getStateFlow(KEY_AMOUNT, "")

    val timestamp: StateFlow<Long> =
        savedState.getStateFlow(KEY_TIMESTAMP, System.currentTimeMillis())

    val transactionType: StateFlow<TransactionType> =
        savedState.getStateFlow(KEY_TYPE, TransactionType.EXPENSE)

    /** Long? stored as Long (-1L = null/unset) */
    val categoryId: StateFlow<Long> =
        savedState.getStateFlow(KEY_CAT_ID, -1L)

    val subCategoryId: StateFlow<Long> =
        savedState.getStateFlow(KEY_SUBCAT_ID, -1L)

    // ── Mutators ──────────────────────────────────────────────────────────────

    fun setTitle(v: String)              { savedState[KEY_TITLE]     = v }
    fun setAmount(v: String)             { savedState[KEY_AMOUNT]    = v }
    fun setTimestamp(v: Long)            { savedState[KEY_TIMESTAMP] = v }
    fun setTransactionType(v: TransactionType) { savedState[KEY_TYPE] = v }
    fun setCategoryId(id: Long?)         { savedState[KEY_CAT_ID]    = id ?: -1L }
    fun setSubCategoryId(id: Long?)      { savedState[KEY_SUBCAT_ID] = id ?: -1L }

    /**
     * Resets all fields to defaults.
     * Call this after a successful save so the form is clean for the next entry.
     */
    fun clearForm() {
        savedState[KEY_TITLE]     = ""
        savedState[KEY_AMOUNT]    = ""
        savedState[KEY_TIMESTAMP] = System.currentTimeMillis()
        savedState[KEY_TYPE]      = TransactionType.EXPENSE
        savedState[KEY_CAT_ID]    = -1L
        savedState[KEY_SUBCAT_ID] = -1L
    }

    /** Returns true if any field has been touched by the user. */
    fun isDirty(): Boolean =
        savedState.get<String>(KEY_TITLE).orEmpty().isNotBlank() ||
        savedState.get<String>(KEY_AMOUNT).orEmpty().isNotBlank() ||
        (savedState.get<Long>(KEY_CAT_ID) ?: -1L) != -1L
}

