# 🚀 Quick Start: Debug Missing Transactions

## TL;DR (30 seconds)

If transactions are missing after category refactoring:

### Step 1: Add Debug Code (Copy-Paste)
```kotlin
// In HomeScreen or any Composable

val debugVm: DebugViewModel = hiltViewModel()

LaunchedEffect(Unit) {
    debugVm.runTransactionVisibilityDiagnostics()
}
```

### Step 2: Check Logcat
```bash
adb logcat | grep "DatabaseDebugger"
```

### Step 3: Look for Root Cause
```
✅ DATABASE STATUS: Healthy → Issue is in UI layer
🔴 Database appears empty → No data found
🟡 Orphaned category references → Apply SQL fix
🟡 Date filtering issue → Check month selector
```

### Step 4: Apply Fix
See the root cause message in logcat output for specific fix

---

## One-Command Test

```kotlin
// Run in ViewModel or Activity:
viewModelScope.launch {
    val debugger = DatabaseDebugger(expenseDao, categoryDao)
    debugger.runDiagnostics()
}
```

Then check logcat for complete analysis.

---

## Most Common Fixes

### 1️⃣ Orphaned Transactions Found
```sql
-- Run in SQLite shell:
UPDATE expenses SET categoryId = 82
WHERE categoryId NOT IN (SELECT id FROM categories)
AND categoryId > 0;
```

### 2️⃣ Fallback Categories Missing
```sql
INSERT OR IGNORE INTO categories (id, name, type, parentId, icon)
VALUES (8, 'Other', 'MAIN', NULL, '📦'),
       (82, 'Miscellaneous', 'SUB', 8, '');
```

### 3️⃣ Month Not Showing Expenses
- Check `ExpenseViewModel.selectedMonth` value
- Verify month selector date is correct
- Run: `debugGetExpensesByDateRange()` for that month

### 4️⃣ Data Actually Lost
- Clear app data and restore from backup
- Or re-enter transactions

---

## Debug Methods at a Glance

| Problem | Debug Method | What It Shows |
|---------|--------------|---------------|
| "No data" | `debugCountAllExpenses()` | Total count |
| Filtering broken | `debugGetAllExpensesNoFilter()` | Raw expenses |
| Missing categories | `debugGetOrphanedExpenses()` | Invalid refs |
| Corrupted | `debugGetMissingCategoryIds()` | Bad IDs |
| Category usage | `debugCategoryReferenceCount()` | Counts per cat |
| Month filtering | `debugGetExpensesByDateRange()` | By date |
| Migration failed | `debugGetFallbackCategories()` | [8, 82] |

---

## Files Added/Modified

| File | Type | Purpose |
|------|------|---------|
| ExpenseDao.kt | Modified | 7 debug methods |
| CategoryDao.kt | Modified | 1 debug method |
| DatabaseDebugger.kt | Created | Automated diagnostics |
| DebugViewModel.kt | Created | Integration helper |
| CategoryReferenceCount | Created | Data class |

---

## Expected Output (Healthy DB)

```
╔════════════════════════════════════════════════════════════╗
║         DATABASE DIAGNOSTIC REPORT (Step 1-8)             ║
╚════════════════════════════════════════════════════════════╝

STEP 1: VERIFY DATA EXISTS
│ Total expenses in database: 42
│ ✅ Data exists (NOT deleted)

STEP 4: CATEGORY INTEGRITY CHECK
│ Expenses with VALID category refs: 42
│ Expenses with ORPHANED (invalid) refs: 0
│ ✅ NO orphaned expenses found

ROOT CAUSE SUMMARY
✅ DATABASE STATUS: Healthy
```

---

## Logcat Command

```bash
# Watch diagnostic output in real-time:
adb logcat DatabaseDebugger:D *:S

# Or one-time grep:
adb logcat | grep "DatabaseDebugger"
```

---

## SQL Check (Without App)

```bash
# Open database directly:
adb shell sqlite3 /data/data/com.xpenseledger.app/databases/xpenseledger.db

# Run this:
sqlite> SELECT COUNT(*) FROM expenses;
sqlite> SELECT COUNT(*) FROM expenses WHERE categoryId NOT IN (SELECT id FROM categories);
sqlite> SELECT id FROM categories WHERE id IN (8,82);
```

---

## Troubleshooting Matrix

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| "No expenses yet" screen | Month filter wrong | Check selectedMonth |
| Data appears in DB but not UI | DAO/ViewModel issue | Check StateFlow |
| Some old months empty | Orphaned categories | Run SQL UPDATE |
| Entire database empty | Data deleted | Restore backup |
| Random categories broken | Migration incomplete | Insert fallback |

---

## Key Files to Review

📄 [ROOT_CAUSE_ANALYSIS_REPORT.md](ROOT_CAUSE_ANALYSIS_REPORT.md) - Deep dive
📄 [TRANSACTION_VISIBILITY_DEBUG_GUIDE.md](TRANSACTION_VISIBILITY_DEBUG_GUIDE.md) - Complete guide
📄 [SQL_RECOVERY_COMMANDS.md](SQL_RECOVERY_COMMANDS.md) - SQL fixes

---

## Complete Checklist

- [ ] Add debug code to HomeScreen
- [ ] Build and install APK
- [ ] Check logcat output
- [ ] Note the ✅/🟡/❌ status
- [ ] Follow recommendations
- [ ] Apply fix if needed
- [ ] Verify transactions reappear

---

## Still Broken?

1. Copy full logcat output
2. Check ROOT_CAUSE_ANALYSIS_REPORT.md for that status
3. Follow the ACTION recommended in the output
4. For SQL fixes, see SQL_RECOVERY_COMMANDS.md
5. If still stuck, file GitHub issue with logcat

---

## Remember

- ✅ Migration 5→6 auto-remaps orphaned transactions
- ✅ Fallback categories (8, 82) ensure no data loss
- ✅ This debug code is production-safe
- ✅ All methods are read-only
- ✅ Output clearly identifies root cause

---

**Last Resort**: Clear app data and restore from backup
`adb shell pm clear com.xpenseledger.app`

But first, run the diagnostics! They'll tell you exactly what's wrong.

🎉 Happy debugging!
