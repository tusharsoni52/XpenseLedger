package com.xpenseledger.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.domain.usecase.ExportExpensesUseCase
import com.xpenseledger.app.domain.usecase.ImportExpensesUseCase
import com.xpenseledger.app.domain.usecase.ExportToExcelUseCase
import com.xpenseledger.app.domain.usecase.ImportFromExcelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  Backup one-shot result
// ─────────────────────────────────────────────────────────────────────────────

sealed class BackupResult {
    object ExportSuccess : BackupResult()
    object ImportSuccess : BackupResult()
    data class Error(val message: String) : BackupResult()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Dashboard UI state  — single object collected once per recomposition
// ─────────────────────────────────────────────────────────────────────────────

sealed class DashboardUiState {
    /** DB not yet ready */
    object Loading : DashboardUiState()
    /** DB ready but no transactions match the current filter */
    data class Empty(
        val selectedMonth:    String,
        val availableMonths:  List<String>,
        val transfersForMonth: List<Expense> = emptyList(),
        val totalIncome:      Double = 0.0,
        val totalExpenses:    Double = 0.0,
        val totalTransfers:   Double = 0.0,
        val balance:          Double = 0.0
    ) : DashboardUiState()
    /** Normal data */
    data class Success(
        val selectedMonth:    String,
        val availableMonths:  List<String>,
        val filteredExpenses: List<Expense>,
        val categoryEntries:  List<Pair<String, Double>>,
        val categoryExpenses: Map<String, List<Expense>>,
        val grandTotal:       Double,
        val searchQuery:      String,
        val transfersForMonth: List<Expense> = emptyList(),
        // ── Financial summary (for the selected month) ──────────────────────
        val totalIncome:      Double = 0.0,
        val totalExpenses:    Double = 0.0,
        val totalTransfers:   Double = 0.0,
        val balance:          Double = 0.0
    ) : DashboardUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repo: ExpenseRepository,
    private val exportUseCase: ExportExpensesUseCase,
    private val importUseCase: ImportExpensesUseCase,
    private val exportToExcelUseCase: ExportToExcelUseCase,
    private val importFromExcelUseCase: ImportFromExcelUseCase,
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val _query          = MutableStateFlow("")
    private val _selectedMonth  = MutableStateFlow<String?>(Companion.currentMonthKey())
    private val _editingExpense = MutableStateFlow<Expense?>(null)
    /** Default to showing expenses; set to a specific type to filter */
    private val _typeFilter     = MutableStateFlow<TransactionType?>(TransactionType.EXPENSE)

    // ── Public read-only flows ────────────────────────────────────────────────

    val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()
    val selectedMonth:  StateFlow<String?>  = _selectedMonth.asStateFlow()
    val typeFilter:     StateFlow<TransactionType?> = _typeFilter.asStateFlow()

    /** Raw all-transactions list (used by full history, export, and edit). */
    val expenses: StateFlow<List<Expense>> = repo.getAll()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // ── Derived flows ─────────────────────────────────────────────────────────

    /** Single combined state consumed by the Dashboard — one recomposition per change. */
    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        expenses, _query, _selectedMonth, _typeFilter
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val list       = arr[0] as List<Expense>
        val query      = arr[1] as String
        val month      = arr[2] as String?
        val typeFilter = arr[3] as TransactionType?


        // ── All transactions in the selected month (for summary totals) ──────
        val monthList = list.filter { e ->
            month == null || monthKey(e) == month
        }

        // ── Financial summary (Balance = Income − Expense − Transfer) ────────
        val totalIncome    = monthList.filter { it.type == TransactionType.INCOME   }.sumOf { it.amount }
        val totalExpenses  = monthList.filter { it.type == TransactionType.EXPENSE  }.sumOf { it.amount }
        val totalTransfers = monthList.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }
        val balance        = totalIncome - totalExpenses - totalTransfers

        // ── Type-filtered list for the transaction list ───────────────────────
        val filtered = monthList.filter { e ->
            (typeFilter == null || e.type == typeFilter) &&
            (query.isBlank() || e.title.contains(query, ignoreCase = true))
        }

