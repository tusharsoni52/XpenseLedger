# Expense Update Button - Implementation Guide

## Issue Fixed ✅

**Problem**: The "Save Changes" button was not appearing when editing an existing expense.

**Root Cause**: The `AddExpenseScreen` composable in the navigation graph was not receiving the `editExpense` parameter, so it always defaulted to `null`, showing only the "Add Expense" button.

## Solution Implemented

### 1. Added State Management to ExpenseViewModel

```kotlin
// In ExpenseViewModel.kt - Added to private mutable state:
private val _editingExpense = MutableStateFlow<Expense?>(null)

// Exposed as public read-only flow:
val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()

// Added helper methods:
fun setEditingExpense(expense: Expense?) {
    _editingExpense.value = expense
}

fun clearEditingExpense() {
    _editingExpense.value = null
}
```

### 2. Updated updateExpense() Method

```kotlin
fun updateExpense(expense: Expense) {
    viewModelScope.launch { 
        repo.update(expense)
        _editingExpense.value = null  // Clear editing state after update
    }
}
```

### 3. Modified AppNavGraph AddExpense Route

**Before**:
```kotlin
AddExpenseScreen(
    categoryVm = categoryVm,
    onDismiss  = { navController.popBackStack() },
    onConfirm  = { title, amount, ... ->
        expenseVm.addExpense(...)  // Only supported add
        navController.popBackStack()
    }
)
```

**After**:
```kotlin
// Collect the editing expense state
val editingExpense by expenseVm.editingExpense.collectAsState()

// Pass it to AddExpenseScreen
AddExpenseScreen(
    editExpense  = editingExpense,  // Now supports both add and edit
    categoryVm   = categoryVm,
    onDismiss    = { 
        expenseVm.clearEditingExpense()
        navController.popBackStack() 
    },
    onConfirm    = { title, amount, category, subCategory,
                     categoryId, subCategoryId, timestamp ->
        if (editingExpense != null) {
            // Update existing expense
            expenseVm.updateExpense(editingExpense.copy(
                title = title, amount = amount, category = category,
                subCategory = subCategory, categoryId = categoryId,
                subCategoryId = subCategoryId, timestamp = timestamp
            ))
        } else {
            // Add new expense
            expenseVm.addExpense(
                title, amount, category,
                subCategory, categoryId, subCategoryId, timestamp
            )
        }
        expenseVm.selectMonth(...)
        expenseVm.clearEditingExpense()
        navController.popBackStack()
    }
)
```

## How to Use (For Developers)

### Add New Expense (from FAB)
```kotlin
// In HomeScreen or any screen:
navController.navigate(Screen.AddExpense.route) {
    launchSingleTop = true
}
// Button shows: "Add Expense"
```

### Edit Existing Expense (Full-Screen)
```kotlin
// From anywhere in the app:
expenseVm.setEditingExpense(expenseToEdit)
navController.navigate(Screen.AddExpense.route) {
    launchSingleTop = true
}
// Button shows: "Save Changes"
```

### Edit Existing Expense (Dialog - HomeScreen)
```kotlin
// This approach still works:
editingExpense.value = expenseToEdit
// Shows AddExpenseScreen in Dialog with "Save Changes" button
```

## Button Text Logic

In `AddExpenseScreen.kt` (line ~338):
```kotlin
Text(
    text = if (editExpense != null) "Save Changes"
           else "Add Expense",
    fontWeight = FontWeight.SemiBold,
    color = Color.White
)

// Icon also changes:
// Add: ✓ symbol with "Add Expense"
// Edit: ✓ symbol with "Save Changes"
```

## Files Modified

| File | Changes |
|------|---------|
| `app/src/main/java/com/xpenseledger/app/ui/viewmodel/ExpenseViewModel.kt` | Added editing state and helper methods |
| `app/src/main/java/com/xpenseledger/app/ui/navigation/AppNavGraph.kt` | Collect editing state, pass to AddExpenseScreen, handle both add/update |

