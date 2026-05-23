package com.xpenseledger.app.ui.screens.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.ui.components.SoftGradientBackground
import com.xpenseledger.app.ui.components.categoryBadgeColor
import com.xpenseledger.app.ui.theme.DarkError
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import com.xpenseledger.app.ui.security.rememberDebouncedClick
import com.xpenseledger.app.ui.viewmodel.CategoryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Public entry point
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen Add / Edit Expense form.
 *
 * UX improvements (Option B + E):
 *  • Category picker replaced with a visual 3-column icon grid — all categories
 *    visible at-a-glance, no dropdown to open/scroll.
 *  • Subcategory picker is a horizontal scrollable chip row — 1-tap selection.
 *  • Amount keyboard uses ImeAction.Next → auto-dismisses keyboard so the
 *    category grid is immediately tappable without a manual keyboard close.
 *  • Recent categories (up to 3) shown in a dedicated row at the top of the
 *    grid for instant 1-tap repeat entries.
 *
 * Security measures (unchanged):
 *  • Title: control-char stripped, max [TITLE_MAX_LEN] chars, min 2 chars
 *  • Amount: digit-only filter, single dot, max 2 dp, range 0 < x ≤ 9,999,999
 *  • Submit guard: debounced 800 ms via [rememberDebouncedClick]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
        timestamp:     Long,
        type:          TransactionType
    ) -> Unit,
    editExpense:       Expense? = null,
    initialTimestamp:  Long     = System.currentTimeMillis(),
    // ViewModel injected here — Hilt provides it scoped to the NavBackStackEntry
    // so it survives session locks while staying isolated to this destination.
    formVm: AddExpenseViewModel = hiltViewModel()
) {
    val allMainCategories by categoryVm.mainCategories.collectAsState()

    // ── Form state ────────────────────────────────────────────────────────────
    // Edit mode: always use the expense as-is — no draft persistence needed.
    // Add mode:  seed from ViewModel (SavedStateHandle) so in-progress drafts
    //            survive session locks and process death.
    val form = remember(editExpense) {
        if (editExpense != null) {
            AddExpenseFormState(
                initialTitle           = editExpense.title,
                initialAmount          = editExpense.amount.toString(),
                initialTimestamp       = editExpense.timestamp,
                initialTransactionType = editExpense.type
            )
        } else {
            // Restore draft from ViewModel (SavedStateHandle)
            AddExpenseFormState(
                initialTitle           = formVm.title.value,
                initialAmount          = formVm.amount.value,
                initialTimestamp       = formVm.timestamp.value,
                initialTransactionType = formVm.transactionType.value
            )
        }
    }

    // ── Sync form → ViewModel on every change (add mode only) ─────────────────
    // LaunchedEffect with snapshot-derived keys re-runs whenever any field changes,
    // writing the new value into SavedStateHandle for process-death survival.
    if (editExpense == null) {
        LaunchedEffect(form.title)           { formVm.setTitle(form.title) }
        LaunchedEffect(form.amount)          { formVm.setAmount(form.amount) }
        LaunchedEffect(form.timestamp)       { formVm.setTimestamp(form.timestamp) }
        LaunchedEffect(form.transactionType) { formVm.setTransactionType(form.transactionType) }
        LaunchedEffect(form.mainCat?.id)     { formVm.setCategoryId(form.mainCat?.id) }
        LaunchedEffect(form.subCat?.id)      { formVm.setSubCategoryId(form.subCat?.id) }
    }

    // ── Category list filtered by current transaction type ────────────────────
    val mainCategories = remember(form.transactionType, allMainCategories) {
        categoryVm.mainCategoriesFor(form.transactionType)
    }

    // Recent categories (for Option E quick-pick row)
    val recentCategories = remember(form.transactionType, allMainCategories) {
        // Only show recents that are valid for the current transaction type
        val validIds = mainCategories.map { it.id }.toSet()
        categoryVm.recentMainCategories().filter { it.id in validIds }
    }

    val keyboard = LocalSoftwareKeyboardController.current

    val safeConfirm = rememberDebouncedClick(debounceMs = 800L) {
        keyboard?.hide()
        form.touchAll()
        if (!form.isValid) return@rememberDebouncedClick
        // Record recently used category before calling onConfirm
        form.mainCat?.id?.let { categoryVm.recordRecentCategory(it) }
        // Clear saved draft — entry was completed successfully
        if (editExpense == null) formVm.clearForm()
        onConfirm(
            form.title.trim(),
            form.amount.toDouble(),
            form.mainCat!!.name,
            form.subCat?.name,
            form.mainCat!!.id,
            form.subCat?.id,
            form.timestamp,
            form.transactionType
        )
    }

    // Pre-select category in edit mode once the category list is ready
    LaunchedEffect(mainCategories) {
        if (form.mainCat == null && editExpense != null && mainCategories.isNotEmpty()) {
            form.mainCat = mainCategories.firstOrNull {
                it.id == editExpense.categoryId || it.name == editExpense.category
            }
        }
    }

    // Auto-select when there is only ONE valid category for the chosen type
    LaunchedEffect(form.transactionType, mainCategories) {
        if (editExpense == null && mainCategories.size == 1) {
            form.mainCat = mainCategories.first()
        }
    }

    val subList = form.mainCat?.let {
        categoryVm.subCategoriesFor(it.id, form.transactionType)
    } ?: emptyList()

    // Auto-select single subcategory when only one is available
    LaunchedEffect(subList) {
        if (editExpense == null && subList.size == 1 && form.subCat == null) {
            form.subCat = subList.first()
        }
    }

    LaunchedEffect(form.mainCat) {
        if (form.subCat == null && editExpense != null) {
            form.subCat = subList.firstOrNull {
                it.id == editExpense.subCategoryId || it.name == editExpense.subCategory
            }
        }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // ── Date picker ───────────────────────────────────────────────────────────
    if (showDatePicker) {
        val initUtcMs = remember(form.timestamp) {
            val lc = Calendar.getInstance().also { it.timeInMillis = form.timestamp }
            Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(lc.get(Calendar.YEAR), lc.get(Calendar.MONTH),
                    lc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val dpState = rememberDatePickerState(initialSelectedDateMillis = initUtcMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { utcMs ->
                        val uc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            .also { it.timeInMillis = utcMs }
                        form.timestamp = Calendar.getInstance().apply {
                            set(uc.get(Calendar.YEAR), uc.get(Calendar.MONTH),
                                uc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dpState) }
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    SoftGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            FormHeaderBar(
                title     = if (editExpense != null) "Edit Transaction"
                            else when (form.transactionType) {
                                TransactionType.INCOME   -> "Add Income"
                                TransactionType.TRANSFER -> "Add Transfer"
                                else                     -> "Add Expense"
                            },
                onDismiss = onDismiss
            )

            // Scrollable fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Transaction Type selector ──────────────────────────────────
                if (editExpense == null) {
                    TopTypeSelector(
                        selected = form.transactionType,
                        onSelect = { newType ->
                            form.transactionType = newType
                            form.mainCat = null
                            form.subCat  = null
                        }
                    )
                }

                // ── Title ─────────────────────────────────────────────────────
                TitleField(form)

                // ── Amount ────────────────────────────────────────────────────
                AmountField(form)

                // ── Category icon grid + subcategory chips ────────────────────
                CategorySection(
                    form              = form,
                    mainCategories    = mainCategories,
                    recentCategories  = recentCategories,
                    subList           = subList,
                    onCategoryTap     = { keyboard?.hide() }
                )

                // ── Transfer/Expense toggle (Family Support, EXPENSE tab only) ─
                AnimatedVisibility(
                    visible = form.subCat?.name == "Family Support" &&
                              form.transactionType == TransactionType.EXPENSE,
                    enter   = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMedium)),
                    exit    = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    TransactionTypeToggle(
                        selected  = form.transactionType,
                        onSelect  = { form.transactionType = it }
                    )
                }

                // ── Date ──────────────────────────────────────────────────────
                DateField(
                    timestamp       = form.timestamp,
                    onPickerRequest = { showDatePicker = true }
                )

                Spacer(Modifier.height(4.dp))
            }

            // ── Confirm button — pinned, always visible ───────────────────────
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars)
                    ),
                color           = Color(0xFF0F1923),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick        = safeConfirm,
                        enabled        = form.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (form.isValid)
                                        Brush.horizontalGradient(
                                            listOf(XpensePrimary, XpenseSecondary)
                                        )
                                    else
                                        Brush.horizontalGradient(
                                            listOf(
                                                XpensePrimary.copy(alpha = 0.35f),
                                                XpenseSecondary.copy(alpha = 0.35f)
                                            )
                                        )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Check,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Text(
                                    text       = if (editExpense != null) "Save Changes"
                                                 else when (form.transactionType) {
                                                     TransactionType.INCOME   -> "Add Income"
                                                     TransactionType.TRANSFER -> "Add Transfer"
                                                     else                     -> "Add Expense"
                                                 },
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Header bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormHeaderBar(title: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1923))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.Close, contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.horizontalGradient(listOf(XpensePrimary, XpenseSecondary)))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Title field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TitleField(form: AddExpenseFormState) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value         = form.title,
        onValueChange = { form.title = sanitizeTitle(it); form.titleTouched = true },
        label         = { Text("Title") },
        placeholder   = { Text("e.g. Grocery run") },
        singleLine    = true,
        isError       = form.titleError != null,
        modifier      = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Expense title" },
        shape         = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            capitalization     = KeyboardCapitalization.Sentences,
            imeAction          = ImeAction.Next,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        supportingText = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FieldErrorText(form.titleError?.message)
                Text(
                    "${form.title.length}/$TITLE_MAX_LEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (form.title.length >= TITLE_MAX_LEN)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = fieldColors(isError = form.titleError != null)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Amount field  — ImeAction.Next so keyboard dismisses toward category grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmountField(form: AddExpenseFormState) {
    val focusManager     = LocalFocusManager.current
    val keyboard         = LocalSoftwareKeyboardController.current
    val currencyFmt      = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }
    val formattedPreview = remember(form.amount) {
        form.amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { currencyFmt.format(it) }
    }

    OutlinedTextField(
        value         = form.amount,
        onValueChange = { form.amount = sanitizeAmount(it); form.amountTouched = true },
        label         = { Text("Amount") },
        placeholder   = { Text("0.00") },
        singleLine    = true,
        isError       = form.amountError != null,
        modifier      = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Expense amount" },
        shape         = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType       = KeyboardType.Decimal,
            // Next instead of Done → pressing ✓/Next on keyboard hides it and
            // moves focus out, making the category grid immediately tappable.
            imeAction          = ImeAction.Next,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onNext = {
            focusManager.clearFocus()   // hides keyboard
            keyboard?.hide()
        }),
        prefix        = { Text("₹ ", fontWeight = FontWeight.Medium) },
        supportingText = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FieldErrorText(form.amountError?.message)
                AnimatedVisibility(visible = formattedPreview != null,
                    enter = fadeIn(tween(150)), exit = fadeOut(tween(100))) {
                    Text(formattedPreview ?: "", style = MaterialTheme.typography.labelSmall,
                        color = XpensePrimary)
                }
            }
        },
        colors = fieldColors(isError = form.amountError != null)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Category section  — icon grid + subcategory chip row  (Option B + E)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    form:             AddExpenseFormState,
    mainCategories:   List<Category>,
    recentCategories: List<Category>,
    subList:          List<Category>,
    onCategoryTap:    () -> Unit       // called to dismiss keyboard
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        // ── Section label ─────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = if (form.categoryError != null)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (form.mainCat != null) {
                Text(
                    text  = "✓ ${form.mainCat!!.icon} ${form.mainCat!!.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = XpensePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Show category error after touch
        if (form.categoryError != null) {
            Text(
                text  = form.categoryError!!.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Option E: Recent row ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = recentCategories.isNotEmpty(),
            enter   = fadeIn(tween(200)) + expandVertically(),
            exit    = fadeOut(tween(150)) + shrinkVertically()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.History,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text  = "Recent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentCategories.forEach { cat ->
                        CategoryTile(
                            category   = cat,
                            isSelected = form.mainCat?.id == cat.id,
                            isRecent   = true,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                onCategoryTap()
                                form.mainCat = cat
                                form.subCat  = null
                                form.catTouched = true
                            }
                        )
                    }
                    // Fill remaining slots with invisible boxes to keep layout stable
                    repeat(RecentCategoryStore.MAX_RECENT - recentCategories.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Divider between recent and full grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        // ── Option B: Full category icon grid (3 columns) ─────────────────────
        FlowRow(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow     = 3
        ) {
            mainCategories.forEach { cat ->
                CategoryTile(
                    category   = cat,
                    isSelected = form.mainCat?.id == cat.id,
                    isRecent   = false,
                    modifier   = Modifier.weight(1f),
                    onClick    = {
                        onCategoryTap()
                        form.mainCat = cat
                        form.subCat  = null
                        form.catTouched = true
                    }
                )
            }
        }

        // ── Subcategory chip row ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = subList.isNotEmpty(),
            enter   = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMedium)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = "Subcategory  (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SubcategoryChipRow(
                    subList    = subList,
                    selected   = form.subCat,
                    onSelect   = { form.subCat = it }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Single category tile  (used in grid and recent row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryTile(
    category:   Category,
    isSelected: Boolean,
    isRecent:   Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val accentColor  = categoryBadgeColor(category.name)
    val bgColor      = if (isSelected) accentColor.copy(alpha = 0.22f)
                       else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val borderColor  = if (isSelected) accentColor else Color.Transparent
    val borderWidth  = if (isSelected) 1.5.dp else 0.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = if (isSelected) 0.30f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = category.icon.ifBlank { "📦" },
                fontSize = 18.sp
            )
        }
        Text(
            text       = category.name,
            style      = MaterialTheme.typography.labelSmall,
            color      = if (isSelected) accentColor
                         else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 2,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Subcategory chip row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubcategoryChipRow(
    subList:  List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // "None" chip
        FilterChip(
            selected = selected == null,
            onClick  = { onSelect(null) },
            label    = {
                Text(
                    "None",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor   = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                selectedLabelColor       = MaterialTheme.colorScheme.onSurface
            )
        )
        subList.forEach { sub ->
            val isSelected = selected?.id == sub.id
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(sub) },
                label    = {
                    Text(
                        sub.name,
                        style    = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = XpensePrimary.copy(alpha = 0.18f),
                    selectedLabelColor       = XpensePrimary
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top-level transaction type selector  (Expense / Income / Transfer)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopTypeSelector(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val items = listOf(
        TransactionType.EXPENSE  to ("Expense"  to Color(0xFFF87171)),
        TransactionType.INCOME   to ("Income"   to Color(0xFF34D399)),
        TransactionType.TRANSFER to ("Transfer" to Color(0xFFFB923C))
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "Transaction Type",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (type, pair) ->
                val (label, color) = pair
                val isSelected = selected == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) color.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { onSelect(type) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) color
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        style      = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Transfer / Expense toggle  (shown only for Family Support sub-category)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionTypeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Transaction Type",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(TransactionType.TRANSFER to "Transfer", TransactionType.EXPENSE to "Expense")
                .forEach { (type, label) ->
                    val isSelected = selected == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(XpensePrimary, XpenseSecondary))
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            XpensePrimary.copy(alpha = 0.08f),
                                            XpenseSecondary.copy(alpha = 0.08f)
                                        )
                                    )
                            )
                            .clickable { onSelect(type) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected) Color.White
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                            style      = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
        }
        Text(
            text  = if (selected == TransactionType.TRANSFER)
                        "Transfer — excluded from expense totals"
                    else
                        "Expense — counted in your spending totals",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Date field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DateField(timestamp: Long, onPickerRequest: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value         = dateFmt.format(Date(timestamp)),
            onValueChange = {},
            label         = { Text("Date") },
            enabled       = false,
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            trailingIcon  = {
                Icon(Icons.Default.DateRange, contentDescription = "Pick date",
                    tint = MaterialTheme.colorScheme.primary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor         = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor       = MaterialTheme.colorScheme.outline,
                disabledLabelColor        = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor    = Color.Transparent
            )
        )
        // Invisible overlay captures clicks while the TextField is disabled
        Box(Modifier.matchParentSize().clickable(onClick = onPickerRequest))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────────────────────


@Composable
private fun FieldErrorText(message: String?) {
    AnimatedVisibility(visible = message != null,
        enter = fadeIn(tween(150)) + expandVertically(),
        exit  = fadeOut(tween(100)) + shrinkVertically()
    ) {
        Text(message ?: "", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun fieldColors(isError: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = if (isError) DarkError else XpensePrimary,
    unfocusedBorderColor    = if (isError) DarkError.copy(0.7f)
                              else MaterialTheme.colorScheme.outline,
    focusedLabelColor       = if (isError) MaterialTheme.colorScheme.error else XpensePrimary,
    unfocusedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor             = XpensePrimary,
    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
    errorBorderColor        = MaterialTheme.colorScheme.error,
    errorLabelColor         = MaterialTheme.colorScheme.error,
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor  = Color.Transparent,
    errorContainerColor     = Color.Transparent
)
