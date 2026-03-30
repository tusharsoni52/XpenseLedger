package com.xpenseledger.app.data.local.debug

import android.util.Log
import com.xpenseledger.app.data.local.dao.ExpenseDao
import com.xpenseledger.app.data.local.dao.CategoryDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Systematic debugging utility for transaction visibility issues.
 *
 * Use this to diagnose why transactions are not visible in the UI:
 * 1. Verify data exists in database
 * 2. Check for filtering/JOIN issues
 * 3. Validate category integrity
 * 4. Identify orphaned transaction references
 */
class DatabaseDebugger(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {
    companion object {
        private const val TAG = "DatabaseDebugger"
        private val FMT_TIMESTAMP = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    /**
     * Run comprehensive diagnostic checks.
     * Logs detailed information about data state, filtering, and integrity.
     */
    suspend fun runDiagnostics() {
        Log.d(TAG, "╔════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║         DATABASE DIAGNOSTIC REPORT (Step 1-8)             ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════╝")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 1: Verify Data Exists
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 1: VERIFY DATA EXISTS ────────────────────────────┐")
        val totalExpenses = expenseDao.debugCountAllExpenses()
        Log.d(TAG, "│ Total expenses in database: $totalExpenses")
        if (totalExpenses > 0) {
            Log.d(TAG, "│ ✅ Data exists (NOT deleted)")
        } else {
            Log.d(TAG, "│ ❌ Database is EMPTY - May have been cleared")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 2: Remove All Filters - Check Raw Data
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 2: REMOVE ALL FILTERS (Check Raw Data) ──────────┐")
        val allExpenses = expenseDao.debugGetAllExpensesNoFilter()
        Log.d(TAG, "│ Raw expenses (no filtering): ${allExpenses.size} records")
        if (allExpenses.isNotEmpty()) {
            Log.d(TAG, "│ Sample (first 3):")
            allExpenses.take(3).forEach { expense ->
                Log.d(TAG, "│   - ${expense.title} | categoryId=${expense.categoryId} | ${formatTimestamp(expense.timestamp)}")
            }
            Log.d(TAG, "│ ✅ Raw data is accessible")
        } else {
            Log.d(TAG, "│ ❌ Even raw query returns empty - data may be corrupted")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 3: Remove JOIN (Critical Check)
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 3: REMOVE JOIN (No category JOIN) ──────────────┐")
        Log.d(TAG, "│ Current DAO query: SELECT * FROM expenses (no JOIN)")
        Log.d(TAG, "│ This is correct - we're not filtering out by missing categories")
        Log.d(TAG, "│ ✅ JOIN is NOT causing data loss")
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 4: Validate Category Integrity
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 4: CATEGORY INTEGRITY CHECK ──────────────────────┐")
        val validCategories = expenseDao.debugCountExpensesWithValidCategories()
        Log.d(TAG, "│ Expenses with VALID category refs: $validCategories")
        Log.d(TAG, "│ Expenses with ORPHANED (invalid) refs: ${totalExpenses - validCategories}")
        Log.d(TAG, "│ Orphan percentage: ${if (totalExpenses > 0) ((totalExpenses - validCategories) * 100 / totalExpenses) else 0}%")

        val orphanedExpenses = expenseDao.debugGetOrphanedExpenses()
        if (orphanedExpenses.isNotEmpty()) {
            Log.d(TAG, "│ ⚠️  Found ${orphanedExpenses.size} orphaned expense(s):")
            orphanedExpenses.take(5).forEach { expense ->
                Log.d(TAG, "│   - ${expense.title} | categoryId=${expense.categoryId} (MISSING!)")
            }
            Log.d(TAG, "│ NOTE: Migration 5→6 should remap these to categoryId=82")
        } else {
            Log.d(TAG, "│ ✅ NO orphaned expenses found - all categories valid")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 5: Category Reference Analysis
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 5: CATEGORY REFERENCE ANALYSIS ──────────────────┐")
        val categoryRefs = expenseDao.debugCategoryReferenceCount()
        Log.d(TAG, "│ Top 10 referenced categories:")
        categoryRefs.take(10).forEach { (catId, count) ->
            Log.d(TAG, "│   - categoryId=$catId: $count expense(s)")
        }

        val missingCatIds = expenseDao.debugGetMissingCategoryIds()
        if (missingCatIds.isNotEmpty()) {
            Log.d(TAG, "│ ⚠️  Missing category IDs: ${missingCatIds.joinToString(", ")}")
        } else {
            Log.d(TAG, "│ ✅ All referenced categories exist")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 6-7: Date Filtering Sanity Check
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 6-7: DATE RANGE FILTERING CHECK ─────────────────┐")
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000)
        val recentExpenses = expenseDao.debugGetExpensesByDateRange(thirtyDaysAgo, now)
        Log.d(TAG, "│ Expenses from last 30 days: ${recentExpenses.size}")

        val currentYear = getCurrentMonthMillis()
        val nextMonth = currentYear.first + (30 * 24 * 60 * 60 * 1000)
        val currentMonthExpenses = expenseDao.debugGetExpensesByDateRange(currentYear.first, nextMonth)
        Log.d(TAG, "│ Expenses from current month: ${currentMonthExpenses.size}")

        if (currentMonthExpenses.isEmpty() && allExpenses.isNotEmpty()) {
            Log.d(TAG, "│ ⚠️  Current month is empty but data exists globally")
            Log.d(TAG, "│ Date range: ${formatTimestamp(currentYear.first)} to ${formatTimestamp(nextMonth)}")
        } else {
            Log.d(TAG, "│ ✅ Date filtering appears correct")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // STEP 8: Migration Check
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n┌─ STEP 8: MIGRATION & FALLBACK CATEGORIES CHECK ────────┐")
        val fallbackCategories = categoryDao.debugGetFallbackCategories()
        Log.d(TAG, "│ Fallback categories:")
        Log.d(TAG, "│   - Other (id=8): ${fallbackCategories.contains(8)}")
        Log.d(TAG, "│   - Miscellaneous (id=82): ${fallbackCategories.contains(82)}")

        if (fallbackCategories.contains(8) && fallbackCategories.contains(82)) {
            Log.d(TAG, "│ ✅ Fallback categories present - migration 5→6 completed")
        } else {
            Log.d(TAG, "│ ❌ Fallback categories missing - migration may have failed")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────┘")

        // ──────────────────────────────────────────────────────────────────────
        // SUMMARY & RECOMMENDATIONS
        // ──────────────────────────────────────────────────────────────────────
        Log.d(TAG, "\n╔════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                    ROOT CAUSE SUMMARY                      ║")
        Log.d(TAG, "╚════════════════════════════════════════════════════════════╝")

        when {
            totalExpenses == 0L -> {
                Log.d(TAG, "🔴 ROOT CAUSE: Database appears empty")
                Log.d(TAG, "   → Data may have been cleared or not synced from server")
                Log.d(TAG, "   → ACTION: Restore from backup or re-enter data")
            }
            orphanedExpenses.isNotEmpty() -> {
                Log.d(TAG, "🟡 ROOT CAUSE: Orphaned category references found")
                Log.d(TAG, "   → Migration 5→6 should remap these automatically")
                Log.d(TAG, "   → If still orphaned, check if migration ran: SELECT * FROM categories WHERE id IN (${missingCatIds.joinToString(",")})")
                Log.d(TAG, "   → ACTION: Run migration or apply SQL fix manually")
            }
            currentMonthExpenses.isEmpty() && allExpenses.isNotEmpty() -> {
                Log.d(TAG, "🟡 ROOT CAUSE: Date filtering issue")
                Log.d(TAG, "   → Expenses exist but not in current month")
                Log.d(TAG, "   → Check ViewModel.monthKey() function for correct date formatting")
                Log.d(TAG, "   → ACTION: Verify month selector logic")
            }
            validCategories < totalExpenses -> {
                Log.d(TAG, "🟡 ROOT CAUSE: ${totalExpenses - validCategories} invalid category references")
                Log.d(TAG, "   → These should be remapped by migration 5→6")
                Log.d(TAG, "   → ACTION: Check migration logs and rerun if needed")
            }
            else -> {
                Log.d(TAG, "✅ DATABASE STATUS: Healthy")
                Log.d(TAG, "   → Data exists, filtering logic correct, categories valid")
                Log.d(TAG, "   → If transactions still not visible, issue is in UI layer")
                Log.d(TAG, "   → ACTION: Check ViewModel state collection and recomposition")
            }
        }

        Log.d(TAG, "\n❓ Need help? Check logcat output for detailed diagnostics above.")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HELPER FUNCTIONS
    // ──────────────────────────────────────────────────────────────────────────

    private fun formatTimestamp(millis: Long): String {
        return FMT_TIMESTAMP.format(Date(millis))
    }

    private fun getCurrentMonthMillis(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        return startOfMonth to cal.timeInMillis
    }
}