## validation Checklist

- ✅ Button text changes based on `editExpense` parameter
- ✅ "Add Expense" text shows when adding new
- ✅ "Save Changes" text shows when editing
- ✅ Update flow correctly calls `updateExpense()`
- ✅ State clears after successful update
- ✅ Dialog approach in HomeScreen still functional
- ✅ Form pre-fills with expense data in edit mode
- ✅ Categories pre-select correctly in edit mode

## Related Code Sections

### AddExpenseScreen Parameters
```kotlin
fun AddExpenseScreen(
    categoryVm:        CategoryViewModel,
    onDismiss:         () -> Unit,
    onConfirm:         (
        title:         String,
        amount:        Double,
        category:      String,
        subCategory:   String?,
        categoryId:    Long,
        subCategoryId: Long?,
        timestamp:     Long
    ) -> Unit,
    editExpense:       Expense? = null,  // ← Check this!
    initialTimestamp:  Long     = System.currentTimeMillis()
)
```

### Form State in AddExpenseScreen
```kotlin
val form = remember(editExpense) {
    AddExpenseFormState(
        initialTitle     = editExpense?.title   ?: "",
        initialAmount    = editExpense?.amount?.toString() ?: "",
        initialTimestamp = editExpense?.timestamp ?: initialTimestamp
    )
}
```

## Enhanced Features (Optional Additions)

### 1. Navigate to Edit from HomeScreen
```kotlin
// In CategoryBreakdownCard or ExpenseItem:
onEdit = { expense ->
    navController.navigate(Screen.AddExpense.route)
    expenseVm.setEditingExpense(expense)
}
```

### 2. Add ViewModel Method for Navigation-Aware Editing
```kotlin
fun showEditForm(expense: Expense, navController: NavHostController) {
    setEditingExpense(expense)
    navController.navigate(Screen.AddExpense.route)
}
```

### 3. Auto-Clear Editing State on Navigation Pop
```kotlin
LaunchedEffect(currentRoute) {
    if (currentRoute != Screen.AddExpense.route) {
        expenseVm.clearEditingExpense()
    }
}
```

## Troubleshooting

### Button Still Shows "Add Expense"
- Check that `expenseVm.editingExpense.collectAsState()` is being called
- Verify `editExpense = editingExpense` is passed to AddExpenseScreen
- Ensure `setEditingExpense()` was called before navigation

### State Not Clearing
- Verify `clearEditingExpense()` is called in onDismiss
- Check that `updateExpense()` clears state internally
- Ensure navigation pops after update

### Form Doesn't Pre-Fill
- Check that `editExpense` parameter is non-null in AddExpenseScreen
- Verify form state uses `remember(editExpense)` correctly
- Ensure category lookup uses both ID and name matching

## Testing Guide

### Test 1: Add New Expense
1. Press FAB button
2. Verify button text is "Add Expense"
3. Fill form and confirm
4. Verify expense appears in list

### Test 2: Edit from Dialog (HomeScreen)
1. Long-press any expense in HomeScreen
2. Dialog appears with pre-filled data
3. Verify button text is "Save Changes"
4. Modify and confirm
5. Verify expense updated in list

### Test 3: Edit from Navigation (Full-Screen)
1. In code, call `expenseVm.setEditingExpense(expense)`
2. Navigate to `Screen.AddExpense.route`
3. Verify full-screen form shows with pre-filled data
4. Verify button text is "Save Changes"
5. Modify and confirm
6. Verify expense updated in list

### Test 4: State Cleanup
1. Start editing an expense
2. Press X/back without confirming
3. Verify editing state is cleared
4. Press FAB again
5. Verify button text is "Add Expense" (not "Save Changes")

## Performance Notes

- StateFlow used for efficient state updates
- Editing state only emitted when explicitly set
- No unnecessary recompositions of entire form
- Debounced confirm prevents double-submit (already implemented)

---

**Last Updated**: March 2026  
**Status**: ✅ Implemented and Ready for Use
