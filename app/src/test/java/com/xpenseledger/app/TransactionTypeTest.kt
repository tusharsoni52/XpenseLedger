package com.xpenseledger.app

import com.xpenseledger.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionTypeTest {

    @Test
    fun `fromString returns EXPENSE for EXPENSE string`() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString("EXPENSE"))
    }

    @Test
    fun `fromString returns TRANSFER for TRANSFER string`() {
        assertEquals(TransactionType.TRANSFER, TransactionType.fromString("TRANSFER"))
    }

    @Test
    fun `fromString returns INCOME for INCOME string`() {
        assertEquals(TransactionType.INCOME, TransactionType.fromString("INCOME"))
    }

    @Test
    fun `fromString returns EXPENSE as safe default for unknown value`() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString returns EXPENSE as safe default for empty string`() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString(""))
    }

    @Test
    fun `fromString is case-sensitive and lowercase returns EXPENSE default`() {
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString("expense"))
        assertEquals(TransactionType.EXPENSE, TransactionType.fromString("transfer"))
    }

    @Test
    fun `enum name strings match expected DB values`() {
        assertEquals("EXPENSE",  TransactionType.EXPENSE.name)
        assertEquals("INCOME",   TransactionType.INCOME.name)
        assertEquals("TRANSFER", TransactionType.TRANSFER.name)
    }

    @Test
    fun `round-trip name to fromString preserves all enum values`() {
        TransactionType.entries.forEach { type ->
            assertEquals(type, TransactionType.fromString(type.name))
        }
    }
}
