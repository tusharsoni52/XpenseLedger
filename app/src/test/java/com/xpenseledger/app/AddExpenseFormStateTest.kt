package com.xpenseledger.app

import com.xpenseledger.app.domain.model.Category
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.ui.screens.add.AddExpenseFormState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AddExpenseFormState] focusing on the new [transactionType] field.
 * Covers:
 *  - default value
 *  - pre-filling in edit mode
 *  - state mutation
 */
class AddExpenseFormStateTest {

    // ── Default value ─────────────────────────────────────────────────────────

    @Test
    fun `transactionType defaults to EXPENSE`() {
        val form = AddExpenseFormState()
        assertEquals(TransactionType.EXPENSE, form.transactionType)
    }

    @Test
    fun `transactionType pre-fills from initialTransactionType parameter`() {
        val form = AddExpenseFormState(initialTransactionType = TransactionType.TRANSFER)
        assertEquals(TransactionType.TRANSFER, form.transactionType)
    }

    @Test
    fun `transactionType pre-fills INCOME correctly`() {
        val form = AddExpenseFormState(initialTransactionType = TransactionType.INCOME)
        assertEquals(TransactionType.INCOME, form.transactionType)
    }

    // ── State mutation ────────────────────────────────────────────────────────

    @Test
    fun `transactionType can be changed to TRANSFER`() {
        val form = AddExpenseFormState()
        form.transactionType = TransactionType.TRANSFER
        assertEquals(TransactionType.TRANSFER, form.transactionType)
    }

    @Test
    fun `transactionType can be toggled back to EXPENSE`() {
        val form = AddExpenseFormState(initialTransactionType = TransactionType.TRANSFER)
        form.transactionType = TransactionType.EXPENSE
        assertEquals(TransactionType.EXPENSE, form.transactionType)
    }

    // ── Edit mode — pre-fill from existing Expense ────────────────────────────

    @Test
    fun `edit mode with TRANSFER expense pre-fills transactionType as TRANSFER`() {
        val form = AddExpenseFormState(
            initialTitle           = "Family Support",
            initialAmount          = "5000.0",
            initialTransactionType = TransactionType.TRANSFER
        )
        assertEquals(TransactionType.TRANSFER, form.transactionType)
        assertEquals("Family Support", form.title)
        assertEquals("5000.0", form.amount)
    }

    @Test
    fun `edit mode with EXPENSE expense pre-fills transactionType as EXPENSE`() {
        val form = AddExpenseFormState(
            initialTitle           = "Grocery",
            initialAmount          = "200.0",
            initialTransactionType = TransactionType.EXPENSE
        )
        assertEquals(TransactionType.EXPENSE, form.transactionType)
    }

    // ── isValid unaffected by transactionType ─────────────────────────────────

    @Test
    fun `form is valid regardless of transactionType when other fields are valid`() {
        val form = AddExpenseFormState(
            initialTitle           = "Family Support",
            initialAmount          = "5000.0",
            initialTransactionType = TransactionType.TRANSFER
        )
        // mainCat must be set for validity
        form.mainCat = Category(id = 7L, name = "Finance", type = "MAIN", icon = "💰")
        assertEquals(true, form.isValid)
    }

    @Test
    fun `form validity does not depend on transactionType value`() {
        val validForm = AddExpenseFormState(
            initialTitle           = "Test",
            initialAmount          = "100.0",
            initialTransactionType = TransactionType.TRANSFER
        )
        validForm.mainCat = Category(id = 7L, name = "Finance", type = "MAIN", icon = "💰")

        val expenseForm = validForm.also { it.transactionType = TransactionType.EXPENSE }
        val transferForm = validForm.also { it.transactionType = TransactionType.TRANSFER }

        assertEquals(expenseForm.isValid, transferForm.isValid)
    }

    // ── touchAll does not affect transactionType ──────────────────────────────

    @Test
    fun `touchAll does not reset transactionType`() {
        val form = AddExpenseFormState(initialTransactionType = TransactionType.TRANSFER)
        form.touchAll()
        assertEquals(TransactionType.TRANSFER, form.transactionType)
    }
}

