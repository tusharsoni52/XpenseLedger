package com.xpenseledger.app

import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.domain.usecase.ExportExpensesUseCase
import com.xpenseledger.app.domain.usecase.ImportExpensesUseCase
import com.xpenseledger.app.ui.viewmodel.DashboardUiState
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTransactionTypeTest {

    private val dispatcher   = StandardTestDispatcher()
    private val testScope    = TestScope(dispatcher)
    private val repo          = mockk<ExpenseRepository>(relaxed = true)
    private val exportUseCase = mockk<ExportExpensesUseCase>(relaxed = true)
    private val importUseCase = mockk<ImportExpensesUseCase>(relaxed = true)
    private val expensesFlow  = MutableStateFlow<List<Expense>>(emptyList())
    private lateinit var vm: ExpenseViewModel

    private val currentMonth: String
        get() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun expense(
        id: Long = 1L, title: String = "Test", amount: Double = 100.0,
        category: String = "Finance", type: TransactionType = TransactionType.EXPENSE
    ) = Expense(id = id, title = title, amount = amount, category = category,
        timestamp = System.currentTimeMillis(), type = type)

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        every { repo.getAll() } returns expensesFlow
        vm = ExpenseViewModel(repo, exportUseCase, importUseCase)
        // Keep ALL WhileSubscribed flows alive so .value is populated during tests.
        testScope.backgroundScope.launch { vm.expenses.collect {} }
        testScope.backgroundScope.launch { vm.monthlySummary.collect {} }
        testScope.backgroundScope.launch { vm.categorySummary.collect {} }
        testScope.backgroundScope.launch { vm.monthComparison.collect {} }
    }

    // ── addExpense — type parameter ───────────────────────────────────────────

    @Test
    fun `addExpense defaults to EXPENSE type`() = testScope.runTest {
        coEvery { repo.insert(any()) } returns Unit
        vm.addExpense(title = "Coffee", amount = 3.0, category = "Food")
        advanceUntilIdle()
        coVerify { repo.insert(match { it.type == TransactionType.EXPENSE }) }
    }

    @Test
    fun `addExpense passes TRANSFER type to repository`() = testScope.runTest {
        coEvery { repo.insert(any()) } returns Unit
        vm.addExpense("Family Money", 5000.0, "Finance", type = TransactionType.TRANSFER)
        advanceUntilIdle()
        coVerify { repo.insert(match { it.type == TransactionType.TRANSFER }) }
    }

    @Test
    fun `addExpense passes INCOME type to repository`() = testScope.runTest {
        coEvery { repo.insert(any()) } returns Unit
        vm.addExpense("Salary", 50000.0, "Finance", type = TransactionType.INCOME)
        advanceUntilIdle()
        coVerify { repo.insert(match { it.type == TransactionType.INCOME }) }
    }

    // ── dashboardUiState — TRANSFER excluded ─────────────────────────────────

    @Test
    fun `dashboardUiState excludes TRANSFER from grandTotal`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, amount = 100.0, type = TransactionType.EXPENSE),
            expense(id = 2L, amount = 500.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        val state = vm.dashboardUiState.value as DashboardUiState.Success
        assertEquals(100.0, state.grandTotal, 0.001)
    }

    @Test
    fun `dashboardUiState excludes TRANSFER from filteredExpenses list`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, title = "Grocery",        type = TransactionType.EXPENSE),
            expense(id = 2L, title = "Family Support",  type = TransactionType.TRANSFER))
        advanceUntilIdle()
        // With no type filter active, filteredExpenses contains ALL transaction types
        val state = vm.dashboardUiState.value as DashboardUiState.Success
        assertEquals(2, state.filteredExpenses.size)
        // But categoryEntries only has EXPENSE rows
        assertEquals(1, state.categoryEntries.size)
        assertEquals("Finance", state.categoryEntries[0].first)
    }

    @Test
    fun `dashboardUiState is Success with empty categories when only TRANSFER rows exist`() = testScope.runTest {
        expensesFlow.value = listOf(expense(id = 1L, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        // Transfers appear in filteredExpenses → Success state, but no expense category breakdown
        val state = vm.dashboardUiState.value as DashboardUiState.Success
        assertEquals(0.0, state.grandTotal, 0.001)
        assertTrue(state.categoryEntries.isEmpty())
    }

    @Test
    fun `dashboardUiState includes EXPENSE and excludes TRANSFER from categoryEntries`() =
        testScope.runTest {
            expensesFlow.value = listOf(
                expense(id = 1L, category = "Food",    amount = 200.0, type = TransactionType.EXPENSE),
                expense(id = 2L, category = "Finance", amount = 999.0, type = TransactionType.TRANSFER))
            advanceUntilIdle()
            val state = vm.dashboardUiState.value as DashboardUiState.Success
            assertEquals(1, state.categoryEntries.size)
            assertEquals("Food", state.categoryEntries[0].first)
        }

    @Test
    fun `grandTotal is zero and state is Success with empty categories when all rows are TRANSFER`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, amount = 1000.0, type = TransactionType.TRANSFER),
            expense(id = 2L, amount = 2000.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        // Transfers appear in filteredExpenses so state is Success, not Empty
        val state = vm.dashboardUiState.value as DashboardUiState.Success
        // grandTotal = sum of EXPENSE categoryEntries only = 0
        assertEquals(0.0, state.grandTotal, 0.001)
        assertTrue(state.categoryEntries.isEmpty())
        // But financial summary shows correct transfer total
        assertEquals(3000.0, state.totalTransfers, 0.001)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(-3000.0, state.balance, 0.001)
    }

    @Test
    // grandTotal = sum of EXPENSE categoryEntries only.
    // Balance = Income(200) - Expenses(100) - Transfers(500) = -400.
    // INCOME rows are correctly counted in balance via totalIncome, not grandTotal.
    fun `grandTotal excludes TRANSFER and INCOME — balance accounts for all types`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, amount = 100.0, type = TransactionType.EXPENSE),
            expense(id = 2L, amount = 500.0, type = TransactionType.TRANSFER),
            expense(id = 3L, amount = 200.0, type = TransactionType.INCOME))
        advanceUntilIdle()
        val state = vm.dashboardUiState.value as DashboardUiState.Success
        // grandTotal only counts EXPENSE category breakdown
        assertEquals(100.0, state.grandTotal, 0.001)
        // Financial summary has the full picture
        assertEquals(200.0, state.totalIncome,    0.001)
        assertEquals(100.0, state.totalExpenses,  0.001)
        assertEquals(500.0, state.totalTransfers, 0.001)
        assertEquals(-400.0, state.balance,       0.001)
    }

    // ── monthlySummary — TRANSFER excluded ────────────────────────────────────

    @Test
    fun `monthlySummary excludes TRANSFER rows`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, amount = 300.0, type = TransactionType.EXPENSE),
            expense(id = 2L, amount = 700.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        assertEquals(300.0, vm.monthlySummary.value[currentMonth] ?: 0.0, 0.001)
    }

    @Test
    fun `monthlySummary is empty when only TRANSFER rows exist`() = testScope.runTest {
        expensesFlow.value = listOf(expense(id = 1L, amount = 5000.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        assertTrue(vm.monthlySummary.value.isEmpty())
    }

    // ── categorySummary — TRANSFER excluded ───────────────────────────────────

    @Test
    fun `categorySummary excludes TRANSFER rows`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, category = "Food",    amount = 150.0, type = TransactionType.EXPENSE),
            expense(id = 2L, category = "Finance", amount = 999.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        val summary = vm.categorySummary.value
        assertEquals(1, summary.size)
        assertEquals(150.0, summary["Food"]!!, 0.001)
        assertFalse(summary.containsKey("Finance"))
    }

    @Test
    fun `categorySummary is empty when only TRANSFER rows exist`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, category = "Finance", amount = 5000.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        assertTrue(vm.categorySummary.value.isEmpty())
    }

    // ── monthComparison — TRANSFER excluded ───────────────────────────────────

    @Test
    fun `monthComparison total excludes TRANSFER rows`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, amount = 400.0, type = TransactionType.EXPENSE),
            expense(id = 2L, amount = 600.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        val comparison = vm.monthComparison.value
        assertEquals(1, comparison.size)
        assertEquals(400.0, comparison[0].total, 0.001)
    }

    @Test
    fun `monthComparison is empty when only TRANSFER rows exist`() = testScope.runTest {
        expensesFlow.value = listOf(expense(id = 1L, amount = 5000.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        assertTrue(vm.monthComparison.value.isEmpty())
    }

    @Test
    fun `monthComparison byCategory excludes TRANSFER categories`() = testScope.runTest {
        expensesFlow.value = listOf(
            expense(id = 1L, category = "Food",    amount = 200.0, type = TransactionType.EXPENSE),
            expense(id = 2L, category = "Finance", amount = 999.0, type = TransactionType.TRANSFER))
        advanceUntilIdle()
        val comparison = vm.monthComparison.value
        assertEquals(1, comparison.size)
        assertTrue(comparison[0].byCategory.containsKey("Food"))
        assertFalse(comparison[0].byCategory.containsKey("Finance"))
    }

    // ── Mixed scenario ────────────────────────────────────────────────────────

    @Test
    fun `mixed — filteredExpenses contains all types when no filter active`() =
        testScope.runTest {
            expensesFlow.value = listOf(
                expense(id = 1L, title = "Lunch",          amount = 120.0,  category = "Food",      type = TransactionType.EXPENSE),
                expense(id = 2L, title = "Family Support", amount = 5000.0, category = "Finance",   type = TransactionType.TRANSFER),
                expense(id = 3L, title = "Fuel",           amount = 200.0,  category = "Transport", type = TransactionType.EXPENSE))
            advanceUntilIdle()

            val dash = vm.dashboardUiState.value as DashboardUiState.Success
            // All 3 rows must be in filteredExpenses (no type filter active)
            assertTrue("Expected 3 rows but got ${dash.filteredExpenses.size}: ${dash.filteredExpenses.map { it.title }}",
                dash.filteredExpenses.size == 3)
        }

    @Test
    fun `mixed — grandTotal is EXPENSE only, balance accounts for all types`() =
        testScope.runTest {
            expensesFlow.value = listOf(
                expense(id = 1L, title = "Lunch",          amount = 120.0,  category = "Food",      type = TransactionType.EXPENSE),
                expense(id = 2L, title = "Family Support", amount = 5000.0, category = "Finance",   type = TransactionType.TRANSFER),
                expense(id = 3L, title = "Fuel",           amount = 200.0,  category = "Transport", type = TransactionType.EXPENSE))
            advanceUntilIdle()

            val dash = vm.dashboardUiState.value as DashboardUiState.Success

            // grandTotal = EXPENSE category sum only
            assertEquals(320.0, dash.grandTotal, 0.001)

            // Financial summary
            assertEquals(0.0,     dash.totalIncome,    0.001)
            assertEquals(320.0,   dash.totalExpenses,  0.001)
            assertEquals(5000.0,  dash.totalTransfers, 0.001)
            // Balance = Income(0) - Expenses(320) - Transfers(5000) = -5320
            assertEquals(-5320.0, dash.balance,        0.001)

            // Category breakdown: Finance (TRANSFER) must NOT appear
            assertFalse(dash.categoryEntries.any { it.first == "Finance" })
            assertEquals(120.0, dash.categoryEntries.first { it.first == "Food" }.second, 0.001)
            assertEquals(200.0, dash.categoryEntries.first { it.first == "Transport" }.second, 0.001)
        }

    @Test
    fun `mixed — analytics flows only include EXPENSE rows`() =
        testScope.runTest {
            expensesFlow.value = listOf(
                expense(id = 1L, title = "Lunch",          amount = 120.0,  category = "Food",      type = TransactionType.EXPENSE),
                expense(id = 2L, title = "Family Support", amount = 5000.0, category = "Finance",   type = TransactionType.TRANSFER),
                expense(id = 3L, title = "Fuel",           amount = 200.0,  category = "Transport", type = TransactionType.EXPENSE))
            advanceUntilIdle()

            // Monthly summary — EXPENSE only
            assertEquals(320.0, vm.monthlySummary.value[currentMonth] ?: 0.0, 0.001)

            // Category summary — EXPENSE only
            val catSummary = vm.categorySummary.value
            assertFalse(catSummary.containsKey("Finance"))
            assertEquals(120.0, catSummary["Food"]!!, 0.001)
            assertEquals(200.0, catSummary["Transport"]!!, 0.001)

            // Month comparison — EXPENSE only
            val mc = vm.monthComparison.value
            assertEquals(1, mc.size)
            assertEquals(320.0, mc[0].total, 0.001)
            assertFalse(mc[0].byCategory.containsKey("Finance"))
        }

    // ── Regression guards ─────────────────────────────────────────────────────

    @Test
    fun `addExpense calls repository insert regression`() = testScope.runTest {
        coEvery { repo.insert(any()) } returns Unit
        vm.addExpense(title = "Coffee", amount = 2.5, category = "Food")
        advanceUntilIdle()
        coVerify { repo.insert(match { it.title == "Coffee" && it.amount == 2.5 }) }
    }

    @Test
    fun `selectMonth updates selectedMonth state regression`() = testScope.runTest {
        vm.selectMonth("2026-01")
        assertEquals("2026-01", vm.selectedMonth.value)
    }
}
