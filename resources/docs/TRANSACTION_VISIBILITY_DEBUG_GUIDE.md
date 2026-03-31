# 🔍 Transaction Visibility Debugging Guide

## Problem Statement
After category refactoring, **existing transactions are not visible in the UI**, but they are expected to still exist in the database.

## Systematic Root Cause Analysis (Steps 1-8)

### ✅ Step 1: Verify Data Exists
**Test**: Check if database contains any expense records.

```kotlin
val totalCount = expenseDao.debugCountAllExpenses()
```

**Expected Result**: `totalCount > 0`
- ✅ If TRUE → Data exists (not deleted)
- ❌ If FALSE → Database is empty (may have been cleared)

---

### ✅ Step 2: Remove All Filters (Check Raw Data)
**Test**: Retrieve all expenses with no filtering logic.

```kotlin
val allExpenses = expenseDao.debugGetAllExpensesNoFilter()
```

**Expected Result**: Should return same or more records than UI shows
- ✅ If populated → Raw data is accessible; issue is in filtering logic
- ❌ If empty → Data is truly missing or corrupted

---

### ✅ Step 3: Remove JOIN (Critical Check)
**Current Implementation**: 
- DAO Query: `SELECT * FROM expenses ORDER BY timestamp DESC`
- ✅ **CORRECT** - No JOIN with categories table
- ✅ This means missing categories won't filter out expenses

**Why This Matters**: If we used `INNER JOIN categories` on categoryId, any expense with invalid categoryId would be excluded from results.

**Status**: ✅ **NOT the cause** - our DAO is safe

---

### ✅ Step 4: Validate Category Integrity
**Test**: Check for orphaned expense references.

```kotlin
val validCount = expenseDao.debugCountExpensesWithValidCategories()
val orphanedCount = totalExpenses - validCount
val orphaned = expenseDao.debugGetOrphanedExpenses()
```

**Expected Result After Migration 5→6**:
- All expenses should have valid categoryId references
- Any orphaned expenses should be remapped to categoryId = 82 (Miscellaneous)

**If Orphaned Expenses Found**:
```
⚠️ Migration 5→6 may not have completed
→ Check: SELECT COUNT(*) FROM expenses WHERE categoryId NOT IN (SELECT id FROM categories)
→ This should return 0 if migration worked
```

---

### ✅ Step 5: Category Reference Analysis
**Test**: Count how many expenses use each category.

```kotlin
val refCounts = expenseDao.debugCategoryReferenceCount()
// Shows: categoryId=1: 5 expenses, categoryId=2: 3 expenses, etc.

val missingIds = expenseDao.debugGetMissingCategoryIds()
// Shows which categoryIds have no matching category record
```

**Interpretation**:
- If `missingIds` is NOT empty → **Migration 5→6 failed to remap these**
- Should be completely empty after successful migration

---

### ✅ Step 6: Date Filtering Sanity Check
**Test**: Verify month selector logic.

```kotlin
val currentMonthExpenses = expenseDao.debugGetExpensesByDateRange(startMillis, endMillis)
```

**Common Issues**:
1. **Wrong month boundaries** → Expenses fall outside month range
2. **Timezone issues** → Timestamps stored in different timezone
3. **Date formatter mismatch** → `monthKey()` function formatting incorrectly

**If Only Current Month Shows Empty**:
```kotlin
// Check ViewModel.monthKey() function:
private val FMT_MONTH = SimpleDateFormat("yyyy-MM", Locale.US)
fun monthKey(expense: Expense): String = FMT_MONTH.format(Date(expense.timestamp))

// Ensure it always returns "yyyy-MM" format
```

---

### ✅ Step 7: Date Formatter (UI Layer)
**Test**: Verify date display format.

```kotlin
// Current (CORRECT):
private val FMT_DISPLAY = SimpleDateFormat("MMM yy", Locale.getDefault())

// This should display months correctly: "Mar 26", "Feb 26", etc.
```

