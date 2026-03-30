package com.xpenseledger.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.domain.usecase.ExportExpensesUseCase
import com.xpenseledger.app.domain.usecase.ImportExpensesUseCase
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
    /** DB ready but no expenses match the current filter */
    data class Empty(
        val selectedMonth:  String,
        val availableMonths: List<String>
    ) : DashboardUiState()
    /** Normal data */
    data class Success(
        val selectedMonth:    String,
        val availableMonths:  List<String>,
        val filteredExpenses: List<Expense>,
        val categoryEntries:  List<Pair<String, Double>>,
        val categoryExpenses: Map<String, List<Expense>>,   // expenses per category
        val grandTotal:       Double,
        val searchQuery:      String
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
) : ViewModel() {

    // ── Private mutable state ─────────────────────────────────────────────────

    private val _query          = MutableStateFlow("")
    private val _selectedMonth  = MutableStateFlow(currentMonthKey())
    private val _editingExpense = MutableStateFlow<Expense?>(null)

    // ── Public read-only flows ────────────────────────────────────────────────
    
    /** The expense currently being edited (null if adding new) */
    val editingExpense: StateFlow<Expense?> = _editingExpense.asStateFlow()

    /** The currently selected month key ("yyyy-MM"). Never null. */
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    /** Raw all-expenses list (used by ComparisonScreen / analytics). */
    val expenses: StateFlow<List<Expense>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // ── Derived flows ─────────────────────────────────────────────────────────

    /** Single combined state consumed by the Dashboard — one recomposition per change. */
    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        expenses, _query, _selectedMonth
    ) { list, query, month ->
        // Still loading — the initial emptyList hasn't been replaced by real data yet
        // We detect "loading" by checking whether the repo has emitted at least once;
        // the simplest proxy: treat a completely empty list while the query is blank as Loading
        // only on first emission (handled below via distinctUntilChanged + WhileSubscribed).

        val filtered = list.filter { e ->
            monthKey(e) == month &&
            (query.isBlank() || e.title.contains(query, ignoreCase = true))
        }

        val categoryGroups = filtered.groupBy { it.category }

        val categoryEntries = categoryGroups.entries
            .map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }

        // Keep the same order as categoryEntries so the UI is consistent
        val categoryExpenses: Map<String, List<Expense>> = categoryEntries
            .associate { (cat, _) ->
                cat to (categoryGroups[cat]?.sortedByDescending { it.timestamp } ?: emptyList())
            }

        val grandTotal = categoryEntries.sumOf { it.second }

        if (filtered.isEmpty()) {
            DashboardUiState.Empty(
                selectedMonth   = month,
                availableMonths = availableMonths()
            )
        } else {
            DashboardUiState.Success(
                selectedMonth    = month,
                availableMonths  = availableMonths(),
                filteredExpenses = filtered,
                categoryEntries  = categoryEntries,
                categoryExpenses = categoryExpenses,
                grandTotal       = grandTotal,
                searchQuery      = query
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,          // keep alive across lock/unlock — no Loading flash on re-entry
        DashboardUiState.Loading
    )

    /** Monthly totals for analytics bar chart */
    val monthlySummary: StateFlow<Map<String, Double>> = expenses
        .map { list ->
            list.groupBy { monthKey(it) }
                .mapValues { (_, v) -> v.sumOf { it.amount } }
                .entries.sortedBy { it.key }
                .associate { it.key to it.value }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())


    /** Category totals for the selected month — used by ComparisonScreen */
    val categorySummary: StateFlow<Map<String, Double>> = combine(
        expenses, _selectedMonth
    ) { list, month ->
        list.filter { monthKey(it) == month }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .associate { it.key to it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    /** Per-month totals with category breakdown — used for comparison chart */
    val monthComparison: StateFlow<List<MonthData>> = expenses
        .map { list ->
            list.groupBy { monthKey(it) }
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

    fun selectMonth(month: String) { _selectedMonth.update { month } }

    fun setQuery(value: String) { _query.update { value } }


    fun addExpense(
        title: String, amount: Double, category: String,
        subCategory: String? = null, categoryId: Long = 0, subCategoryId: Long? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repo.insert(
                Expense(
                    title = title, amount = amount, category = category,
                    subCategory = subCategory, categoryId = categoryId,
                    subCategoryId = subCategoryId, timestamp = timestamp
                )
            )
        }
    }


    fun updateExpense(expense: Expense) {
        viewModelScope.launch { 
            repo.update(expense)
            _editingExpense.value = null  // Clear editing state after update
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun monthKey(expense: Expense): String = FMT_MONTH.format(Date(expense.timestamp))

    companion object {
        /** Reused formatter — never recreated per-call */
        private val FMT_MONTH = SimpleDateFormat("yyyy-MM", Locale.US)

        fun currentMonthKey(): String = FMT_MONTH.format(Date())

        /** Returns 12 month keys: current month first, then previous 11. */
        fun availableMonths(): List<String> {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            return buildList {
                repeat(12) {
                    add(FMT_MONTH.format(cal.time))
                    cal.add(java.util.Calendar.MONTH, -1)
                }
            }
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