        // ── Transfers for the dedicated transfers section ─────────────────────
        val transfersForMonth = monthList
            .filter { it.type == TransactionType.TRANSFER }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .sortedByDescending { it.timestamp }

        // ── Category breakdown — only EXPENSE rows ────────────────────────────
        val expenseOnly    = filtered.filter { it.type == TransactionType.EXPENSE }
        val categoryGroups = expenseOnly.groupBy { it.category }
        val categoryEntries = categoryGroups.entries
            .map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
        val categoryExpenses: Map<String, List<Expense>> = categoryEntries
            .associate { (cat, _) ->
                cat to (categoryGroups[cat]?.sortedByDescending { it.timestamp } ?: emptyList())
            }
        val grandTotal = categoryEntries.sumOf { it.second }

        if (filtered.isEmpty() && monthList.isEmpty()) {
            DashboardUiState.Empty(
                selectedMonth     = month ?: "All Months",
                availableMonths   = availableMonths(list),
                transfersForMonth = transfersForMonth,
                totalIncome       = totalIncome,
                totalExpenses     = totalExpenses,
                totalTransfers    = totalTransfers,
                balance           = balance
            )
        } else {
            DashboardUiState.Success(
                selectedMonth     = month ?: "All Months",
                availableMonths   = availableMonths(list),
                filteredExpenses  = filtered,
                categoryEntries   = categoryEntries,
                categoryExpenses  = categoryExpenses,
                grandTotal        = grandTotal,
                searchQuery       = query,
                transfersForMonth = transfersForMonth,
                totalIncome       = totalIncome,
                totalExpenses     = totalExpenses,
                totalTransfers    = totalTransfers,
                balance           = balance
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DashboardUiState.Loading
    )

    /** Monthly EXPENSE totals for analytics bar chart. */
    val monthlySummary: StateFlow<Map<String, Double>> = expenses
        .map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .groupBy { monthKey(it) }
                .mapValues { (_, v) -> v.sumOf { it.amount } }
                .entries.sortedBy { it.key }
                .associate { it.key to it.value }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    /** Per-month INCOME totals for analytics. */
    val monthlyIncomeSummary: StateFlow<Map<String, Double>> = expenses
        .map { list ->
            list.filter { it.type == TransactionType.INCOME }
                .groupBy { monthKey(it) }
                .mapValues { (_, v) -> v.sumOf { it.amount } }
                .entries.sortedBy { it.key }
                .associate { it.key to it.value }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    /** Category totals for the selected month — EXPENSE only. */
    val categorySummary: StateFlow<Map<String, Double>> = combine(
        expenses, _selectedMonth
    ) { list, month ->
        list.filter { it.type == TransactionType.EXPENSE }
            .filter { month == null || monthKey(it) == month }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    /** Per-month expense totals with category breakdown — EXPENSE only. */
    val monthComparison: StateFlow<List<MonthData>> = expenses
        .map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .groupBy { monthKey(it) }
                .entries.sortedBy { it.key }
                .map { (month, items) ->
                    MonthData(
                        month      = month,
                        total      = items.sumOf { it.amount },
                        byCategory = items.groupBy { it.category }
                            .mapValues { (_, v) -> v.sumOf { it.amount } }
                            .entries.sortedByDescending { it.value }
                            .associate { it.key to it.value }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // ── Public commands ───────────────────────────────────────────────────────

    fun selectMonth(month: String?) { _selectedMonth.value = month }
    fun setQuery(value: String) { _query.update { value } }
    fun setTypeFilter(type: TransactionType?) { _typeFilter.value = type }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        subCategory: String? = null,
        categoryId: Long = 0,
        subCategoryId: Long? = null,
        timestamp: Long = System.currentTimeMillis(),
        type: TransactionType = TransactionType.EXPENSE   // new parameter, safe default
    ) {
        viewModelScope.launch {
            repo.insert(
                Expense(
                    title         = title,
                    amount        = amount,
                    category      = category,
                    subCategory   = subCategory,
                    categoryId    = categoryId,
                    subCategoryId = subCategoryId,
                    timestamp     = timestamp,
                    type          = type
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repo.update(expense)
            _editingExpense.value = null
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repo.delete(expense) }
    }

    fun setEditingExpense(expense: Expense?) {
        _editingExpense.value = expense
    }

    fun clearEditingExpense() {
        _editingExpense.value = null
    }

    // ── Backup / Restore ──────────────────────────────────────────────────────

    private val _backupResult = MutableSharedFlow<BackupResult>(extraBufferCapacity = 1)
    val backupResult: SharedFlow<BackupResult> = _backupResult.asSharedFlow()

    fun exportData(targetUri: Uri) {
        viewModelScope.launch {
            runCatching { exportUseCase(targetUri) }
                .onSuccess { _backupResult.tryEmit(BackupResult.ExportSuccess) }
                // Sanitize: never expose file paths or internal exception details to the UI
                .onFailure { _backupResult.tryEmit(BackupResult.Error("Export failed. Please try again.")) }
        }
    }

    fun importData(sourceUri: Uri) {
        viewModelScope.launch {
            runCatching { importUseCase(sourceUri) }
                .onSuccess { _backupResult.tryEmit(BackupResult.ImportSuccess) }
                // Sanitize: never expose file paths or internal exception details to the UI
                .onFailure { _backupResult.tryEmit(BackupResult.Error("Import failed. The file may be corrupt or incompatible.")) }
        }
    }

    fun exportToExcel(targetUri: Uri) {
        viewModelScope.launch {
            runCatching { exportToExcelUseCase(targetUri) }
                .onSuccess { _backupResult.tryEmit(BackupResult.ExportSuccess) }
                .onFailure { _backupResult.tryEmit(BackupResult.Error("Excel export failed. Please try again.")) }
        }
    }

    fun importFromExcel(sourceUri: Uri) {
        viewModelScope.launch {
            runCatching { importFromExcelUseCase(sourceUri) }
                .onSuccess { _backupResult.tryEmit(BackupResult.ImportSuccess) }
                .onFailure { e ->
                    val message = when {
                        e.message?.contains("Cannot access") == true -> e.message!!
                        e.message?.contains("Permission denied") == true -> e.message!!
                        e.message?.contains("Failed to read") == true -> e.message!!
                        else -> "Import failed. Please check the file format and try again."
                    }
                    _backupResult.tryEmit(BackupResult.Error(message))
                }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun monthKey(expense: Expense): String = FMT_MONTH.format(Date(expense.timestamp))


    companion object {
        /** Reused formatter — never recreated per-call */
        private val FMT_MONTH = SimpleDateFormat("yyyy-MM", Locale.US)

        fun currentMonthKey(): String = FMT_MONTH.format(Date())

        /**
         * Returns the last 12 months starting from current month.
         * Always shows 12 months regardless of expense data.
         */
        fun availableMonths(expenses: List<Expense>): List<String> {
            val calendar = java.util.Calendar.getInstance()
            val months = mutableListOf<String>()
            
            // Generate last 12 months (current month + previous 11)
            repeat(12) { index ->
                val monthKey = FMT_MONTH.format(calendar.time)
                months.add(monthKey)
                // Move to previous month
                calendar.add(java.util.Calendar.MONTH, -1)
            }
            
            return months // Already in reverse chronological order (most recent first)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Supporting data class
// ─────────────────────────────────────────────────────────────────────────────

data class MonthData(
    val month:      String,
    val total:      Double,
    val byCategory: Map<String, Double>
)

// ─────────────────────────────────────────────────────────────────────────────
//  Extension helpers
// ─────────────────────────────────────────────────────────────────────────────

fun Expense.isExpense()  = type == TransactionType.EXPENSE
fun Expense.isTransfer() = type == TransactionType.TRANSFER
fun Expense.isIncome()   = type == TransactionType.INCOME