**Common Mistake** (DON'T DO THIS):
```kotlin
SimpleDateFormat("MMM dd")  // ❌ Shows day, not month!
```

---

### ✅ Step 8: Migration & Fallback Categories
**Test**: Verify migration 5→6 completed successfully.

```kotlin
val fallbackIds = categoryDao.debugGetFallbackCategories()
// Should return: [8, 82]  (Other and Miscellaneous)
```

**If Fallbacks Missing**:
```sql
-- Run these SQL statements manually:
INSERT OR IGNORE INTO categories (id, name, type, parentId, icon)
VALUES (8, 'Other', 'MAIN', NULL, '📦');

INSERT OR IGNORE INTO categories (id, name, type, parentId, icon)
VALUES (82, 'Miscellaneous', 'SUB', 8, '');
```

---

## 🔧 How to Run Diagnostics

### Option 1: Automatic Diagnostics (Recommended)
```kotlin
// In ViewModel or Activity:
val debugger = DatabaseDebugger(expenseDao, categoryDao)
debugger.runDiagnostics()  // Outputs to logcat

// View logs:
// adb logcat | grep DatabaseDebugger
```

### Option 2: Manual Queries
```kotlin
viewModelScope.launch {
    // Step 1
    val count = expenseDao.debugCountAllExpenses()
    Log.d("DEBUG", "Total expenses: $count")

    // Step 4
    val valid = expenseDao.debugCountExpensesWithValidCategories()
    Log.d("DEBUG", "Valid categories: $valid")

    // Step 4 - Check for orphans
    val orphaned = expenseDao.debugGetOrphanedExpenses()
    Log.d("DEBUG", "Orphaned expenses: ${orphaned.size}")
    orphaned.forEach { e ->
        Log.d("DEBUG", "  - ${e.title} categoryId=${e.categoryId}")
    }
}
```

---

## 🎯 Root Cause Decision Tree

```
Is database empty?
├─ YES → Data was deleted/cleared
│        ACTION: Restore from backup
└─ NO → Continue to Step 4

Are there orphaned expense references?
├─ YES → Migration 5→6 incomplete
│        ACTION: Rerun migration or apply SQL fix
└─ NO → Continue to Step 5

Are all category references valid?
├─ YES → Continue to Step 6
└─ NO → Migration 5→6 failed
         ACTION: Check migration logs

Is current month empty but other months have data?
├─ YES → Date filtering issue
│        ACTION: Check month selector & monthKey() function
└─ NO → Continue to Step 7

Check UI-level issues:
├─ AnimatedContent not showing Success state?
│  ACTION: Check dashboardUiState.collectAsState()
├─ StateFlow not recomposing?
│  ACTION: Verify .stateIn() with correct SharingStarted
├─ List is empty but filtered correctly?
│  ACTION: Check EmptyExpenseState vs list rendering
└─ OTHER → Check Composable hierarchy & LazyColumn
```

---

## 🚨 Critical Migration Check

After refactoring, verify migration 5→6 ran successfully:

```sql
-- Check total expenses
SELECT COUNT(*) FROM expenses;

-- Check orphaned expenses (should be 0)
SELECT COUNT(*) FROM expenses 
WHERE categoryId NOT IN (SELECT id FROM categories) 
AND categoryId > 0;

-- Check fallback categories exist
SELECT * FROM categories WHERE id IN (8, 82);

-- If orphaned count > 0, apply this fix:
UPDATE expenses SET categoryId = 82 
WHERE categoryId NOT IN (SELECT id FROM categories) 
AND categoryId > 0;
```

---

## 📋 Debugging Checklist

- [ ] Run `DatabaseDebugger.runDiagnostics()` and check logcat output
- [ ] Verify `debugCountAllExpenses() > 0`
- [ ] Verify no orphaned expenses
- [ ] Verify fallback categories (8, 82) exist
- [ ] Check month selector is set correctly
- [ ] Verify `monthKey()` function returns correct format
- [ ] Check `dashboardUiState` is in Success state (not Loading/Empty)
- [ ] Check `LazyColumn` is rendering expense list
- [ ] Verify AnimationContent is animating correctly
- [ ] Check device date/time is correct (matches device system clock)

---

## 📞 If Issues Persist

1. **Clear app data & reinstall** (Last resort):
   ```bash
   adb shell pm clear com.xpenseledger.app
   adb install path/to/app.apk
   ```

2. **Check backup file** (if you have one):
   ```bash
   adb shell ls -la /sdcard/XpenseLedger/
   ```

3. **Review build logs**:
   ```bash
   ./gradlew assembleDebug --info 2>&1 | grep -i migration
   ./gradlew assembleDebug --info 2>&1 | grep -i category
   ```

4. **Verify Database Schema**:
   ```bash
   adb shell sqlite3 /data/data/com.xpenseledger.app/databases/xpenseledger.db
   sqlite> .tables
   sqlite> PRAGMA table_info(expenses);
   sqlite> PRAGMA table_info(categories);
   ```

---

## 💡 Key Takeaways

| Component | Issue | Solution |
|-----------|-------|----------|
| **DAO Query** | INNER JOIN | Use plain SELECT (no JOIN) ✅ |
| **Category Refs** | Orphaned IDs | Migration 5→6 remaps to 82 ✅ |
| **Month Filter** | Wrong boundaries | Use Calendar.DAY_OF_MONTH = 1 ✅ |
| **Fallbacks** | Missing Other/Misc | Insert with INSERT OR IGNORE ✅ |
| **StateFlow** | Not recomposing | Use WhileSubscribed() & Eagerly ✅ |

---

## 📊 Quick Validation

Run this in logcat to quickly validate everything:

```bash
adb logcat | grep "DatabaseDebugger\|ROOT CAUSE"
```

You should see output like:
```
✅ DATABASE STATUS: Healthy
   → Data exists, filtering logic correct, categories valid
   → If transactions still not visible, issue is in UI layer
```

If you see `🔴 ROOT CAUSE: Database appears empty`, that's your issue!
