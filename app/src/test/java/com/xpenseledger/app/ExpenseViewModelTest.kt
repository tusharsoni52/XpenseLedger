package com.xpenseledger.app

import com.xpenseledger.app.domain.repository.ExpenseRepository
import com.xpenseledger.app.domain.usecase.ExportExpensesUseCase
import com.xpenseledger.app.domain.usecase.ImportExpensesUseCase
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    private val repo = mockk<ExpenseRepository>(relaxed = true)
    private val exportUseCase = mockk<ExportExpensesUseCase>(relaxed = true)
    private val importUseCase = mockk<ImportExpensesUseCase>(relaxed = true)
    private lateinit var vm: ExpenseViewModel

    @Before
    fun setup() {
        val dispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        vm = ExpenseViewModel(repo, exportUseCase = exportUseCase, importUseCase = importUseCase)
    }

    @After
    fun tearDown() {
        // Intentionally left blank for example skeletons; in full test suites restore the main dispatcher.
    }

    @Test
    fun `addExpense calls repository insert`() = runTest {
        // arrange
        coEvery { repo.insert(any()) } returns Unit

        // act
        vm.addExpense(title = "Coffee", amount = 2.5, category = "Food")

        // ensure launched coroutines complete
        advanceUntilIdle()

        // assert
        coVerify(timeout = 1_000) { repo.insert(match { it.title == "Coffee" && it.amount == 2.5 }) }
    }

    @Test
    fun `selectMonth updates selectedMonth state`() = runTest {
        vm.selectMonth("2026-01")
        assert(vm.selectedMonth.value == "2026-01") { "Expected selectedMonth to update" }
    }
}
