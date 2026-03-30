package com.xpenseledger.app.ui.screens.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
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
import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.model.Expense
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
 * Security measures applied here (UI layer):
 *  • Title: control-char stripped, max [TITLE_MAX_LEN] chars, min 2 chars
 *  • Amount: digit-only filter, single dot, max 2 dp, range 0< x ≤9,999,999
 *  • Negative/zero amounts: blocked by [AddExpenseFormState] and button disabled state
 *  • Submit guard: [AddExpenseFormState.submitting] disables the button on first tap to
 *    prevent rapid double-submit regardless of animation timing
 *  • Fields show errors only after the user has interacted with them (not on first render)
 *  • No validation logic is in the composable's lambda bodies — all derived from [AddExpenseFormState]
 *
 * @param editExpense  When non-null the form pre-fills and the header reads "Edit Expense".
 * @param categoryVm   Provides main/sub category lists.
 * @param onDismiss    Called on back / × press.
 * @param onConfirm    Called with validated values — note [title] is already trimmed.
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
        timestamp:     Long
    ) -> Unit,
    editExpense:       Expense? = null,
    initialTimestamp:  Long     = System.currentTimeMillis()
) {
    val mainCategories by categoryVm.mainCategories.collectAsState()

    // ── Form state ────────────────────────────────────────────────────────────
    val form = remember(editExpense) {
        AddExpenseFormState(
            initialTitle     = editExpense?.title   ?: "",
            initialAmount    = editExpense?.amount?.toString() ?: "",
            initialTimestamp = editExpense?.timestamp ?: initialTimestamp
        )
    }

    val keyboard = LocalSoftwareKeyboardController.current
    // Debounce prevents double-submit if the user taps twice quickly
    val safeConfirm = rememberDebouncedClick(debounceMs = 800L) {
        keyboard?.hide()
        form.touchAll()
        if (!form.isValid) return@rememberDebouncedClick
        onConfirm(
            form.title.trim(),
            form.amount.toDouble(),
            form.mainCat!!.name,
            form.subCat?.name,
            form.mainCat!!.id,
            form.subCat?.id,
            form.timestamp
        )
    }


    // Pre-select categories once the category list arrives (edit mode)
    LaunchedEffect(mainCategories) {
        if (form.mainCat == null && editExpense != null && mainCategories.isNotEmpty()) {
            form.mainCat = mainCategories.firstOrNull {
                it.id == editExpense.categoryId || it.name == editExpense.category
            }
        }
    }

    val subList = form.mainCat?.let { categoryVm.subCategoriesFor(it.id) } ?: emptyList()

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
                // navigationBarsPadding / imePadding are intentionally NOT on the root Column.
                // When this composable runs inside a Dialog (decorFitsSystemWindows=true),
                // applying them at the root pushes the entire layout—including the pinned
                // button—off the bottom edge of the screen. Instead:
                //   • The scrollable field column uses imePadding() so fields scroll up
                //     when the keyboard opens, keeping the focused field visible.
                //   • The button Surface uses windowInsetsPadding(ime + navigationBars)
                //     so it always sits just above the keyboard / nav bar.
        ) {
            // Header
            FormHeaderBar(
                title     = if (editExpense != null) "Edit Expense" else "Add Expense",
                onDismiss = onDismiss
            )

            // Scrollable fields — weight(1f) so they never push the button off screen.
            // imePadding() here scrolls fields up when the keyboard appears so the
            // focused field is always visible without displacing the pinned button.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Title ─────────────────────────────────────────────────────
                TitleField(form)

                // ── Amount ────────────────────────────────────────────────────
                AmountField(form)

                // ── Category → Subcategory (animated) ─────────────────────────
                CategorySection(
                    form           = form,
                    mainCategories = mainCategories,
                    subList        = subList
                )

                // ── Date ──────────────────────────────────────────────────────
                DateField(
                    timestamp       = form.timestamp,
                    onPickerRequest = { showDatePicker = true }
                )

                // Extra space so the last field isn't hidden behind the confirm bar
                Spacer(Modifier.height(4.dp))
            }

            // ── Confirm button — pinned, always visible ───────────────────────
            // windowInsetsPadding(ime + navigationBars) keeps the button above
            // the soft keyboard AND the system navigation bar in all cases,
            // including when rendered inside a Dialog window.
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars)
                    ),
                color           = Color(0xFF0E1520),
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
                                                 else "Add Expense",
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
            .background(Color(0xFF0E1520))
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
        // Gradient accent line along the bottom of the header
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
            // Exclude from autofill traversal — expense titles are private
            .semantics { contentDescription = "Expense title" },
        shape         = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction      = ImeAction.Next,
            autoCorrect    = false          // prevent keyboard from logging/suggesting titles
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
//  Amount field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmountField(form: AddExpenseFormState) {
    val focusManager    = LocalFocusManager.current
    val currencyFmt     = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
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
            keyboardType = KeyboardType.Decimal,
            imeAction    = ImeAction.Done,
            autoCorrect  = false            // no clipboard/suggestion bar over a financial field
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
//  Category section
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySection(
    form:           AddExpenseFormState,
    mainCategories: List<Category>,
    subList:        List<Category>
) {
    var mainExpanded by remember { mutableStateOf(false) }
    var subExpanded  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        // ── Main category ─────────────────────────────────────────────────────
        StyledDropdown(
            label     = "Category",
            value     = form.mainCat?.let { "${it.icon}  ${it.name}" } ?: "",
            expanded  = mainExpanded,
            isError   = form.categoryError != null,
            errorMsg  = form.categoryError?.message,
            onToggle  = { mainExpanded = !mainExpanded; form.catTouched = true },
            onDismiss = { mainExpanded = false }
        ) {
            mainCategories.forEach { cat ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                                    .background(categoryBadgeColor(cat.name).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) { Text(cat.icon, fontSize = 14.sp) }
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = { form.mainCat = cat; form.subCat = null; mainExpanded = false }
                )
            }
        }

        // ── Subcategory (slides in when a main category with subs is selected) ─
        AnimatedVisibility(
            visible = subList.isNotEmpty(),
            enter   = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMedium)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                StyledDropdown(
                    label     = "Subcategory  (optional)",
                    value     = form.subCat?.name ?: "",
                    expanded  = subExpanded,
                    isError   = false,
                    errorMsg  = null,
                    onToggle  = { subExpanded = !subExpanded },
                    onDismiss = { subExpanded = false }
                ) {
                    DropdownMenuItem(
                        text    = { Text("— None —",
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { form.subCat = null; subExpanded = false }
                    )
                    subList.forEach { sub ->
                        DropdownMenuItem(
                            text    = { Text(sub.name) },
                            onClick = { form.subCat = sub; subExpanded = false }
                        )
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledDropdown(
    label:     String,
    value:     String,
    expanded:  Boolean,
    isError:   Boolean,
    errorMsg:  String?,
    onToggle:  () -> Unit,
    onDismiss: () -> Unit,
    content:   @Composable () -> Unit
) {
    val chevronAngle by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label         = "chevron"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onToggle() }) {
        OutlinedTextField(
            value          = value,
            onValueChange  = {},
            label          = { Text(label) },
            readOnly       = true,
            isError        = isError,
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth().menuAnchor(),
            shape          = RoundedCornerShape(14.dp),
            trailingIcon   = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronAngle),
                    tint = if (isError) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingText = if (errorMsg != null) { { FieldErrorText(errorMsg) } } else null,
            colors         = fieldColors(isError = isError)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) { content() }
    }
}

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
