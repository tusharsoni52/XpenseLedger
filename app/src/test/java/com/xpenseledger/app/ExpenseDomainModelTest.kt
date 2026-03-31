package com.xpenseledger.app

import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.domain.model.TransactionType
import com.xpenseledger.app.ui.viewmodel.isExpense
import com.xpenseledger.app.ui.viewmodel.isTransfer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Expense] domain model with [TransactionType] field.
 * Covers:
 *  - default type value
 *  - extension helpers isExpense() / isTransfer()
 *  - kotlinx.serialization round-trip (including backward compat with missing field)
 */
class ExpenseDomainModelTest {

    private val baseExpense = Expense(
        id        = 1L,
        title     = "Test",
        amount    = 100.0,
        category  = "Finance",
        timestamp = 1_000_000L
    )

    // ── Default value ─────────────────────────────────────────────────────────

    @Test
    fun `Expense defaults to EXPENSE type`() {
        assertEquals(TransactionType.EXPENSE, baseExpense.type)
    }

    @Test
    fun `Expense can be created with TRANSFER type`() {
        val transfer = baseExpense.copy(type = TransactionType.TRANSFER)
        assertEquals(TransactionType.TRANSFER, transfer.type)
    }

    // ── Extension helpers ─────────────────────────────────────────────────────

    @Test
    fun `isExpense returns true for EXPENSE type`() {
        assertTrue(baseExpense.isExpense())
    }

    @Test
    fun `isExpense returns false for TRANSFER type`() {
        assertFalse(baseExpense.copy(type = TransactionType.TRANSFER).isExpense())
    }

    @Test
    fun `isTransfer returns true for TRANSFER type`() {
        assertTrue(baseExpense.copy(type = TransactionType.TRANSFER).isTransfer())
    }

    @Test
    fun `isTransfer returns false for EXPENSE type`() {
        assertFalse(baseExpense.isTransfer())
    }

    // ── Serialization round-trip ──────────────────────────────────────────────

    @Test
    fun `serialize and deserialize Expense preserves EXPENSE type`() {
        val json   = Json.encodeToString(baseExpense)
        val result = Json.decodeFromString<Expense>(json)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `serialize and deserialize Expense preserves TRANSFER type`() {
        val transfer = baseExpense.copy(type = TransactionType.TRANSFER)
        val json     = Json.encodeToString(transfer)
        val result   = Json.decodeFromString<Expense>(json)
        assertEquals(TransactionType.TRANSFER, result.type)
    }

    @Test
    fun `deserialize old backup JSON without type field defaults to EXPENSE`() {
        // Simulates an old backup file that has no "type" field
        val oldJson = """
            {
              "id": 1,
              "title": "Old Expense",
              "amount": 50.0,
              "category": "Food",
              "timestamp": 1000000
            }
        """.trimIndent()

        val lenientJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val result = lenientJson.decodeFromString<Expense>(oldJson)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `serialize list of expenses round-trips correctly`() {
        val list = listOf(
            baseExpense,
            baseExpense.copy(id = 2L, type = TransactionType.TRANSFER),
            baseExpense.copy(id = 3L, type = TransactionType.INCOME)
        )
        val json   = Json.encodeToString(list)
        val result = Json.decodeFromString<List<Expense>>(json)

        assertEquals(TransactionType.EXPENSE,  result[0].type)
        assertEquals(TransactionType.TRANSFER, result[1].type)
        assertEquals(TransactionType.INCOME,   result[2].type)
    }
}

