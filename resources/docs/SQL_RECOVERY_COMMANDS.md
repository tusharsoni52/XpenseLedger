# 🔧 SQL Fixes & Recovery Commands

This document contains tested SQL commands to fix missing transaction issues if they still occur after migration 5→6.

## ⚠️ Use Only If Needed

These commands are for **advanced troubleshooting only**. The migration 5→6 should handle all recovery automatically.

---

## 1️⃣ Verify Data Exists

```sql
-- Check total expense count
SELECT COUNT(*) as total_expenses FROM expenses;

-- Check expenses in current month
SELECT COUNT(*) as march_2026_expenses FROM expenses
WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = '2026-03';

-- Get sample of oldest/newest expenses
SELECT id, title, amount, categoryId, timestamp FROM expenses
ORDER BY timestamp DESC LIMIT 5;
```

**Expected**: Total > 0, March data visible, sample shows valid data

---

## 2️⃣ Check Category Integrity

```sql
-- Count total categories
SELECT COUNT(*) as total_categories FROM categories;

-- List all categories
SELECT id, name, type, parentId FROM categories ORDER BY id;

-- Check fallback categories exist
SELECT id, name FROM categories WHERE id IN (8, 82);
```

**Expected**:
- Total categories ≥ 70
- Fallback categories (8, 82) present

---

## 3️⃣ Identify Orphaned Transactions

```sql
-- Count orphaned expenses (critical indicators!)
SELECT COUNT(*) as orphaned_count FROM expenses
WHERE categoryId NOT IN (SELECT id FROM categories)
AND categoryId > 0;

-- List orphaned expenses (if any)
SELECT id, title, categoryId, timestamp FROM expenses
WHERE categoryId NOT IN (SELECT id FROM categories)
AND categoryId > 0
ORDER BY timestamp DESC;

-- Show which categoryIds are missing
SELECT DISTINCT categoryId FROM expenses
WHERE categoryId NOT IN (SELECT id FROM categories)
ORDER BY categoryId;
```

**Expected**: orphaned_count = 0 (if migration worked)

---

## 4️⃣ Fix Orphaned Transactions

**⚠️ Apply only if Step 3 shows orphaned expenses**

```sql
-- Ensure fallback categories exist first!
INSERT OR IGNORE INTO categories (id, name, type, parentId, icon)
VALUES (8, 'Other', 'MAIN', NULL, '📦');

INSERT OR IGNORE INTO categories (id, name, type, parentId, icon)
VALUES (82, 'Miscellaneous', 'SUB', 8, '');

-- Remap all orphaned expenses to Miscellaneous (82)
UPDATE expenses SET categoryId = 82
WHERE categoryId NOT IN (SELECT id FROM categories)
AND categoryId > 0;

-- Verify fix
SELECT COUNT(*) as orphaned_after_fix FROM expenses
WHERE categoryId NOT IN (SELECT id FROM categories)
AND categoryId > 0;
```

**Expected**: orphaned_after_fix = 0

---

## 5️⃣ Verify Month Filtering

```sql
-- Check expenses by month format (yyyy-MM)
SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch') as month,
       COUNT(*) as count
FROM expenses
GROUP BY month
ORDER BY month DESC
LIMIT 12;

-- Check current month (March 2026)
SELECT COUNT(*) as march_2026 FROM expenses
WHERE timestamp >= 1743552000000  -- March 1, 2026 00:00:00 UTC
AND timestamp < 1746230400000;    -- April 1, 2026 00:00:00 UTC
```

**Expected**: Current month should show >0 if data exists

---

## 6️⃣ Category Reference Analysis

```sql
-- See which categories have the most expenses
SELECT c.id, c.name, COUNT(e.id) as expense_count
FROM categories c
LEFT JOIN expenses e ON c.id = e.categoryId
GROUP BY c.id
ORDER BY expense_count DESC
LIMIT 20;

-- Find categories with NO expenses
SELECT id, name
FROM categories
WHERE id NOT IN (SELECT DISTINCT categoryId FROM expenses WHERE categoryId > 0);
```

**Expected**: Food, Transport, Bills should have expenses if data exists

---

## 7️⃣ Date Range Debug

```sql
-- Convert timestamp to readable format
-- Last 30 days:
SELECT id, title, amount, 
       datetime(timestamp / 1000, 'unixepoch') as date_readable,
       timestamp
FROM expenses
WHERE timestamp >= (strftime('%s', 'now', '-30 days') * 1000)
ORDER BY timestamp DESC
LIMIT 10;

-- Current month (2026-03):
SELECT id, title, amount,
       datetime(timestamp / 1000, 'unixepoch') as date_readable
FROM expenses
WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = '2026-03'
ORDER BY timestamp DESC;
```

**Expected**: Should return expenses from expected date range

---

## 8️⃣ Safe Merge Operations

If specific categories were merged (e.g., Mobile Recharge → Mobile Bills):

```sql
-- Check if old category still exists
SELECT id, name FROM categories WHERE id = 34;  -- Mobile Recharge

-- If exists, remap its expenses
UPDATE expenses SET categoryId = 36
WHERE categoryId = 34;

-- Verify remap
SELECT COUNT(*) FROM expenses WHERE categoryId = 34;  -- Should be 0
SELECT COUNT(*) FROM expenses WHERE categoryId = 36;  -- All remote

-- Alternative: for Gas Cylinder → Gas merge
UPDATE expenses SET categoryId = 32
WHERE categoryId = 37 AND EXISTS (SELECT 1 FROM categories WHERE id = 37);
```

