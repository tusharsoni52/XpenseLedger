package com.xpenseledger.app.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// ─────────────────────────────────────────────────────────────────────────────
//  Debounced click handler
//  Prevents rapid repeated taps from firing duplicate actions (delete, confirm)
// ─────────────────────────────────────────────────────────────────────────────

private const val DEBOUNCE_MS = 600L

/**
 * Returns a lambda that wraps [action] with a [DEBOUNCE_MS]-millisecond debounce.
 * Multiple taps within the window are silently dropped — only the first fires.
 *
 * Usage:
 * ```
 * val safeClick = rememberDebouncedClick { vm.deleteExpense(expense) }
 * IconButton(onClick = safeClick) { ... }
 * ```
 */
@Composable
fun rememberDebouncedClick(
    debounceMs: Long = DEBOUNCE_MS,
    action: () -> Unit
): () -> Unit {
    var lastClickMs by remember { mutableLongStateOf(0L) }
    return remember(action) {
        {
            val now = System.currentTimeMillis()
            if (now - lastClickMs >= debounceMs) {
                lastClickMs = now
                action()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Amount masking
//  Shows "₹ ••••" instead of the real value when [masked] is true
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a display string for [amount]:
 *  • [masked] = true  →  "₹ ••••••"
 *  • [masked] = false →  "₹ 1,234.56"
 */
fun maskedAmount(amount: Double, masked: Boolean): String =
    if (masked) "₹ ••••••" else "₹${"%.2f".format(amount)}"

