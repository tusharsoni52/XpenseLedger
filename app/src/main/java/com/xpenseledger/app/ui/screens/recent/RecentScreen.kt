package com.xpenseledger.app.ui.screens.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.ui.components.DashboardBackground
import com.xpenseledger.app.ui.components.InputField
import com.xpenseledger.app.ui.screens.add.AddExpenseScreen
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.viewmodel.CategoryViewModel
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Colours
// ─────────────────────────────────────────────────────────────────────────────
private val incomeColor   = Color(0xFF34D399)
private val expenseColor  = Color(0xFFF87171)
private val transferColor = Color(0xFFFB923C)

// ─────────────────────────────────────────────────────────────────────────────
//  Date grouping helpers
// ─────────────────────────────────────────────────────────────────────────────
private val FMT_DAY     = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val FMT_DISPLAY = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
private val FMT_TIME    = SimpleDateFormat("hh:mm a", Locale.getDefault())
private val FMT_MONTH_SAVE = SimpleDateFormat("yyyy-MM", Locale.US)

private fun dayKey(ts: Long): String = FMT_DAY.format(Date(ts))

private fun dayLabel(key: String): String {
    val today     = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(System.currentTimeMillis() - 86_400_000L)
    return when (key) {
        today     -> "Today"
        yesterday -> "Yesterday"
        else      -> try { FMT_DISPLAY.format(FMT_DAY.parse(key)!!) } catch (e: Exception) { key }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    vm:         ExpenseViewModel,
    categoryVm: CategoryViewModel
) {
    val allRecent     by vm.recentActivity.collectAsState()
    var searchQuery   by remember { mutableStateOf("") }
    var typeFilter    by remember { mutableStateOf<TransactionType?>(null) }
    val editingExpense = remember { mutableStateOf<Expense?>(null) }
    val snackbarState  = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()
    var lastDeleted    by remember { mutableStateOf<Expense?>(null) }

    fun onDeleteWithUndo(expense: Expense) {
        lastDeleted = expense
        vm.deleteExpense(expense)
        scope.launch {
            val result = snackbarState.showSnackbar(
                message     = "Transaction deleted",
                actionLabel = "Undo",
                duration    = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                lastDeleted?.let { vm.addExpense(
                    title         = it.title,
                    amount        = it.amount,
                    category      = it.category,
                    subCategory   = it.subCategory,
                    categoryId    = it.categoryId,
                    subCategoryId = it.subCategoryId,
                    timestamp     = it.timestamp,
                    type          = it.type
                ) }
            }
            lastDeleted = null
        }
    }

    // Apply search + type filter
    val filtered = remember(allRecent, searchQuery, typeFilter) {
        allRecent.filter { e ->
            (typeFilter == null || e.type == typeFilter) &&
            (searchQuery.isBlank() ||
                e.title.contains(searchQuery, ignoreCase = true) ||
                e.category.contains(searchQuery, ignoreCase = true))
        }
    }

    // Group by day key for section headers
    val grouped: List<Pair<String, List<Expense>>> = remember(filtered) {
        filtered.groupBy { dayKey(it.timestamp) }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

    // Summary totals for the filtered list
    val totalIncome    = remember(filtered) { filtered.filter { it.type == TransactionType.INCOME   }.sumOf { it.amount } }
    val totalExpenses  = remember(filtered) { filtered.filter { it.type == TransactionType.EXPENSE  }.sumOf { it.amount } }
    val totalTransfers = remember(filtered) { filtered.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount } }

    val listState = rememberLazyListState()

        DashboardBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost   = { SnackbarHost(snackbarState, modifier = Modifier.padding(bottom = 80.dp)) },
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1923))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text       = "Recent Activity",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = "Last ${ExpenseViewModel.RECENT_DAYS.toInt()} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── Search bar ───────────────────────────────────────────────
                item(key = "search") {
                    InputField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        label         = "Search recent transactions",
                        leadingIcon   = {
                            Icon(Icons.Default.Search, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                }

                // ── Type filter chips ────────────────────────────────────────
                item(key = "filter") {
                    RecentTypeFilterRow(
                        active   = typeFilter,
                        onSelect = { typeFilter = it }
                    )
                }

                // ── Summary strip ────────────────────────────────────────────
                if (filtered.isNotEmpty()) {
                    item(key = "summary") {
                        RecentSummaryStrip(
                            income    = totalIncome,
                            expenses  = totalExpenses,
                            transfers = totalTransfers
                        )
                    }
                }

                // ── Transaction list grouped by day ──────────────────────────
                if (grouped.isEmpty()) {
                    item(key = "empty") {
                        RecentEmptyState(hasFilter = searchQuery.isNotBlank() || typeFilter != null)
                    }
                } else {
                    grouped.forEach { (dayKey, dayItems) ->
                        // Day header
                        item(key = "header_$dayKey") {
                            RecentDayHeader(label = dayLabel(dayKey), count = dayItems.size)
                        }
                        // Transactions
                        itemsIndexed(
                            items = dayItems,
                            key   = { _, e -> "tx_${e.id}" }
                        ) { _, expense ->
                            RecentTransactionRow(
                                expense  = expense,
                                onEdit   = { editingExpense.value = it },
                                onDelete = { onDeleteWithUndo(it) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                item(key = "bottom_space") { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    editingExpense.value?.let { expense ->
        Dialog(
            onDismissRequest = { editingExpense.value = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.95f)) {
                AddExpenseScreen(
                    editExpense      = expense,
                    initialTimestamp = expense.timestamp,
                    categoryVm       = categoryVm,
                    onDismiss        = { editingExpense.value = null },
                    onConfirm        = { title, amount, category, subCategory,
                                         categoryId, subCategoryId, timestamp, type ->
                        vm.updateExpense(expense.copy(
                            title         = title,
                            amount        = amount,
                            category      = category,
                            subCategory   = subCategory,
                            categoryId    = categoryId,
                            subCategoryId = subCategoryId,
                            timestamp     = timestamp,
                            type          = type
                        ))
                        vm.selectMonth(FMT_MONTH_SAVE.format(Date(timestamp)))
                        editingExpense.value = null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Summary strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentSummaryStrip(
    income:    Double,
    expenses:  Double,
    transfers: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryCell("Income",    "₹%.0f".format(income),    incomeColor)
            VerticalDivider(modifier = Modifier.height(36.dp))
            SummaryCell("Expenses",  "₹%.0f".format(expenses),  expenseColor)
            VerticalDivider(modifier = Modifier.height(36.dp))
            SummaryCell("Transfers", "₹%.0f".format(transfers), transferColor)
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Day header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentDayHeader(label: String, count: Int) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = XpensePrimary,
            modifier   = Modifier.weight(1f)
        )
        Text(
            text  = "$count txn${if (count > 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
    HorizontalDivider(thickness = 0.4.dp, color = XpensePrimary.copy(alpha = 0.3f))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Transaction row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTransactionRow(
    expense:  Expense,
    onEdit:   (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (expense.type) {
        TransactionType.INCOME   -> incomeColor
        TransactionType.EXPENSE  -> expenseColor
        TransactionType.TRANSFER -> transferColor
    }
    val typeIcon = when (expense.type) {
        TransactionType.INCOME   -> Icons.Default.ArrowDownward
        TransactionType.EXPENSE  -> Icons.Default.ArrowUpward
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    }
    val sign = when (expense.type) {
        TransactionType.INCOME   -> "+"
        TransactionType.EXPENSE  -> "-"
        TransactionType.TRANSFER -> "⇄"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Type indicator dot ────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .background(
                        typeColor.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = null,
                    tint = typeColor, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(12.dp))

            // ── Title + category ──────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = expense.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (!expense.subCategory.isNullOrBlank()) {
                        Text("·", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(
                            text  = expense.subCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text     = FMT_TIME.format(Date(expense.timestamp)),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }

            // ── Amount ────────────────────────────────────────────────────────
            Text(
                text       = "$sign ₹%.2f".format(expense.amount),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = typeColor,
                modifier   = Modifier.padding(horizontal = 8.dp)
            )

            // ── Actions ───────────────────────────────────────────────────────
            IconButton(onClick = { onEdit(expense) }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onDelete(expense) }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Type filter row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTypeFilterRow(
    active:   TransactionType?,
    onSelect: (TransactionType?) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // All chip
        FilterChip(
            selected = active == null,
            onClick  = { onSelect(null) },
            label    = { Text("All", style = MaterialTheme.typography.labelMedium, fontSize = 12.sp) },
            colors   = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF94A3B8).copy(alpha = 0.20f),
                selectedLabelColor     = Color(0xFF94A3B8)
            )
        )
        listOf(
            Triple(TransactionType.INCOME,   "Income",    incomeColor),
            Triple(TransactionType.EXPENSE,  "Expenses",  expenseColor),
            Triple(TransactionType.TRANSFER, "Transfers", transferColor)
        ).forEach { (type, label, color) ->
            FilterChip(
                selected = active == type,
                onClick  = { onSelect(if (active == type) null else type) },
                label    = { Text(label, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = color.copy(alpha = 0.20f),
                    selectedLabelColor        = color,
                    selectedLeadingIconColor  = color
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentEmptyState(hasFilter: Boolean) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.SearchOff,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier           = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text  = if (hasFilter) "No results found" else "No activity in the last ${ExpenseViewModel.RECENT_DAYS.toInt()} days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasFilter) {
                Text(
                    text  = "Try clearing the search or filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

