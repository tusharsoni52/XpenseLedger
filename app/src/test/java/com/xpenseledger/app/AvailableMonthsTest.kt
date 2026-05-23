package com.xpenseledger.app

import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class AvailableMonthsTest {

    @Test
    fun `availableMonths returns 12 months regardless of expense data`() {
        // Given: No expenses
        val expenses = emptyList<com.xpenseledger.app.domain.model.Expense>()

        // When: Get available months
        val months = ExpenseViewModel.availableMonths(expenses)

        // Then: Should return exactly 12 months
        assertEquals(12, months.size)
        println("Generated months:")
        months.forEach { println("  $it") }
    }

    @Test
    fun `availableMonths returns months in reverse chronological order`() {
        // Given: No expenses
        val expenses = emptyList<com.xpenseledger.app.domain.model.Expense>()

        // When: Get available months
        val months = ExpenseViewModel.availableMonths(expenses)

        // Then: Months should be in descending order (most recent first)
        val sortedMonths = months.sortedDescending()
        assertEquals(sortedMonths, months)
    }
}