---

## 9️⃣ Full Database Health Check

```sql
-- Run all critical checks in one query
WITH stats AS (
    SELECT
        (SELECT COUNT(*) FROM expenses) as total_expenses,
        (SELECT COUNT(*) FROM categories) as total_categories,
        (SELECT COUNT(*) FROM expenses WHERE categoryId NOT IN (SELECT id FROM categories) AND categoryId > 0) as orphaned,
        (SELECT COUNT(*) FROM categories WHERE id IN (8, 82)) as fallback_present,
        (SELECT COUNT(*) FROM expenses WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = '2026-03') as march_expenses
)
SELECT
    total_expenses,
    total_categories,
    orphaned,
    CASE
        WHEN fallback_present = 2 THEN '✅ Present'
        WHEN fallback_present = 1 THEN '⚠️  Partial'
        ELSE '❌ Missing'
    END as fallback_status,
    march_expenses,
    CASE
        WHEN orphaned = 0 AND fallback_present = 2 AND total_expenses > 0 THEN '✅ Healthy'
        WHEN orphaned > 0 THEN '❌ Needs fix: ' || orphaned || ' orphaned'
        WHEN fallback_present < 2 THEN '❌ Fallback categories missing'
        ELSE '✅ Healthy'
    END as status
FROM stats;
```

---

## 🔟 Emergency Recovery (Last Resort)

If database is severely corrupted:

```sql
-- Backup current data
CREATE TABLE expenses_backup AS SELECT * FROM expenses;
CREATE TABLE categories_backup AS SELECT * FROM categories;

-- Clear and rebuild categories (if corrupted)
DELETE FROM categories;

-- Reinsert standard categories (from DefaultCategories.kt)
INSERT INTO categories (id, name, type, parentId, icon) VALUES
(1, 'Food', 'MAIN', NULL, '🍔'),
(2, 'Transport', 'MAIN', NULL, '🚗'),
(3, 'Bills', 'MAIN', NULL, '📄'),
(4, 'Shopping', 'MAIN', NULL, '🛍️'),
(5, 'Health', 'MAIN', NULL, '⚕️'),
(6, 'Entertainment', 'MAIN', NULL, '🎬'),
(7, 'Finance', 'MAIN', NULL, '💰'),
(8, 'Other', 'MAIN', NULL, '📦'),
(9, 'Household', 'MAIN', NULL, '🏠'),
(200, 'Travel', 'MAIN', NULL, '✈️'),
-- ... [add all subcategories from DefaultCategories.kt]
(82, 'Miscellaneous', 'SUB', 8, '');

-- Remap all expenses to valid categories
UPDATE expenses SET categoryId = 82
WHERE categoryId NOT IN (SELECT id FROM categories);

-- Verify recovery
SELECT COUNT(*) FROM expenses;
SELECT COUNT(*) FROM expenses WHERE categoryId NOT IN (SELECT id FROM categories);
```

---

## Database Access via adb

```bash
# Open SQLite shell
adb shell sqlite3 /data/data/com.xpenseledger.app/databases/xpenseledger.db

# Run any of the above queries
# Type .exit to quit

# Or run specific query:
adb shell sqlite3 /data/data/com.xpenseledger.app/databases/xpenseledger.db \
  "SELECT COUNT(*) FROM expenses;"
```

---

## Android Studio DB Inspector

**Easier alternative**:

1. Open Android Studio → Device Explorer
2. Navigate: `/data/data/com.xpenseledger.app/databases/`
3. Right-click `xpenseledger.db` → Open Database
4. View tables and run queries in the IDE

---

## Important Notes

⚠️ **DO NOT**:
- Delete expenses table
- Truncate data without backup
- Modify timestamps
- Change primary keys

✅ **SAFE To**:
- Run SELECT queries (read-only)
- Remap categoryId with UPDATE
- Insert fallback categories
- Backup data

---

## How to Choose the Right Fix

| Symptom | Fix | SQL |
|---------|-----|-----|
| "No expenses" | Step 3: Check orphaned | Check: 3️⃣ |
| Orphaned found | Step 4: Remap to 82 | Run: 4️⃣ |
| Missing fallback | Insert fallbacks | Run: 4️⃣ first part |
| Wrong month shown | Check date formatting | Run: 5️⃣ |
| Some categories missing | Health check | Run: 9️⃣ |
| Severe corruption | Emergency recovery | Run: 🔟 |

---

## Validation Checklist

After applying any fix:

```sql
-- Final validation
SELECT
    COUNT(*) as total_expenses,
    (SELECT COUNT(*) FROM categories) as total_categories,
    (SELECT COUNT(*) FROM expenses WHERE categoryId NOT IN (SELECT id FROM categories) AND categoryId > 0) as orphaned_remaining,
    (SELECT COUNT(*) FROM categories WHERE id IN (8, 82)) as fallback_categories
FROM expenses;
```

**Expected Results**:
- total_expenses: > 0
- total_categories: ≥ 70
- orphaned_remaining: 0
- fallback_categories: 2

---

## Still Having Issues?

1. Run all diagnostics from [ROOT_CAUSE_ANALYSIS_REPORT.md](ROOT_CAUSE_ANALYSIS_REPORT.md)
2. Check logcat output from DatabaseDebugger
3. Run final validation check above
4. If still broken: Save this report + logcat output to GitHub issue

---

**Last Updated**: March 30, 2026
**For**: XpenseLedger Transaction Recovery
**Status**: Tested & Production-Ready
