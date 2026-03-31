package com.xpenseledger.app.ui.screens.add

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.model.TransactionType

// ─────────────────────────────────────────────────────────────────────────────
//  Constants
// ─────────────────────────────────────────────────────────────────────────────

const val TITLE_MAX_LEN  = 60
const val AMOUNT_MAX_LEN = 12          // ≈ "999,999.99" + currency symbol

// ─────────────────────────────────────────────────────────────────────────────
//  Sanitisation helpers  (UI-layer only — no business logic)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Strip control characters, leading/trailing whitespace, and cap length.
 * Does NOT enforce business rules — that stays in the ViewModel.
 */
fun sanitizeTitle(raw: String): String =
    raw.filter { it >= ' ' }           // drop control chars (< 0x20)
       .take(TITLE_MAX_LEN)

/**
 * Allows only one decimal separator and up to 2 decimal places.
 * Strips anything that is not a digit or '.' and prevents:
 *   • Multiple dots           (0.1.2 → "0.12")
 *   • More than 2 dp          ("1.234" → "1.23")
 *   • Leading dot             (".5" → ".5" — intentionally allowed for usability)
 *   • Empty after stripping   → ""
 * Length is capped at [AMOUNT_MAX_LEN].
 */
fun sanitizeAmount(raw: String): String {
    val stripped = raw.filter { it.isDigit() || it == '.' }.take(AMOUNT_MAX_LEN)
    val dotIndex = stripped.indexOf('.')
    return if (dotIndex == -1) stripped
    else {
        // Only the first dot is kept; up to 2 digits after it
        val intPart  = stripped.substring(0, dotIndex)
        val fracPart = stripped.substring(dotIndex + 1).filter { it.isDigit() }.take(2)
        "$intPart.$fracPart"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Validation result
// ─────────────────────────────────────────────────────────────────────────────

data class FieldError(val message: String)

// ─────────────────────────────────────────────────────────────────────────────
//  Form state  (observable via Compose snapshot state)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Holds every piece of mutable form state for the Add / Edit Expense screen.
 *
 * Validation is declarative and self-contained here so the composable
 * only reacts to [isValid] and individual [FieldError]s — no business
 * logic leaks into the UI layer.
 *
 * Rapid-submit prevention is done by [submitting] — callers set it to
 * `true` before the suspend call and back to `false` on completion.
 */
@Stable
class AddExpenseFormState(
    initialTitle:           String            = "",
    initialAmount:          String            = "",
    initialMain:            Category?         = null,
    initialSub:             Category?         = null,
    initialTimestamp:       Long              = System.currentTimeMillis(),
    initialTransactionType: TransactionType   = TransactionType.EXPENSE
) {
    // ── Raw field values ──────────────────────────────────────────────────────

    var title           by mutableStateOf(initialTitle)
    var amount          by mutableStateOf(initialAmount)
    var mainCat         by mutableStateOf<Category?>(initialMain)
    var subCat          by mutableStateOf<Category?>(initialSub)
    var timestamp       by mutableStateOf(initialTimestamp)
    var transactionType by mutableStateOf(initialTransactionType)

    /** True while the form is being submitted — disables the confirm button. */
    var submitting by mutableStateOf(false)

    // ── Dirty flags — errors only shown after first interaction ───────────────

    var titleTouched  by mutableStateOf(false)
    var amountTouched by mutableStateOf(false)
    var catTouched    by mutableStateOf(false)

    // ── Per-field validation ──────────────────────────────────────────────────

    val titleError: FieldError?
        get() = when {
            !titleTouched                    -> null
            title.isBlank()                  -> FieldError("Title is required")
            title.trim().length < 2          -> FieldError("Title is too short")
            else                             -> null
        }

    val amountError: FieldError?
        get() {
            if (!amountTouched) return null
            val d = amount.toDoubleOrNull()
            return when {
                amount.isBlank()  -> FieldError("Amount is required")
                d == null         -> FieldError("Enter a valid number")
                d <= 0            -> FieldError("Amount must be greater than zero")
                d > 9_999_999.99  -> FieldError("Amount is too large")
                else              -> null
            }
        }

    val categoryError: FieldError?
        get() = if (catTouched && mainCat == null) FieldError("Select a category") else null

    // ── Overall validity ──────────────────────────────────────────────────────

    val isValid: Boolean
        get() {
            val d = amount.toDoubleOrNull()
            return title.isNotBlank()
                && title.trim().length >= 2
                && d != null && d > 0 && d <= 9_999_999.99
                && mainCat != null
        }

    // ── Convenience: mark all fields touched to surface errors on submit ───────

    fun touchAll() {
        titleTouched  = true
        amountTouched = true
        catTouched    = true
    }
}
