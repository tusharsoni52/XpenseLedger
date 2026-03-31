package com.xpenseledger.app.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.ui.components.CategoryBreakdownCard
import com.xpenseledger.app.ui.components.DashboardBackground
import com.xpenseledger.app.ui.components.EmptyExpenseState
import com.xpenseledger.app.ui.components.FinancialSummaryCard
import com.xpenseledger.app.ui.components.InputField
import com.xpenseledger.app.ui.components.SectionHeader
import com.xpenseledger.app.ui.components.TransactionRow
import com.xpenseledger.app.ui.screens.add.AddExpenseScreen
import com.xpenseledger.app.ui.viewmodel.BackupResult
import com.xpenseledger.app.ui.viewmodel.CategoryViewModel
import com.xpenseledger.app.ui.viewmodel.DashboardUiState
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import com.xpenseledger.app.ui.viewmodel.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FMT_DISPLAY = SimpleDateFormat("MMM yy", Locale.getDefault())
private val FMT_KEY     = SimpleDateFormat("yyyy-MM", Locale.US)

private fun monthLabel(key: String): String = try {
    FMT_DISPLAY.format(FMT_KEY.parse(key)!!)
} catch (e: Exception) { key }

// ─────────────────────────────────────────────────────────────────────────────
//  HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm:         ExpenseViewModel,
    categoryVm: CategoryViewModel,
    profileVm:  UserProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val profile           by profileVm.profile.collectAsState()
    val menuExpanded       = remember { mutableStateOf(false) }
    val snackbarHostState  = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { vm.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importData(it) } }

    LaunchedEffect(Unit) {
        vm.backupResult.collect { result ->
            val msg = when (result) {
                is BackupResult.ExportSuccess -> "✅ Backup exported successfully"
                is BackupResult.ImportSuccess -> "✅ Data imported successfully"
                is BackupResult.Error         -> "❌ ${result.message}"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text       = if (profile.name.isNotBlank())
                                     "Hi, ${profile.name.trim().split(" ").first()}"
                                 else "XpenseLedger",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.align(Alignment.CenterStart)
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { menuExpanded.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(
                        expanded         = menuExpanded.value,
                        onDismissRequest = { menuExpanded.value = false }
                    ) {
                        DropdownMenuItem(text = { Text("Export backup") }, onClick = {
                            menuExpanded.value = false
                            exportLauncher.launch("xpenseledger_backup.xpbak")
                        })
                        DropdownMenuItem(text = { Text("Import backup") }, onClick = {
                            menuExpanded.value = false
                            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        })
                    }
                }
            }
        }
    ) { innerPadding ->
        DashboardBackground {
            DashboardContent(
                vm         = vm,
                categoryVm = categoryVm,
                modifier   = Modifier.padding(innerPadding)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dashboard content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardContent(
    vm:         ExpenseViewModel,
    categoryVm: CategoryViewModel,
    modifier:   Modifier = Modifier
) {
    val uiState           by vm.dashboardUiState.collectAsState()
    val activeTypeFilter  by vm.typeFilter.collectAsState()
    val editingExpense     = remember { mutableStateOf<Expense?>(null) }
    val searchText         = rememberSaveable { mutableStateOf("") }
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }

    AnimatedContent(
        targetState    = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label          = "dashboardState",
        modifier       = modifier.fillMaxSize()
    ) { state ->

        when (state) {
            // ── Loading ───────────────────────────────────────────────────────
            is DashboardUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // ── Empty ─────────────────────────────────────────────────────────
            is DashboardUiState.Empty -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "monthFilter") {
                        MonthFilterRow(
                            months        = state.availableMonths,
                            selectedMonth = state.selectedMonth,
                            onSelect      = { vm.selectMonth(it) }
                        )
                    }
                    item(key = "summaryCard") {
                        FinancialSummaryCard(
                            totalIncome    = state.totalIncome,
                            totalExpenses  = state.totalExpenses,
                            totalTransfers = state.totalTransfers,
                            balance        = state.balance,
                            period         = monthLabel(state.selectedMonth)
                        )
                    }
                    item(key = "search") {
                        InputField(
                            value         = searchText.value,
                            onValueChange = { searchText.value = it; vm.setQuery(it) },
                            label         = "Search transactions"
                        )
                    }
                    item(key = "typeFilter") {
                        TypeFilterRow(
                            active   = activeTypeFilter,
                            onSelect = { vm.setTypeFilter(it) }
                        )
                    }
                    item(key = "empty") { EmptyExpenseState() }
                    item(key = "bottom_space") { Spacer(Modifier.height(16.dp)) }
                }
            }

            // ── Success ───────────────────────────────────────────────────────
            is DashboardUiState.Success -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "monthFilter") {
                        MonthFilterRow(
                            months        = state.availableMonths,
                            selectedMonth = state.selectedMonth,
                            onSelect      = { vm.selectMonth(it) }
                        )
                    }

                    item(key = "summaryCard") {
                        FinancialSummaryCard(
                            totalIncome    = state.totalIncome,
                            totalExpenses  = state.totalExpenses,
                            totalTransfers = state.totalTransfers,
                            balance        = state.balance,
                            period         = monthLabel(state.selectedMonth)
                        )
                    }

                    item(key = "search") {
                        InputField(
                            value         = searchText.value,
                            onValueChange = { searchText.value = it; vm.setQuery(it) },
                            label         = "Search transactions"
                        )
                    }

                    item(key = "typeFilter") {
                        TypeFilterRow(
                            active   = activeTypeFilter,
                            onSelect = { vm.setTypeFilter(it) }
                        )
                    }

                    // ── Expense category breakdown (only when showing Expenses or All) ──
                    val showCategoryBreakdown = activeTypeFilter == null ||
                                               activeTypeFilter == TransactionType.EXPENSE
                    if (showCategoryBreakdown && state.categoryEntries.isNotEmpty()) {
                        item(key = "catHeader") {
                            SectionHeader(
                                title    = "By Category · ${monthLabel(state.selectedMonth)}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = state.categoryEntries,
                            key   = { _, e -> "cat_${e.first}" }
                        ) { idx, (category, amount) ->
                            CategoryBreakdownCard(
                                category   = category,
                                amount     = amount,
                                fraction   = if (state.grandTotal > 0)
                                                 (amount / state.grandTotal).toFloat() else 0f,
                                index      = idx,
                                expenses   = state.categoryExpenses[category] ?: emptyList(),
                                isExpanded = expandedCategories.contains(category),
                                onToggle   = {
                                    expandedCategories =
                                        if (expandedCategories.contains(category))
                                            expandedCategories - category
                                        else
                                            expandedCategories + category
                                },
                                onEdit     = { editingExpense.value = it },
                                onDelete   = { vm.deleteExpense(it) },
                                modifier   = Modifier.animateItem()
                            )
                        }
                    }

                    // ── Unified transaction list ───────────────────────────────
                    // Shown for Income, Transfer, or when category breakdown is off
                    val showTransactionList = activeTypeFilter != null &&
                                             activeTypeFilter != TransactionType.EXPENSE
                    if (showTransactionList && state.filteredExpenses.isNotEmpty()) {
                        item(key = "txHeader") {
                            SectionHeader(
                                title = when (activeTypeFilter) {
                                    TransactionType.INCOME   -> "Income · ${monthLabel(state.selectedMonth)}"
                                    TransactionType.TRANSFER -> "Transfers · ${monthLabel(state.selectedMonth)}"
                                    else                     -> "Transactions · ${monthLabel(state.selectedMonth)}"
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        itemsIndexed(
                            items = state.filteredExpenses,
                            key   = { _, e -> "tx_${e.id}" }
                        ) { _, expense ->
                            TransactionRow(
                                expense  = expense,
                                onEdit   = { editingExpense.value = it },
                                onDelete = { vm.deleteExpense(it) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item(key = "bottom_space") { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Edit overlay (Dialog) ─────────────────────────────────────────────────
    editingExpense.value?.let { expense ->
        Dialog(
            onDismissRequest = { editingExpense.value = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows  = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = WindowInsets.systemBars.getBottom(LocalDensity.current).dp + 600.dp)
            ) {
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
                        vm.selectMonth(
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))
                        )
                        editingExpense.value = null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Type filter chips  — All / Income / Expense / Transfer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypeFilterRow(
    active:   TransactionType?,
    onSelect: (TransactionType?) -> Unit
) {
    // Chip accent colors per type
    val incomeColor   = Color(0xFF34D399)
    val expenseColor  = Color(0xFFF87171)
    val transferColor = Color(0xFFFB923C)

    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // "All" chip
        TypeChip(
            label    = "All",
            selected = active == null,
            color    = MaterialTheme.colorScheme.primary,
            onClick  = { onSelect(null) }
        )
        TypeChip(label = "Income",   selected = active == TransactionType.INCOME,
            color = incomeColor,   onClick = { onSelect(TransactionType.INCOME) })
        TypeChip(label = "Expenses", selected = active == TransactionType.EXPENSE,
            color = expenseColor,  onClick = { onSelect(TransactionType.EXPENSE) })
        TypeChip(label = "Transfers", selected = active == TransactionType.TRANSFER,
            color = transferColor, onClick = { onSelect(TransactionType.TRANSFER) })
    }
}

@Composable
private fun TypeChip(
    label:    String,
    selected: Boolean,
    color:    Color,
    onClick:  () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor    = color.copy(alpha = 0.20f),
            selectedLabelColor        = color,
            selectedLeadingIconColor  = color
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Month filter row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthFilterRow(
    months:        List<String>,
    selectedMonth: String,
    onSelect:      (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        months.forEach { m ->
            FilterChip(
                selected = selectedMonth == m,
                onClick  = { onSelect(m) },
                label    = { Text(monthLabel(m)) }
            )
        }
    }
}
